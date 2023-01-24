package com.ourdatamine.lock_screen_notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log



private const val TAG = "MyBroadcastReceiver"

class MyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        StringBuilder().apply {
            append("Action: ${intent.action}\n")
            append("URI: ${intent.toUri(Intent.URI_INTENT_SCHEME)}\n")
            toString().also { log ->
                Log.d(TAG, log)
            }
        }

        val requestCode: Int = intent.extras?.getInt("android.intent.extra.NOTIFICATION_ID", -1)!!
        val status = context.getString(LockScreenNotificationPlugin.smile_emojis[requestCode - 1])
        LockScreenNotificationPlugin.recordFeelings(context, requestCode, status)
    }
}