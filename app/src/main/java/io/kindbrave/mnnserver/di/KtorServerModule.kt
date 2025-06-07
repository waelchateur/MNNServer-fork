package io.kindbrave.mnnserver.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.kindbrave.mnn.webserver.webserver.KtorServer
import io.kindbrave.mnn.webserver.webserver.MNNHandler
import io.kindbrave.mnnserver.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KtorServerModule {

    @Provides
    @Singleton
    fun provideKtorServer(
        mnnHandler: MNNHandler
    ): KtorServer {
        return KtorServer(mnnHandler)
    }
}