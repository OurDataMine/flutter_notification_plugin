package com.ourdatamine.lock_screen_notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        StringBuilder().apply {
            append("Action: ${intent.action}\n")
            append("URI: ${intent.toUri(Intent.URI_INTENT_SCHEME)}\n")
            toString().also { log ->
                Log.d(TAG, log)
            }
        }

        val requestCode: Int? = intent.extras?.getInt("Feeling", -1)
        if(requestCode == null) {
            Log.d(TAG, "Unable to get extra from notification!: ${intent.extras}")
            return;
        }
        LockScreenNotificationPlugin.recordFeelings(context, requestCode)
    }

    companion object {
        private const val TAG = "MyBroadcastReceiver"
    }
}
