#!/bin/bash

# =============================================================================
# Build & Upload to Telegram Script
# =============================================================================

# Konfigurasi Telegram
BOT_TOKEN="8780321748:AAFBdXQW8JWeR2lr3cpsyrydZz62lXBOExg"
CHAT_ID="-1003322434219"

# Konfigurasi Path
PROJECT_DIR="/mnt/f/New folder"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
OUT_DIR="$PROJECT_DIR/builds"
LOG_FILE="$PROJECT_DIR/build.log"

# Buat folder output jika belum ada
mkdir -p "$OUT_DIR"

# 1. Kirim Notif Build Dimulai
echo "🚀 Memulai proses build..."
START_TIME=$(date +"%d-%m-%Y %H:%M:%S")
curl -s -X POST "https://api.telegram.org/bot$BOT_TOKEN/sendMessage" \
     -d chat_id="$CHAT_ID" \
     -d text="🏗️ *Build Started*
📅 *Waktu:* \`$START_TIME\`
👤 *Build by:* @noticesa" \
     -d parse_mode="Markdown"

# Jalankan Gradle Build (Pakai --console=plain agar log muncul real-time di terminal)
cd "$PROJECT_DIR" || exit
gradle assembleDebug --console=plain 2>&1 | tee "$LOG_FILE"



# Cek apakah build sukses (mengambil exit code dari gradlew, bukan tee)
if [ ${PIPESTATUS[0]} -eq 0 ]; then

    echo "✅ Build Sukses!"
    
    # Ambil waktu sekarang untuk nama file unik
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    NEW_FILE_NAME="Build_${TIMESTAMP}_app-debug.apk"
    TARGET_PATH="$OUT_DIR/$NEW_FILE_NAME"
    
    # Salin dan rename file
    cp "$APK_PATH" "$TARGET_PATH"
    
    # Hitung Checksum
    MD5_SUM=$(md5sum "$TARGET_PATH" | cut -d ' ' -f 1)
    SHA256_SUM=$(sha256sum "$TARGET_PATH" | cut -d ' ' -f 1)
    
    echo "📦 Menghitung checksum..."
    
    # Kirim ke Telegram
    echo "📤 Mengunggah ke Telegram..."
    
    CAPTION=$(cat <<EOF
🚀 *Build Success!*

📄 *File:* \`$NEW_FILE_NAME\`
📅 *Selesai:* \`$(date +"%d-%m-%Y %H:%M:%S")\`
👤 *Build by:* @noticesa

🔐 *Checksum:*
• *MD5:* \`$MD5_SUM\`
• *SHA256:* \`$SHA256_SUM\`

#Build #Android #Success
EOF
)

    curl -F chat_id="$CHAT_ID" \
         -F document=@"$TARGET_PATH" \
         -F caption="$CAPTION" \
         -F parse_mode="Markdown" \
         "https://api.telegram.org/bot$BOT_TOKEN/sendDocument"

    echo ""
    echo "✨ Berhasil! Build telah diunggah ke Telegram."
    rm "$LOG_FILE" # Hapus log jika sukses
else
    echo "❌ Build Gagal!"
    
    # Kirim notifikasi gagal + Log File
    curl -F chat_id="$CHAT_ID" \
         -F document=@"$LOG_FILE" \
         -F caption="❌ *Build Failed!*
👤 *Build by:* @noticesa
📝 *Log detail terlampir.*" \
         -F parse_mode="Markdown" \
         "https://api.telegram.org/bot$BOT_TOKEN/sendDocument"
         
    exit 1
fi

