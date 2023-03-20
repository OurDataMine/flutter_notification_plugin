package com.ourdatamine.lock_screen_notification

import io.flutter.embedding.android.FlutterActivity

class NotificationFlutterActivity : FlutterActivity() {
    override fun getDartEntrypointFunctionName(): String {
        return "mySpecialEntryPoint"
    }
}
