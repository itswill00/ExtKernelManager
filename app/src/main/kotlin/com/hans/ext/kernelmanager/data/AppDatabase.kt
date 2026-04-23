package com.hans.ext.kernelmanager.data

import androidx.room.*
import com.hans.ext.kernelmanager.data.entity.Setting
import com.hans.ext.kernelmanager.data.entity.Profile
import com.hans.ext.kernelmanager.data.entity.CustomScript
import com.hans.ext.kernelmanager.data.entity.SettingPersistence

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<Setting>

    @Query("SELECT * FROM settings WHERE applyOnBoot = 1")
    suspend fun getBootSettings(): List<Setting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: Setting)

    @Delete
    suspend fun delete(setting: Setting)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE packageName = :pkg")
    suspend fun getForApp(pkg: String): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile)
}

@Dao
interface ScriptDao {
    @Query("SELECT * FROM custom_scripts")
    suspend fun getAll(): List<CustomScript>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: CustomScript)

    @Delete
    suspend fun delete(script: CustomScript)
}

@Dao
interface PersistenceDao {
    @Query("SELECT * FROM setting_persistence")
    suspend fun getAll(): List<SettingPersistence>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(persistence: SettingPersistence)
}

@Database(entities = [Setting::class, Profile::class, CustomScript::class, SettingPersistence::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingDao(): SettingDao
    abstract fun profileDao(): ProfileDao
    abstract fun scriptDao(): ScriptDao
    abstract fun persistenceDao(): PersistenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ext_kernel_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
