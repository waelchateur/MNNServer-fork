package io.kindbrave.mnnserver

import android.app.Application
import com.alibaba.mls.api.ApplicationProvider
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MNNServerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ApplicationProvider.set(this)
    }
}