package com.tbtechsdev.lexiread.data.di

import android.content.Context
import com.tbtechsdev.lexiread.data.db.AppDatabase
import com.tbtechsdev.lexiread.data.db.dao.CachedDefinitionDao
import com.tbtechsdev.lexiread.data.db.dao.DictionaryDao
import com.tbtechsdev.lexiread.data.db.dao.UserWordDao
import com.tbtechsdev.lexiread.data.dictionary.DictionaryRepository
import com.tbtechsdev.lexiread.data.dictionary.IDictionaryRepository
import com.tbtechsdev.lexiread.data.vocabulary.IUserWordRepository
import com.tbtechsdev.lexiread.data.vocabulary.UserWordRepository
import com.tbtechsdev.lexiread.data.vocabulary.UserWordRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindIDictionaryRepository(
        impl: DictionaryRepository
    ): IDictionaryRepository

    @Binds
    @Singleton
    abstract fun bindUserWordRepository(
        impl: UserWordRepositoryImpl
    ): UserWordRepository

    @Binds
    @Singleton
    abstract fun bindIUserWordRepository(
        impl: UserWordRepository
    ): IUserWordRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return AppDatabase.getInstance(context)
        }

        @Provides
        fun provideCachedDefinitionDao(database: AppDatabase): CachedDefinitionDao {
            return database.cachedDefinitionDao()
        }

        @Provides
        fun provideDictionaryDao(database: AppDatabase): DictionaryDao {
            return database.dictionaryDao()
        }

        @Provides
        fun provideUserWordDao(database: AppDatabase): UserWordDao {
            return database.userWordDao()
        }
    }
}
