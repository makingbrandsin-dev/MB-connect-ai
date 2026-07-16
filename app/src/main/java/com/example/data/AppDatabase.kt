package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- DAOs ---

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(log: CallLogEntity)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLogById(id: Int)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCallLogs()
}

@Dao
interface FaqDao {
    @Query("SELECT * FROM faqs ORDER BY timestamp DESC")
    fun getAllFaqs(): Flow<List<FaqEntity>>

    @Query("SELECT * FROM faqs WHERE isEnabled = 1")
    suspend fun getEnabledFaqs(): List<FaqEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaq(faq: FaqEntity)

    @Query("DELETE FROM faqs WHERE id = :id")
    suspend fun deleteFaqById(id: Int)
}

@Dao
interface PriorityContactDao {
    @Query("SELECT * FROM priority_contacts ORDER BY id DESC")
    fun getAllPriorityContacts(): Flow<List<PriorityContactEntity>>

    @Query("SELECT * FROM priority_contacts WHERE isEnabled = 1")
    suspend fun getEnabledPriorityContacts(): List<PriorityContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriorityContact(contact: PriorityContactEntity)

    @Query("DELETE FROM priority_contacts WHERE id = :id")
    suspend fun deletePriorityContactById(id: Int)
}

// --- DATABASE CLASS ---

@Database(
    entities = [CallLogEntity::class, FaqEntity::class, PriorityContactEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
    abstract fun faqDao(): FaqDao
    abstract fun priorityContactDao(): PriorityContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mb_connect_secure_db"
                )
                .fallbackToDestructiveMigration() // Simple migration strategy for this prototype
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
