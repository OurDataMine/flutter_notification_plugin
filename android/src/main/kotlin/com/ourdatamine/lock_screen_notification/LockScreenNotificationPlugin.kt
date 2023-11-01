package com.ourdatamine.lock_screen_notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result

/** LockScreenNotificationPlugin */
private const val TAG = "FlutterNotification"

class LockScreenNotificationPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var _channel: MethodChannel
    private var _appContext: Context? = null
    private var _engine: FlutterPlugin.FlutterPluginBinding? = null

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (_appContext == null) {
            result.error("No Context", "Context is missing", "")
            return
        }
        when (call.method) {
            "createNotification" -> {
                Log.d(TAG, "args = ${call.arguments}")
                _appContext?.also {
                    val argument: String = call.arguments as String
                    createNotification(it, argument)
                    val ret = if (areNotificationsEnabled(it)) 1 else -1
                    result.success(ret)
                }
            }
            "cancelNotification" -> {
                _appContext?.let {
                    val nm: NotificationManager =
                        it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(NOTIFICATION_ID)
                    result.success(1)
                }
            }
            "launchApp" -> {
                _appContext?.also {
                    it.startActivity(it.packageManager.getLaunchIntentForPackage(it.packageName))
                }
            }
            "takePicture" -> {
                _appContext?.also {
                    val intent1 = Intent(it, Camera::class.java)
                    intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    it.startActivity(intent1)
                }
            }
            else -> {
                Log.w(TAG, "Couldn't find ${call.method}")
                result.notImplemented()
            }
        }
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(TAG, "Attaching to Engine: $flutterPluginBinding")
        _engine = flutterPluginBinding
        _appContext = flutterPluginBinding.applicationContext
        _channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_ID)
        _channel.setMethodCallHandler(this)
        FlutterEngineCache.getInstance()
            .put("notification_engine", flutterPluginBinding.flutterEngine)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(TAG, "Killing Engine: $binding")
        FlutterEngineCache.getInstance().remove("notification_engine")
        _channel.setMethodCallHandler(null)
        _appContext = null
        _engine = null
    }

    init {
        instance = this
    }

    companion object {
        var instance: LockScreenNotificationPlugin? = null

        private var CHANNEL_ID = "com.ourdatamine.heathtracking/notification_click"

        private val smile_buttons =
            arrayOf(R.id.smile1, R.id.smile2, R.id.smile3, R.id.smile4, R.id.smile5)
        val smile_emojis = arrayOf(
            R.string.Smile1,
            R.string.Smile2,
            R.string.Smile3,
            R.string.Smile4,
            R.string.Smile5
        )

        private const val NOTIFICATION_ID = 101

        private fun startEngine(context: Context, args: List<String> = listOf()) {
            if (FlutterEngineCache.getInstance()
                    .contains("notifcation_engine") || instance?._engine != null
            ) {
                Log.d(TAG, "Engine is already initialised @ ${instance?._engine}")
                return
            }
            Log.i(TAG, "Engine is null, starting engine")

            val injector = FlutterInjector.instance()
            val loader = injector.flutterLoader()
            loader.startInitialization(context)
            loader.ensureInitializationComplete(context, null)
            val engine = FlutterEngine(context)

            Log.d(TAG, "Engine up: $engine")

            val dartExecutor = engine.dartExecutor
            Log.d(TAG, "Exec up: $dartExecutor")

            val entryPoint = DartExecutor.DartEntrypoint.createDefault()
            dartExecutor.executeDartEntrypoint(entryPoint, args)
        }

        fun recordFeelings(context: Context, code: Int) {
            val emojiId = smile_emojis.getOrNull(code-1) ?: return
            val emoji = context.getString(emojiId)


            startEngine(context, listOf("recordFeelingEvent=$emoji"))
            instance?._channel?.invokeMethod("feelings_event", listOf(code, emoji))
            Log.d(TAG, "Attempted dart method feelings from Native")
        }

        fun recordPictures(context: Context) {
            startEngine(context, listOf("pictures"))
            instance?._channel?.invokeMethod("picture_event", listOf<String>())
            Log.d(TAG, "Attempted dart method recordPictures from Native")
        }

        fun editPicture(context: Context, filename: String) {
            if (instance?._engine != null) {
                instance?._channel?.invokeMethod("edit_picture", filename)
                Log.d(TAG, "Attempted dart method edit_picture to running instance")
            } else {
                context.also {
                    val intent = it.packageManager.getLaunchIntentForPackage(it.packageName)
                    intent?.putExtra("filename", filename)
                    it.startActivity(intent)
                }
                Log.d(TAG, "Attempted dart method edit_picture via Intent to start dart UI")
            }
        }

        private fun createPI(context: Context): PendingIntent {
            val requestCode = 10 //Camera
            val intent = Intent(context, Camera::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("Source", requestCode)
            }
            return PendingIntent.getActivity(
                context, requestCode,
                intent, PendingIntent.FLAG_IMMUTABLE
            )
        }

        @SuppressLint("WrongConstant")
        private fun createPIBroadcast(context: Context, requestCode: Int): PendingIntent {

            val snoozeIntent = Intent(context, MyBroadcastReceiver::class.java).apply {
                action = "RECORD_FEELINGS"
                putExtra("Feeling", requestCode)
            }
            return PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    snoozeIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
        }

        fun createNotification(context: Context, update_text: String = "") {
            createNotificationChannel(context)
            val notificationView =
                RemoteViews(
                    context.packageName,
                    R.layout.notification_layout
                )

            notificationView.setCharSequence(R.id.notifcationText, "setText", update_text)

            val cameraIntent = createPI(context)

//            for (ii in 1..5)
//                notificationView.setOnClickPendingIntent(
//                    smile_buttons[ii - 1],
//                    createPIBroadcast(context, ii)
//                )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Record Health Event")
                .setContentText(update_text)
                .setContentInfo("Content Info")
                .setAutoCancel(false)
                .setLocalOnly(true)
                .setSilent(true)
                .setOngoing(true)
                .setShowWhen(false)
                .setColor(ContextCompat.getColor(context, R.color.leafy_green))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(cameraIntent)
//            .setCustomContentView(notificationView)
//                .setCustomBigContentView(notificationView)

            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
            Log.d("Notification", "Create Notification with text: $update_text")
        }

        fun areNotificationsEnabled(context: Context): Boolean {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        private fun createNotificationChannel(context: Context) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Quick Record"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = "Quickly record a reaction or photo"
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)
                }

                val notificationManager = context.getSystemService(
                    NotificationManager::class.java
                )
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
