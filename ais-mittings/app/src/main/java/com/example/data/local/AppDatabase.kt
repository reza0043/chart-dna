package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.AdvisorEntity
import com.example.data.model.ChatMessage
import com.example.data.model.MasterFile
import com.example.data.model.MeetingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface CouncilDao {
    @Query("SELECT * FROM advisors ORDER BY id ASC")
    fun getAllAdvisors(): Flow<List<AdvisorEntity>>

    @Query("SELECT * FROM advisors WHERE id = :id")
    suspend fun getAdvisorById(id: Int): AdvisorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(advisors: List<AdvisorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(advisor: AdvisorEntity)

    @Update
    suspend fun update(advisor: AdvisorEntity)

    @Query("UPDATE advisors SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("UPDATE advisors SET latestReport = :report, status = :status WHERE id = :id")
    suspend fun updateReport(id: Int, report: String, status: String)

    @Query("UPDATE advisors SET isAllowedInMeeting = :allowed WHERE id = :id")
    suspend fun updatePermission(id: Int, allowed: Boolean)

    @Query("UPDATE advisors SET isTriageLead = CASE WHEN id = :triageId THEN 1 ELSE 0 END")
    suspend fun setTriageLead(triageId: Int)

    @Query("SELECT MAX(id) FROM advisors")
    suspend fun getMaxAdvisorId(): Int?

    @Query("DELETE FROM advisors WHERE id = :id")
    suspend fun deleteAdvisorById(id: Int)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM meetings ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<MeetingSession>>

    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getSessionById(id: Long): MeetingSession?

    @Query("SELECT * FROM meetings ORDER BY createdAt DESC LIMIT 1")
    fun getLatestSession(): Flow<MeetingSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MeetingSession): Long

    @Update
    suspend fun updateSession(session: MeetingSession)

    @Query("UPDATE meetings SET executiveSummary = :summary, finalResolution = :resolution WHERE id = :id")
    suspend fun updateResolution(id: Long, summary: String, resolution: String)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteSession(id: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)
}

@Dao
interface MasterFileDao {
    @Query("SELECT * FROM master_files ORDER BY addedTimestamp DESC")
    fun getAllMasterFiles(): Flow<List<MasterFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterFile(file: MasterFile): Long

    @Query("DELETE FROM master_files WHERE id = :id")
    suspend fun deleteMasterFile(id: Long)
}

@Database(
    entities = [
        AdvisorEntity::class,
        MeetingSession::class,
        ChatMessage::class,
        MasterFile::class
    ],
    // نسخهٔ ۲: تعداد کارگروه‌ها از ۲۰ ثابت به پویا (پیش‌فرض ۴ گروه دانش‌آموزی) تغییر کرد؛
    // با fallbackToDestructiveMigration نصب‌های قبلی هم با پیش‌فرض جدید بازسازی می‌شوند.
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun councilDao(): CouncilDao
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun masterFileDao(): MasterFileDao
}
