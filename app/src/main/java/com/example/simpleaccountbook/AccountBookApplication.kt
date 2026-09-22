package com.example.simpleaccountbook

import android.app.Application
import androidx.room.Room
import com.example.simpleaccountbook.data.AppDatabase
import com.example.simpleaccountbook.data.AppRepository
import com.example.simpleaccountbook.data.SettingsStore

class AccountBookApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "account-book.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    val repository by lazy { AppRepository(database.appDao()) }
    val settingsStore by lazy { SettingsStore(this) }
}
