package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.MemoryManager
import com.example.data.local.SecureKeyStore
import com.example.data.repository.BoardroomRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BoardroomApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var memoryManager: MemoryManager
        private set

    lateinit var secureKeyStore: SecureKeyStore
        private set

    lateinit var repository: BoardroomRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "boardroom_database.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()

        memoryManager = MemoryManager(applicationContext)
        secureKeyStore = SecureKeyStore(applicationContext)
        repository = BoardroomRepository(database, memoryManager, applicationContext, secureKeyStore)

        CoroutineScope(Dispatchers.IO).launch {
            repository.initializeDefaultDataIfEmpty()
        }
    }
}
