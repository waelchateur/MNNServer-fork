package io.kindbrave.mnn.webserver

import android.app.Application
import android.content.Context

object WebServer {
    private lateinit var application: Application

    fun init(context: Context) {
        application = context.applicationContext as Application
    }

    fun getApplication(): Application = application
}