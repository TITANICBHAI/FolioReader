package com.tbtechsdev.lexiread.data.di

import com.tbtechsdev.lexiread.data.ai.GeminiRepository
import com.tbtechsdev.lexiread.data.ai.IGeminiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindGeminiRepository(
        impl: GeminiRepository
    ): IGeminiRepository
}
