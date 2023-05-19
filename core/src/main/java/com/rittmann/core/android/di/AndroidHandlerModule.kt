package com.rittmann.core.android.di

import android.content.Context
import com.rittmann.core.android.AndroidHandler
import com.rittmann.core.android.AndroidHandlerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AndroidHandlerModule {

    @Provides
    @Singleton
    fun providesExecutorService(): ExecutorService = Executors.newSingleThreadExecutor()

    @Provides
    @Singleton
    fun providesAndroidHandler(
        @ApplicationContext context: Context,
        executorService: ExecutorService,
    ): AndroidHandler = AndroidHandlerFactory.create(context, executorService)
}