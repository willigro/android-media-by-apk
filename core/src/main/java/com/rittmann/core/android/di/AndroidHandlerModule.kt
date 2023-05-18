package com.rittmann.core.android.di

import android.content.Context
import com.rittmann.core.android.AndroidHandler
import com.rittmann.core.android.AndroidHandlerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AndroidHandlerModule {

    @Provides
    @Singleton
    fun providesAndroidHandler(
        @ApplicationContext context: Context
    ): AndroidHandler = AndroidHandlerFactory.create(context)
}