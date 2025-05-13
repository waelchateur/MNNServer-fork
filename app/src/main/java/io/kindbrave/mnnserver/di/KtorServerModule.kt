package io.kindbrave.mnnserver.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.kindbrave.mnnserver.webserver.KtorServer
import io.kindbrave.mnnserver.webserver.MNNHandler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KtorServerModule {

    @Provides
    @Singleton
    fun provideKtorServer(
        mnnHandler: MNNHandler,
    ): KtorServer {
        return KtorServer(mnnHandler)
    }
}