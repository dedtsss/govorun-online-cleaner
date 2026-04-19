package com.govorun.lite

import android.app.Application
import com.google.android.material.color.DynamicColors

class LiteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
