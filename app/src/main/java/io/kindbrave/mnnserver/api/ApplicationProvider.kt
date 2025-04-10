// Created by ruoyi.sjd on 2024/12/18.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
package io.kindbrave.mnnserver.api

import android.app.Application

object ApplicationProvider {
    private lateinit var application: Application
    fun set(application: Application) {
        ApplicationProvider.application = application
    }

    fun get(): Application {
        return application
    }
}
