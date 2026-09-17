package com.tbtechs.folioreader.data.di

import android.content.Context
import com.tbtechs.folioreader.data.db.AppDatabase
import com.tbtechs.folioreader.data.db.dao.CachedDefinitionDao
import com.tbtechs.folioreader.data.db.dao.DictionaryDao
import com.tbtechs.folioreader.data.db.dao.UserWordDao
import com.tbtechs.folioreader.data.dictionary.DictionaryRepository
import com.tbtechs.folioreader.data.dictionary.IDictionaryRepository
import com.tbtechs.folioreader.data.vocabulary.IUserWordRepository
import com.tbtechs.folioreader.data.vocabulary.UserWordRepository
import com.tbtechs.folioreader.data.vocabulary.UserWordRepositoryImpl
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
