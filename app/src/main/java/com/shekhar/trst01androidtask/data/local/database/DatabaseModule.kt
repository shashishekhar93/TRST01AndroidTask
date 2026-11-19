package com.shekhar.trst01androidtask.data.local.database

import android.content.Context
import androidx.room.Room
import com.shekhar.trst01androidtask.data.local.dao.DocumentDao
import com.shekhar.trst01androidtask.data.local.dao.EmployeeDao
import com.shekhar.trst01androidtask.data.local.dao.MapAreaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideEmployeeDao(database: AppDatabase): EmployeeDao {
        return database.employeeDao()
    }

    @Provides
    @Singleton
    fun provideDocumentDao(database: AppDatabase): DocumentDao {
        return database.documentDao()
    }

    @Provides
    @Singleton
    fun provideMapAreaDao(database: AppDatabase): MapAreaDao {
        return database.mapAreaDao()
    }
}
