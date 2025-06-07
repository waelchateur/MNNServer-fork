package io.kindbrave.mnn.server

import android.app.Application
import android.content.Context
import com.alibaba.mls.api.ApplicationProvider

object MNN {
    private lateinit var application: Application

    fun init(context: Context) {
        application = context.applicationContext as Application
        ApplicationProvider.set(application)
    }

    fun getApplication(): Application = application
}