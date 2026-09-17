package com.tbtechs.folioreader.data.di

import com.tbtechs.folioreader.data.translation.ITranslationRepository
import com.tbtechs.folioreader.data.translation.TranslationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TranslationModule {

    @Binds
    @Singleton
    abstract fun bindTranslationRepository(
        impl: TranslationRepository
    ): ITranslationRepository
}
