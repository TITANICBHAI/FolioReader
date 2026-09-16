package com.tbtechsdev.lexiread.data.di

import com.tbtechsdev.lexiread.data.translation.ITranslationRepository
import com.tbtechsdev.lexiread.data.translation.TranslationRepository
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
