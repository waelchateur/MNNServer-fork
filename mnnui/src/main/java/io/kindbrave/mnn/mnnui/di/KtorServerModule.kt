package io.kindbrave.mnn.mnnui.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.kindbrave.mnn.webserver.webserver.KtorServer
import io.kindbrave.mnn.webserver.webserver.MNNHandler
import io.kindbrave.mnn.webserver.webserver.MNNTTSHandler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KtorServerModule {

    @Provides
    @Singleton
    fun provideKtorServer(
        mnnHandler: MNNHandler,
        mnnTtsHandler: MNNTTSHandler,
    ): KtorServer {
        return KtorServer(mnnHandler, mnnTtsHandler)
    }
}