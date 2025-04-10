package io.kindbrave.mnnserver

import android.app.Application
import io.kindbrave.mnnserver.api.ApplicationProvider

class MNNServerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ApplicationProvider.set(this)
    }
}