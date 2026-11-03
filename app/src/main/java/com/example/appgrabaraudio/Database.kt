package com.example.appgrabaraudio

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "audio_records")
data class AudioRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val fileName: String,
    val timestamp: Long
)

@Dao
interface RecordingDao {
    @Insert
    suspend fun insertRecording(record: AudioRecord)

    @Query("SELECT * FROM audio_records ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<AudioRecord>>

    @Query("SELECT * FROM audio_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastRecording(): AudioRecord?
}

@Database(entities = [AudioRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "audio_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
