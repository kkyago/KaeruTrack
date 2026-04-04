package com.kaeru.app.tracking.database

@androidx.room.Entity(tableName = "backup_logs")
data class BackupLog(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val action: String,
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@androidx.room.Dao
interface BackupLogDao {
    @androidx.room.Query("SELECT * FROM backup_logs ORDER BY timestamp DESC")
    fun getAllLogs(): kotlinx.coroutines.flow.Flow<List<BackupLog>>

    @androidx.room.Insert
    suspend fun insertLog(log: BackupLog)

    @androidx.room.Query("DELETE FROM backup_logs")
    suspend fun clearHistory()
}