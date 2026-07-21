package com.kaeru.app.tracking.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.Serializable

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracking_history ADD COLUMN firstDate TEXT DEFAULT NULL")
    }
}
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracking_history ADD COLUMN cpf TEXT DEFAULT NULL")
    }
}
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `backup_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `action` TEXT NOT NULL, `fileName` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
        )
    }
}
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracking_history ADD COLUMN carrier TEXT NOT NULL DEFAULT 'Auto'")
    }
}
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracking_history ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 1")
    }
}

@Serializable
@Entity(tableName = "tracking_history")
data class TrackingEntity(
    @PrimaryKey val code: String,
    val description: String,
    val lastStatus: String,
    val lastDate: String,
    val firstDate: String? = null,
    val savedAt: Long = System.currentTimeMillis(),
    val cpf: String? = null,
    val carrier: String = "Auto",
    val notificationsEnabled: Boolean = true
)

@Dao
interface TrackingDao {
    @Query("SELECT * FROM tracking_history ORDER BY savedAt DESC")
    fun getAllTracking(): Flow<List<TrackingEntity>>
    @Query("SELECT savedAt FROM tracking_history")
    fun getAllTrackingDates(): Flow<List<Long>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracking(tracking: TrackingEntity)
    @Query("DELETE FROM tracking_history WHERE code = :code")
    suspend fun deleteTracking(code: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trackings: List<TrackingEntity>)
    @Query("UPDATE tracking_history SET description = :newDescription WHERE code = :code")
    suspend fun updateDescription(code: String, newDescription: String)
    @Query("SELECT cpf FROM tracking_history WHERE code = :code")
    suspend fun getCpf(code: String): String?
    @Query("SELECT * FROM tracking_history WHERE code = :code")
    suspend fun getTrackingById(code: String): TrackingEntity?
    @Query("UPDATE tracking_history SET notificationsEnabled = :enabled WHERE code = :code")
    suspend fun updateNotificationStatus(code: String, enabled: Boolean)
}
@Database(entities = [TrackingEntity::class, BackupLog::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackingDao(): TrackingDao
    abstract fun backupLogDao(): BackupLogDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kaeru_track_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}