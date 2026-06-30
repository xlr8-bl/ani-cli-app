package com.xlr8.app

import android.app.Application
import com.xlr8.app.di.ServiceLocator

/**
 * XLR8 application entry point. No analytics, no crash SDKs, no trackers —
 * the device is the account and all state lives on-device.
 */
class XLR8Application : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
