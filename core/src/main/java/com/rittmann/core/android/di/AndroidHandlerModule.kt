package com.rittmann.core.android.di

import com.rittmann.core.android.AndroidHandler
import com.rittmann.core.android.AndroidHandlerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AndroidHandlerModule {

    @Provides
    fun providesAndroidHandler(): AndroidHandler = AndroidHandlerFactory.create()
}