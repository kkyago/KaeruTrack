package com.kaeru.app.data.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaeru.app.tracking.TrackingViewModel

class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_MY_PACKAGE_REPLACED || action == Intent.ACTION_BOOT_COMPLETED) {
            TrackingViewModel.scheduleTrackingWorker(context)
            TrackingViewModel.scheduleUpdateWorker(context)
        }
    }
}