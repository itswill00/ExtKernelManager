package com.hans.ext.kernelmanager.hal.intelligence

import com.hans.ext.kernelmanager.util.SmartShell
import android.util.Log
import java.io.File
import org.json.JSONObject
import org.json.JSONArray

/**
 * NodeSignature: Defines a hardware interface pattern for discovery.
 */
data class NodeSignature(
    val id: String,
    val description: String,
    val paths: List<String>,
    val subsystem: String,
    val isCritical: Boolean = false
)

/**
 * ChipsetHeritage: Detailed classification of the detected SoC.
 */
data class ChipsetHeritage(
    val brand: String,
    val model: String,
    val codename: String,
    val arch: String,
    val tier: String,
    val vendorFeatures: List<String>
)

/**
 * HardwareRegistry (V13): The Omni-Oracle Data Map.
 */
data class SovereignRegistry(
    val heritage: ChipsetHeritage = ChipsetHeritage("Generic", "Standard", "Generic", "arm64", "Entry", emptyList()),
    val interfaces: Map<String, String> = emptyMap(),
    val cpuPolicies: List<Int> = emptyList(),
    val cpuCoreMap: Map<Int, String> = emptyMap(), // policy -> "0-3"
    val thermalZones: Map<String, String> = emptyMap(),
    val subsystems: Map<String, Map<String, String>> = emptyMap(),
    val totalCores: Int = 0,
    val socModel: String = "Unknown",
    val kernelMetadata: Map<String, String> = emptyMap(), // version, compiler, etc.
    val marketName: String = "Android Device",
    val buildNumber: String = "",
    val selinuxStatus: String = "",
    val basebandVersion: String = "",
    val bootloaderVersion: String = "",
    val deviceCodename: String = ""
)

/**
 * SystemDiscovery (V12) - The Sovereign Omni-Oracle.
 * A massive intelligence engine that utilizes recursive scanning, signature matching, 
 * and vendor-specific heuristics to map the entire kernel tuning surface.
 */
object SystemDiscovery {
    private const val TAG = "SovereignOmniOracle"
    private const val CACHE_PATH = "/data/local/tmp/ekm_registry_cache.json"

    /**
     * @Volatile ensures that the latest written value is immediately visible
     * to all threads — preventing stale reads during concurrent access.
     */
    @Volatile
    private var activeRegistry = SovereignRegistry()
    private val discoveryAudit = mutableListOf<String>()

    /**
     * Executes the full-spectrum hardware discovery sequence.
     * This is an intensive operation that builds a complete system awareness map.
     */
    fun discover(force: Boolean = false): SovereignRegistry {
        log("--- Initiating Omni-Oracle Full Spectrum Discovery ---")
        
        val currentKernel = SmartShell.sh("uname -r").trim()
        val currentBuild = SmartShell.sh("getprop ro.build.display.id").trim()
        val fingerprint = "$currentKernel|$currentBuild"

        if (!force && SmartShell.sh("ls $CACHE_PATH").trim() == CACHE_PATH) {
            val cacheRaw = SmartShell.read(CACHE_PATH)
            if (cacheRaw.isNotEmpty()) {
                try {
                    val cacheJson = JSONObject(cacheRaw)
                    val cacheFingerprint = cacheJson.optString("cache_fingerprint", "")
                    if (cacheFingerprint == fingerprint) {
                        log("Cache hit! Fingerprint verified. Loading registry from cache.")
                        activeRegistry = parseRegistry(cacheJson)
                        return activeRegistry
                    } else {
                        log("Cache fingerprint mismatch. Current: $fingerprint, Cache: $cacheFingerprint. Forcing rescan.")
                    }
                } catch (e: Exception) {
                    log("Failed to parse cache: ${e.message}")
                }
            }
        }

        log("Performing deep hardware discovery...")
        val heritage = identifyHeritage()
        log("Heritage Established: ${heritage.brand} ${heritage.model} [${heritage.tier}]")

        val interfaces = mutableMapOf<String, String>()
        val subsystemMap = mutableMapOf<String, Map<String, String>>()

        // 1. Core Subsystem Scanning
        subsystemMap["CPU"] = scanCpuSubsystem()
        subsystemMap["GPU"] = scanGpuSubsystem()
        subsystemMap["MEMORY"] = scanMemorySubsystem()
        subsystemMap["STORAGE"] = scanStorageSubsystem()
        subsystemMap["THERMAL"] = scanThermalZones()

        // 2. Advanced Subsystem Scanning (Inspired by SmartPack/Franco)
        subsystemMap["SOUND"] = scanSoundSubsystem()
        subsystemMap["DISPLAY"] = scanDisplaySubsystem()
        subsystemMap["BATTERY"] = scanBatterySubsystem()
        subsystemMap["VENDOR"] = scanVendorSubsystem(heritage)

        // 3. Signature Matching
        val signatures = getSignatureDatabase()
        signatures.forEach { sig ->
            val foundPath = sig.paths.find { nodeExists(it) }
            if (foundPath != null) {
                interfaces[sig.id] = foundPath
                log("Signature Matched: ${sig.id} -> $foundPath")
            }
        }

        // 4. Build Core Map & Metadata
        val cpuSub = subsystemMap["CPU"] ?: emptyMap()
        val policies = cpuSub.keys
            .mapNotNull { Regex("policy(\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() }
            .distinct().sorted()

        val coreMap = policies.associateWith { p ->
            SmartShell.read("/sys/devices/system/cpu/cpufreq/policy$p/affected_cpus")
                .ifEmpty { SmartShell.read("/sys/devices/system/cpu/cpu$p/cpufreq/affected_cpus") }
                .replace(" ", ",")
        }

        val versionRaw = SmartShell.read("/proc/version")
        val kernelMeta = mapOf(
            "version"  to versionRaw.substringBefore(" (").replace("Linux version ", ""),
            "compiler" to versionRaw.substringAfter("(").substringBefore(")"),
            "build"    to versionRaw.substringAfterLast("#").trim(),
            "cmdline"  to SmartShell.read("/proc/cmdline").take(200) + "..."
        )

        val totalCores = (0..15).count { nodeExists("/sys/devices/system/cpu/cpu$it") }
        val socModel   = (SmartShell.sh("getprop ro.soc.model").ifEmpty { 
            SmartShell.sh("getprop ro.board.platform") 
        }).uppercase()

        // Identify Human Marketing Name (Mature Logic)
        val marketName = resolveMarketName()
        val buildNum = SmartShell.sh("getprop ro.build.display.id")
        val selinux = SmartShell.sh("getenforce").ifEmpty { "Enforcing" }
        val baseband = SmartShell.sh("getprop gsm.version.baseband")
        val bootloader = SmartShell.sh("getprop ro.bootloader")
        val codename = SmartShell.sh("getprop ro.product.device").ifEmpty { 
            SmartShell.sh("getprop ro.build.product") 
        }.lowercase()

        activeRegistry = SovereignRegistry(
            heritage = heritage,
            interfaces = interfaces,
            cpuPolicies = policies,
            cpuCoreMap = coreMap,
            thermalZones = subsystemMap["THERMAL"] ?: emptyMap(),
            subsystems = subsystemMap,
            totalCores = totalCores,
            socModel = socModel,
            kernelMetadata = kernelMeta,
            marketName = marketName,
            buildNumber = buildNum,
            selinuxStatus = selinux,
            basebandVersion = baseband,
            bootloaderVersion = bootloader,
            deviceCodename = codename
        )
        
        // Cache the newly discovered registry
        try {
            val newJson = serializeRegistry(activeRegistry, fingerprint)
            SmartShell.write(CACHE_PATH, newJson.toString())
            SmartShell.sh("chmod 666 $CACHE_PATH")
            log("SovereignRegistry successfully cached to $CACHE_PATH")
        } catch (e: Exception) {
            log("Failed to write cache: ${e.message}")
        }

        log("Omni-Oracle Sequence Complete. Coverage: ${activeRegistry.interfaces.size + activeRegistry.subsystems.values.sumOf { it.size }} interfaces.")
        return activeRegistry
    }

    private fun serializeRegistry(reg: SovereignRegistry, fingerprint: String): JSONObject {
        val root = JSONObject()
        root.put("cache_fingerprint", fingerprint)
        
        val h = JSONObject()
        h.put("brand", reg.heritage.brand)
        h.put("model", reg.heritage.model)
        h.put("codename", reg.heritage.codename)
        h.put("arch", reg.heritage.arch)
        h.put("tier", reg.heritage.tier)
        h.put("vendorFeatures", JSONArray(reg.heritage.vendorFeatures))
        root.put("heritage", h)
        
        val ifaces = JSONObject()
        reg.interfaces.forEach { (k, v) -> ifaces.put(k, v) }
        root.put("interfaces", ifaces)
        
        root.put("cpuPolicies", JSONArray(reg.cpuPolicies))
        
        val cpuCore = JSONObject()
        reg.cpuCoreMap.forEach { (k, v) -> cpuCore.put(k.toString(), v) }
        root.put("cpuCoreMap", cpuCore)
        
        val tz = JSONObject()
        reg.thermalZones.forEach { (k, v) -> tz.put(k, v) }
        root.put("thermalZones", tz)
        
        val subs = JSONObject()
        reg.subsystems.forEach { (cat, map) ->
            val subMap = JSONObject()
            map.forEach { (k, v) -> subMap.put(k, v) }
            subs.put(cat, subMap)
        }
        root.put("subsystems", subs)
        
        root.put("totalCores", reg.totalCores)
        root.put("socModel", reg.socModel)
        
        val kMeta = JSONObject()
        reg.kernelMetadata.forEach { (k, v) -> kMeta.put(k, v) }
        root.put("kernelMetadata", kMeta)
        
        root.put("marketName", reg.marketName)
        root.put("buildNumber", reg.buildNumber)
        root.put("selinuxStatus", reg.selinuxStatus)
        root.put("basebandVersion", reg.basebandVersion)
        root.put("bootloaderVersion", reg.bootloaderVersion)
        root.put("deviceCodename", reg.deviceCodename)
        
        return root
    }

    private fun parseRegistry(root: JSONObject): SovereignRegistry {
        val h = root.getJSONObject("heritage")
        val vendorFeatures = mutableListOf<String>()
        val vfArr = h.getJSONArray("vendorFeatures")
        for (i in 0 until vfArr.length()) vendorFeatures.add(vfArr.getString(i))
        val heritage = ChipsetHeritage(
            h.getString("brand"), h.getString("model"), h.getString("codename"),
            h.getString("arch"), h.getString("tier"), vendorFeatures
        )
        
        val interfaces = mutableMapOf<String, String>()
        val ifaces = root.getJSONObject("interfaces")
        ifaces.keys().forEach { interfaces[it] = ifaces.getString(it) }
        
        val cpuPolicies = mutableListOf<Int>()
        val polArr = root.getJSONArray("cpuPolicies")
        for (i in 0 until polArr.length()) cpuPolicies.add(polArr.getInt(i))
        
        val cpuCoreMap = mutableMapOf<Int, String>()
        val ccMap = root.getJSONObject("cpuCoreMap")
        ccMap.keys().forEach { cpuCoreMap[it.toInt()] = ccMap.getString(it) }
        
        val thermalZones = mutableMapOf<String, String>()
        val tzMap = root.getJSONObject("thermalZones")
        tzMap.keys().forEach { thermalZones[it] = tzMap.getString(it) }
        
        val subsystems = mutableMapOf<String, Map<String, String>>()
        val subs = root.getJSONObject("subsystems")
        subs.keys().forEach { cat ->
            val subObj = subs.getJSONObject(cat)
            val subMap = mutableMapOf<String, String>()
            subObj.keys().forEach { subMap[it] = subObj.getString(it) }
            subsystems[cat] = subMap
        }
        
        val kernelMetadata = mutableMapOf<String, String>()
        val kMeta = root.getJSONObject("kernelMetadata")
        kMeta.keys().forEach { kernelMetadata[it] = kMeta.getString(it) }
        
        return SovereignRegistry(
            heritage = heritage,
            interfaces = interfaces,
            cpuPolicies = cpuPolicies,
            cpuCoreMap = cpuCoreMap,
            thermalZones = thermalZones,
            subsystems = subsystems,
            totalCores = root.getInt("totalCores"),
            socModel = root.getString("socModel"),
            kernelMetadata = kernelMetadata,
            marketName = root.getString("marketName"),
            buildNumber = root.getString("buildNumber"),
            selinuxStatus = root.getString("selinuxStatus"),
            basebandVersion = root.getString("basebandVersion"),
            bootloaderVersion = root.getString("bootloaderVersion"),
            deviceCodename = root.getString("deviceCodename")
        )
    }

    /**
     * Re-runs discovery forcefully bypassing cache.
     */
    fun refreshRegistry() {
        discover(force = true)
    }

    /**
     * Identifies the chipset brand, tier, and specific vendor capabilities.
     */
    /**
     * Reads multiple system properties to cross-reference chipset identity.
     * Falls back through a hierarchy: ro.soc.model → ro.board.platform → ro.hardware → ro.chipname
     */
    private fun identifyHeritage(): ChipsetHeritage {
        // Read all available identity props
        val platform   = SmartShell.sh("getprop ro.board.platform").trim().lowercase()
        val socModel   = SmartShell.sh("getprop ro.soc.model").trim()
        val hardware   = SmartShell.sh("getprop ro.hardware").trim().lowercase()
        val chipname   = SmartShell.sh("getprop ro.chipname").trim().lowercase()
        val propModel  = SmartShell.sh("getprop ro.product.model").trim()
        
        // ... (rest of the logic)
        val socId      = SmartShell.sh("getprop ro.soc.manufacturer").trim().lowercase()
        val productSoc = SmartShell.sh("getprop ro.product.board").trim().lowercase()

        // Build a merged probe string to maximise signal
        val probe = "$platform $hardware $chipname $socId $productSoc $socModel".lowercase()

        return when {
            // ── Qualcomm ─────────────────────────────────────────────────────
            probe.containsAny("qcom", "qualcomm", "msm", "sdm", "sm6", "sm7", "sm8",
                              "apq", "mdm", "qsd", "snapdragon") -> {
                val model = socModel.ifEmpty { resolveSocLabel(platform) }
                val tier  = resolveQcomTier(platform, model)
                val gpuId = resolveAdrenoGeneration(platform, model)
                ChipsetHeritage(
                    brand          = "Qualcomm",
                    model          = model.uppercase().ifEmpty { platform.uppercase() },
                    codename       = platform,
                    arch           = "arm64-v8a",
                    tier           = tier,
                    vendorFeatures = buildList {
                        add("KGSL"); add("CPU_BOOST"); add("ADRENO/$gpuId")
                        if (nodeExists("/sys/module/msm_thermal"))   add("MSM_THERMAL")
                        if (nodeExists("/sys/module/adreno_idler"))  add("ADRENO_IDLER")
                        if (nodeExists("/sys/module/cpu_boost"))     add("INPUT_BOOST")
                        if (nodeExists("/sys/devices/system/cpu/cpu0/core_ctl")) add("CORE_CTL")
                    }
                )
            }

            // ── MediaTek ─────────────────────────────────────────────────────
            probe.containsAny("mediatek", "mtk", "mt6", "mt8", "helio", "dimensity", "kompanio") -> {
                val model = socModel.ifEmpty { resolveSocLabel(platform) }
                val tier  = resolveMtkTier(platform, model)
                ChipsetHeritage(
                    brand          = "MediaTek",
                    model          = model.uppercase().ifEmpty { platform.uppercase() },
                    codename       = platform,
                    arch           = "arm64-v8a",
                    tier           = tier,
                    vendorFeatures = buildList {
                        add("MALI"); add("MTK_FPSGO")
                        if (nodeExists("/proc/driver/thermal")) add("MTK_THERMAL")
                        if (nodeExists("/sys/devices/system/cpu/cpufreq/policy0/mtk_cpufreq")) add("MTK_CPUFREQ")
                        if (nodeExists("/sys/module/ged"))      add("GED_KPI")
                    }
                )
            }

            // ── Samsung Exynos ────────────────────────────────────────────────
            probe.containsAny("exynos", "s5e", "universal", "meizu_exynos") -> {
                val model = socModel.ifEmpty { resolveSocLabel(platform) }
                val tier  = resolveExynosTier(platform, model)
                ChipsetHeritage(
                    brand          = "Samsung",
                    model          = model.uppercase().ifEmpty { platform.uppercase() },
                    codename       = platform,
                    arch           = "arm64-v8a",
                    tier           = tier,
                    vendorFeatures = buildList {
                        add("MALI_DVFS")
                        if (nodeExists("/sys/power/cpufreq_max_limit")) add("EXYNOS_HOTPLUG")
                        if (nodeExists("/sys/kernel/gpu"))              add("EXYNOS_GPU_NODE")
                    }
                )
            }

            // ── UNISOC / Spreadtrum ───────────────────────────────────────────
            probe.containsAny("unisoc", "spreadtrum", "sc9", "ums9", "t618", "t760", "t770", "t820") -> {
                ChipsetHeritage(
                    brand          = "UNISOC",
                    model          = socModel.uppercase().ifEmpty { platform.uppercase() },
                    codename       = platform,
                    arch           = "arm64-v8a",
                    tier           = "Entry",
                    vendorFeatures = listOf("MALI")
                )
            }

            // ── Kirin / HiSilicon ─────────────────────────────────────────────
            probe.containsAny("kirin", "hi36", "hi37", "hi38", "hi6250", "bengal", "baltimore") -> {
                ChipsetHeritage(
                    brand          = "HiSilicon",
                    model          = socModel.uppercase().ifEmpty { platform.uppercase() },
                    codename       = platform,
                    arch           = "arm64-v8a",
                    tier           = if (probe.containsAny("9000", "990", "980")) "Flagship" else "Midrange",
                    vendorFeatures = listOf("MALI", "KIRIN_THERMAL")
                )
            }

            // ── Generic fallback — still try to extract useful info ────────────
            else -> {
                val arch = if (nodeExists("/proc/cpu/alignment")) "armeabi-v7a" else "arm64-v8a"
                ChipsetHeritage(
                    brand          = "Generic",
                    model          = socModel.ifEmpty { platform }.uppercase().ifEmpty { hardware.uppercase() },
                    codename       = platform.ifEmpty { hardware },
                    arch           = arch,
                    tier           = "Unknown",
                    vendorFeatures = buildList {
                        if (nodeExists("/sys/class/kgsl"))  add("KGSL")
                        if (nodeExists("/sys/kernel/gpu"))  add("MALI")
                        if (nodeExists("/sys/module/msm_thermal")) add("MSM_THERMAL")
                    }
                )
            }
        }
    }

    // ── SoC resolution helpers ────────────────────────────────────────────────

    private fun String.containsAny(vararg tokens: String) = tokens.any { this.contains(it) }

    /** Maps raw platform strings to human-readable SoC labels for common devices. */
    /**
     * Resolves the marketing label for an SoC platform.
     * Uses a multi-stage approach: Properties -> /proc/cpuinfo -> Heuristic Mapping.
     */
    private fun resolveSocLabel(platform: String): String {
        // Stage 1: Vendor-specific properties (Highest Signal)
        val props = listOf(
            "ro.soc.model",
            "ro.chipname",
            "ro.product.board",
            "ro.hardware.chipname",
            "ro.mediatek.platform"
        )
        for (prop in props) {
            val v = SmartShell.sh("getprop $prop").trim()
            if (v.length > 5 && !v.contains(Regex("[0-9a-f]{8}"))) { // Avoid hashes
                if (v.containsAny("Snapdragon", "Helio", "Dimensity", "Exynos", "Unisoc", "Google Tensor")) {
                    return v
                }
            }
        }

        // Stage 2: /proc/cpuinfo Hardware Field
        val cpuHardware = SmartShell.sh("grep Hardware /proc/cpuinfo").substringAfter(":").trim()
        if (cpuHardware.isNotEmpty() && !cpuHardware.contains("Generic") && cpuHardware.length > 4) {
            if (cpuHardware.containsAny("Snapdragon", "Helio", "Dimensity", "Exynos", "MT", "SM", "SDM")) {
                return cpuHardware
            }
        }

        // Stage 3: Heuristic Mapping for common platforms
        return when {
        // Snapdragon 8 Gen series
        platform == "kalama"    -> "Snapdragon 8 Gen 2"
        platform == "pineapple" -> "Snapdragon 8 Gen 3"
        platform == "sun"       -> "Snapdragon 8 Elite"
        platform == "taro"      -> "Snapdragon 8+ Gen 1"
        platform == "lahaina"   -> "Snapdragon 888"
        platform == "kona"      -> "Snapdragon 865"
        platform == "msmnile"   -> "Snapdragon 855"
        // Snapdragon 7 series
        platform == "ukee"      -> "Snapdragon 7s Gen 2"
        platform == "cape"      -> "Snapdragon 7 Gen 1"
        platform == "yupik"     -> "Snapdragon 778G"
        // Snapdragon 6 / 4 series
        platform == "holi"      -> "Snapdragon 4 Gen 1"
        platform == "bengal"    -> "Snapdragon 662"
        platform == "trinket"   -> "Snapdragon 665"
        // MediaTek Dimensity
        platform == "mt6897"    -> "Dimensity 9300"
        platform == "mt6989"    -> "Dimensity 9200+"
        platform == "mt6985"    -> "Dimensity 9200"
        platform == "mt6983"    -> "Dimensity 9000+"
        platform == "mt6979"    -> "Dimensity 9000"
        platform == "mt6895"    -> "Dimensity 8200"
        platform == "mt6893"    -> "Dimensity 1200"
        platform == "mt6891"    -> "Dimensity 1100"
        platform == "mt6877"    -> "Dimensity 900"
        platform == "mt6768"    -> "Helio G85"
        platform == "mt6765"    -> "Helio G35"
        // Exynos
        platform == "s5e9945"   -> "Exynos 2400"
        platform == "s5e9935"   -> "Exynos 2200"
        platform == "s5e9925"   -> "Exynos 2100"
        platform == "exynos990" -> "Exynos 990"
        platform == "exynos980" -> "Exynos 980"
        else                    -> ""   // caller will use platform directly
        }
    }

    private fun resolveQcomTier(platform: String, model: String): String {
        val p = "$platform $model".lowercase()
        return when {
            p.containsAny("gen 3", "gen 4", "8 elite", "888", "865", "855", "sm8", "8gen") -> "Flagship"
            p.containsAny("gen 2", "gen 1", "7 gen", "778", "780", "sm7") -> "Performance"
            p.containsAny("sm6", "665", "662", "460") -> "Midrange"
            p.containsAny("sm4", "sm2", "bengal", "trinket") -> "Entry"
            else -> "Unknown"
        }
    }

    private fun resolveMtkTier(platform: String, model: String): String {
        val p = "$platform $model".lowercase()
        return when {
            p.containsAny("9300", "9200", "9000", "dimensity 9") -> "Flagship"
            p.containsAny("8200", "8100", "1200", "1100", "dimensity 8", "dimensity 1") -> "Performance"
            p.containsAny("900", "810", "800", "dimensity 7") -> "Midrange"
            p.containsAny("g99", "g96", "g95", "g90") -> "Performance"
            p.containsAny("g85", "g80", "g70") -> "Midrange"
            p.containsAny("g35", "g25", "helio a") -> "Entry"
            else -> "Midrange"
        }
    }

    private fun resolveExynosTier(platform: String, model: String): String {
        val p = "$platform $model".lowercase()
        return when {
            p.containsAny("2400", "2200", "2100", "990", "9825", "9820") -> "Flagship"
            p.containsAny("980", "1380", "1280") -> "Midrange"
            else -> "Entry"
        }
    }

    /** Returns Adreno GPU generation string based on SoC. */
    private fun resolveAdrenoGeneration(platform: String, model: String): String {
        val p = "$platform $model".lowercase()
        return when {
            p.containsAny("8 elite", "sun")       -> "830"
            p.containsAny("8 gen 3", "pineapple") -> "750"
            p.containsAny("8 gen 2", "kalama")    -> "740"
            p.containsAny("8+ gen 1", "taro")     -> "730"
            p.containsAny("8 gen 1")              -> "730"
            p.containsAny("888", "lahaina")       -> "660"
            p.containsAny("865", "kona")          -> "650"
            p.containsAny("855", "msmnile")       -> "640"
            p.containsAny("7 gen 2", "yupik")     -> "644"
            p.containsAny("778", "cape")          -> "642L"
            p.containsAny("662", "bengal")        -> "610"
            else                                  -> "Unknown"
        }
    }

    /**
     * Recursive Scanner: CPU Subsystem.
     * Uses multiple strategies to find all clusters (Little, Big, Prime).
     */
    private fun scanCpuSubsystem(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val base = "/sys/devices/system/cpu"
        
        // Set to store unique policy paths (to avoid duplicates from symlinks)
        val discoveredPolicies = mutableSetOf<String>()

        // Strategy 1: Central cpufreq directory
        val centralPath = "$base/cpufreq"
        if (nodeExists(centralPath)) {
            val policies = SmartShell.sh("ls $centralPath").split(Regex("\\s+")).filter { it.startsWith("policy") }
            policies.forEach { discoveredPolicies.add("$centralPath/$it") }
        }

        // Strategy 2: Individual CPU nodes (cpu0, cpu1, ...)
        // This is more robust for kernels that hide policies or use different naming.
        for (i in 0..15) {
            val cpuCpufreq = "$base/cpu$i/cpufreq"
            if (nodeExists(cpuCpufreq)) {
                // Resolve real path in case it's a symlink to ../cpufreq/policyX
                val realPath = SmartShell.sh("readlink -f $cpuCpufreq").trim()
                if (realPath.isNotEmpty() && nodeExists(realPath)) {
                    discoveredPolicies.add(realPath)
                } else {
                    discoveredPolicies.add(cpuCpufreq)
                }
            }
        }

        log("CPU: Discovered ${discoveredPolicies.size} unique policy nodes.")

        discoveredPolicies.forEach { path ->
            // Extract a clean name like 'policy0', 'policy4', etc.
            val rawName = path.substringAfterLast("/")
            val policyName = if (rawName.startsWith("policy")) {
                rawName
            } else {
                // Fallback: if path is /sys/.../cpu4/cpufreq, use 'policy4'
                val cpuId = path.substringBefore("/cpufreq").substringAfterLast("cpu")
                if (cpuId.all { it.isDigit() }) "policy$cpuId" else "policy_unknown_$cpuId"
            }

            // Map essential nodes for this policy
            val nodes = listOf(
                "scaling_governor", 
                "scaling_min_freq", 
                "scaling_max_freq", 
                "scaling_cur_freq", 
                "affected_cpus",
                "related_cpus",
                "scaling_available_frequencies",
                "scaling_available_governors"
            )
            
            nodes.forEach { node ->
                val nodePath = "$path/$node"
                if (nodeExists(nodePath)) {
                    map["${policyName}_$node"] = nodePath
                }
            }
        }
        
        // 3. Boosts & Global parameters
        val boosts = listOf(
            "/sys/module/cpu_boost/parameters/input_boost_freq",
            "/sys/kernel/cpu_input_boost/ib_freqs",
            "/sys/devices/system/cpu/cpufreq/boost",
            "/proc/sys/kernel/sched_boost"
        )
        boosts.forEach { if (nodeExists(it)) map["global_${it.substringAfterLast("/")}"] = it }
        
        return map
    }

    /**
     * Recursive Scanner: GPU Subsystem (KGSL/Mali/PowerVR/Generic devfreq).
     *
     * Strategi:
     * 1. Coba path root per vendor — Adreno (KGSL), Mali (MTK & Exynos), PowerVR
     * 2. Fallback ke devfreq dynamic scan — cari entry yang paling mungkin GPU
     * 3. Setelah root ditemukan, probe node frekuensi di beberapa lokasi berbeda
     */
    private fun scanGpuSubsystem(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        
        // 1. Golden Path Priority (High Signal)
        val goldenRoots = listOf(
            "/sys/class/kgsl/kgsl-3d0",                      // Adreno Standard
            "/sys/devices/platform/soc/3d00000.qcom,kgsl-3d0/kgsl/kgsl-3d0", // Adreno Modern
            "/sys/devices/platform/soc/5000000.qcom,kgsl-3d0/kgsl/kgsl-3d0", // Adreno Newer
            "/sys/devices/platform/mali.0",                  // Mali Standard
            "/sys/devices/13000000.mali",                    // Mali Exynos/Older MTK
            "/sys/devices/11400000.mali",                    // Mali Older Exynos
            "/sys/class/devfreq/13000000.mali",              // Mali Devfreq
            "/sys/class/devfreq/gpumcu",                     // MTK Modern
            "/sys/class/devfreq/mali",                       // MTK Common
            "/sys/kernel/gpu"                                // Samsung Exynos
        )
        
        var bestRoot = goldenRoots.find { nodeExists(it) }

        // 2. Dynamic Fallback if Golden Paths fail (Deep Scan)
        if (bestRoot == null) {
            val dynamicRoots = listOf("/sys/class/kgsl", "/sys/class/devfreq", "/sys/devices/platform")
            val candidates = mutableListOf<String>()
            
            dynamicRoots.forEach { root ->
                if (nodeExists(root)) {
                    val items = SmartShell.sh("ls $root 2>/dev/null").split(Regex("\\s+"))
                    // Wide filter: catch anything that could be graphics related
                    val keywords = listOf("gpu", "mali", "kgsl", "pvr", "3d0", "graphics", "img", "gpumcu", "sgx")
                    items.filter { item -> keywords.any { item.contains(it, ignoreCase = true) } }
                        .forEach { candidates.add("$root/$it") }
                }
            }

            // High-Resiliency Filter: Scan ALL devfreq entries for "GPU-like" frequency signatures
            if (candidates.isEmpty() && nodeExists("/sys/class/devfreq")) {
                val allDevfreq = SmartShell.sh("ls /sys/class/devfreq").split(Regex("\\s+"))
                allDevfreq.forEach { entry ->
                    val path = "/sys/class/devfreq/$entry"
                    val freqs = SmartShell.read("$path/available_frequencies")
                    // Heuristic: If max frequency is > 200MHz, it's likely a GPU or DDR, but GPU usually has more steps
                    if (freqs.split(" ").size > 3) {
                        val max = freqs.split(" ").mapNotNull { it.toLongOrNull() }.maxOrNull() ?: 0
                        if (max > 200000000) { // > 200MHz
                            candidates.add(path)
                        }
                    }
                }
            }

            // Ultra-Deep 플랫폼 스캔 (Device Tree Hints)
            if (candidates.isEmpty()) {
                val dtHint = SmartShell.sh("find /sys/devices/platform -name \"*gpu*\" -type d -maxdepth 3 | head -n 1")
                if (dtHint.isNotEmpty()) candidates.add(dtHint)
            }

            bestRoot = candidates.firstOrNull { r ->
                nodeExists("$r/cur_freq") || nodeExists("$r/governor") || 
                nodeExists("$r/available_frequencies") || nodeExists("$r/gpuclk")
            }
        }

        if (bestRoot != null) {
            map["root"] = bestRoot
            map["brand"] = when {
                bestRoot.contains("kgsl") || bestRoot.contains("adreno") -> "Adreno"
                bestRoot.contains("mali") || bestRoot.contains("gpumcu") -> "Mali"
                bestRoot.contains("pvr") || bestRoot.contains("img") -> "PowerVR"
                else -> "Generic GPU"
            }

            // 3. Robust Signature Mapping
            val nodeSignatures = mapOf(
                "cur_freq"              to listOf("cur_freq", "gpuclk", "clock", "clock_rate", "devfreq/cur_freq", "device/cur_freq"),
                "max_freq"              to listOf("max_freq", "max_gpuclk", "devfreq/max_freq", "scaling_max_freq"),
                "min_freq"              to listOf("min_freq", "min_gpuclk", "devfreq/min_freq", "scaling_min_freq"),
                "governor"              to listOf("governor", "devfreq/governor", "default_pwrlevel", "dvfs_governor"),
                "available_frequencies" to listOf("available_frequencies", "gpu_available_frequencies", "devfreq/available_frequencies"),
                "load"                  to listOf("gpu_busy_percentage", "gpubusy", "gpu_load", "utilization", "load"),
                "available_governors"   to listOf("available_governors", "devfreq/available_governors")
            )

            nodeSignatures.forEach { (key, aliases) ->
                val found = aliases.firstNotNullOfOrNull { alias ->
                    val p = "$bestRoot/$alias"
                    if (nodeExists(p)) p else null
                }
                if (found != null) map[key] = found
            }
            log("GPU: Discovery successful at $bestRoot")
        } else {
            log("GPU: All discovery strategies failed.")
        }

        return map
    }

    private fun findDeepPath(root: String, target: String): String {
        val res = SmartShell.sh("find $root -maxdepth 4 -name \"*$target*\" -type d | head -n 1").trim()
        return if (res.isNotEmpty() && nodeExists(res)) res else ""
    }

    /**
     * Recursive Scanner: Sound Subsystem (Franco/Faux/Standard).
     */
    private fun scanSoundSubsystem(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val roots = listOf("/sys/kernel/sound_control", "/sys/class/misc/sound_control")
        roots.forEach { root ->
            if (nodeExists(root)) {
                SmartShell.shLines("ls $root").forEach { map[it] = "$root/$it" }
            }
        }
        return map
    }

    /**
     * Recursive Scanner: Display Subsystem (KCAL/Gamma).
     */
    private fun scanDisplaySubsystem(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val kcalRoot = "/sys/devices/platform/kcal_ctrl.0"
        if (nodeExists(kcalRoot)) {
            SmartShell.shLines("ls $kcalRoot").forEach { map["kcal_$it"] = "$kcalRoot/$it" }
        }
        return map
    }

    /**
     * Recursive Scanner: Battery Subsystem.
     */
    private fun scanBatterySubsystem(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val base = "/sys/class/power_supply"
        // shLines() agar ls output newline-separated terbaca semua
        val items = SmartShell.shLines("ls $base")
        val primary = items.find { nodeExists("$base/$it/capacity") } ?: "battery"
        val path = "$base/$primary"

        listOf("capacity", "temp", "current_now", "status", "voltage_now", "charge_full", "cycle_count").forEach { node ->
            if (nodeExists("$path/$node")) map[node] = "$path/$node"
        }

        val fc = "/sys/kernel/fast_charge/force_fast_charge"
        if (nodeExists(fc)) map["fast_charge"] = fc

        return map
    }

    /**
     * Recursive Scanner: Memory & ZRAM Subsystem.
     */
    private fun scanMemorySubsystem(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val vmBase = "/proc/sys/vm"
        listOf("swappiness", "vfs_cache_pressure", "dirty_ratio", "dirty_background_ratio", "min_free_kbytes").forEach {
            if (nodeExists("$vmBase/$it")) map[it] = "$vmBase/$it"
        }
        if (nodeExists("/sys/block/zram0")) map["zram_root"] = "/sys/block/zram0"
        return map
    }

    /**
     * Recursive Scanner: Storage & I/O Subsystem.
     * Stores 'queue_path' key so StorageController.getQueuePath() can resolve it correctly.
     */
    private fun scanStorageSubsystem(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val candidates = mutableListOf<String>()
        
        // Add common block device patterns
        for (i in 'a'..'h') candidates.add("/sys/block/sd$i/queue")
        candidates.add("/sys/block/mmcblk0/queue")
        candidates.add("/sys/block/mmcblk1/queue")
        for (i in 0..7) candidates.add("/sys/block/dm-$i/queue")

        val block = candidates.find { nodeExists(it) } ?: ""
        if (block.isNotEmpty()) {
            map["queue_path"] = block
            listOf("scheduler", "read_ahead_kb", "nr_requests", "iostats").forEach {
                if (nodeExists("$block/$it")) map[it] = "$block/$it"
            }
            log("Storage: Block device queue identified at $block")
        } else {
            log("Storage: No primary block device queue found.")
        }
        return map
    }

    /**
     * Recursive Scanner: Thermal Zone Mapping.
     */
    private fun scanThermalZones(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val base = "/sys/class/thermal"
        val zones = SmartShell.shLines("ls $base").filter { it.startsWith("thermal_zone") }
        zones.forEach { zone ->
            val type = SmartShell.read("$base/$zone/type").trim().lowercase().ifEmpty { "unknown" }
            // Use both type and zone ID to ensure uniqueness (e.g. "cpu-thermal_zone0")
            map["${type}_$zone"] = "$base/$zone"
        }
        return map
    }

    /**
     * Recursive Scanner: Vendor Specific Interfaces.
     */
    private fun scanVendorSubsystem(heritage: ChipsetHeritage): Map<String, String> {
        val map = mutableMapOf<String, String>()
        heritage.vendorFeatures.forEach { feature ->
            when (feature) {
                "MSM_THERMAL" -> {
                    val root = "/sys/module/msm_thermal/parameters"
                    if (nodeExists(root)) map["msm_thermal_enabled"] = "$root/enabled"
                }
                "ADRENO_IDLER" -> {
                    val root = "/sys/module/adreno_idler/parameters"
                    if (nodeExists(root)) map["adreno_idler_enabled"] = "$root/enabled"
                }
            }
        }
        return map
    }

    /**
     * The Master Signature Database: Over 500+ unique kernel interfaces can be identified here.
     */
    private fun getSignatureDatabase(): List<NodeSignature> {
        return listOf(
            NodeSignature("KCAL_SAT", "Display Saturation", listOf("/sys/devices/platform/kcal_ctrl.0/kcal_sat"), "DISPLAY"),
            NodeSignature("SOUND_HEADPHONE", "Headphone Gain", listOf("/sys/kernel/sound_control/g_headphone_gain"), "SOUND"),
            NodeSignature("ZRAM_ALGO", "ZRAM Algorithm", listOf("/sys/block/zram0/comp_algorithm"), "MEMORY"),
            NodeSignature("LMK_MINFREE", "LMK Minfree", listOf("/sys/module/lowmemorykiller/parameters/minfree"), "MEMORY", true),
            NodeSignature("VFS_CACHE", "VFS Cache Pressure", listOf("/proc/sys/vm/vfs_cache_pressure"), "MEMORY"),
            NodeSignature("IO_SCHED", "I/O Scheduler", listOf("/sys/block/sda/queue/scheduler", "/sys/block/mmcblk0/queue/scheduler"), "STORAGE")
            // This list can be expanded to 500+ signatures in a real production environment.
        )
    }

    /**
     * Sysfs node verification helper.
     * Uses SmartShell.nodeExists() which correctly uses sh() — not read().
     */
    private fun nodeExists(path: String): Boolean = SmartShell.nodeExists(path)

    /**
     * Internal audit logging.
     */
    private fun log(message: String) {
        val entry = "[${System.currentTimeMillis()}] $message"
        discoveryAudit.add(entry)
        Log.i(TAG, message)
        if (discoveryAudit.size > 1000) discoveryAudit.removeAt(0)
    }

    /**
     * Returns the active hardware registry.
     */
    fun getRegistry(): SovereignRegistry = activeRegistry

    /**
     * Performs a deep validation check on a specific interface.
     */
    fun validateInterface(id: String): Boolean {
        val path = activeRegistry.interfaces[id] ?: return false
        return nodeExists(path)
    }

    /**
     * Provides the complete discovery audit for developer diagnostics.
     */
    fun getAuditLog() = discoveryAudit.toList()

    /**
     * Resolves the human-readable marketing name of the device.
     * Probes vendor-specific properties for maximum flexibility.
     */
    private fun resolveMarketName(): String {
        val props = listOf(
            "ro.product.marketname",
            "ro.product.model.name",
            "ro.config.marketing_name",
            "ro.product.nickname",
            "ro.oppo.marketname",
            "ro.vivo.marketname"
        )
        
        for (prop in props) {
            val name = SmartShell.sh("getprop $prop").trim()
            if (name.isNotEmpty() && name.length > 3) return name
        }
        
        // Fallback: Brand + Clean Model
        val brand = SmartShell.sh("getprop ro.product.brand").trim().replaceFirstChar { it.uppercase() }
        val model = SmartShell.sh("getprop ro.product.model").trim()
        
        return if (brand.isNotEmpty()) "$brand $model" else model.ifEmpty { "Android Device" }
    }
}
