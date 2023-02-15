package com.ourdatamine.lock_screen_notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.annotation.NonNull
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
    private lateinit var channel: MethodChannel
    private var _appContext: Context? = null
    private var _engine: FlutterPlugin.FlutterPluginBinding? = null

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
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
                    result.success(1)
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
            else -> {
                Log.w(TAG, "Couldn't find ${call.method}")
                result.notImplemented()
            }
        }
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(TAG, "Attaching to Engine: $flutterPluginBinding")
        _engine = flutterPluginBinding
        _appContext = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_ID)
        channel.setMethodCallHandler(this)
        FlutterEngineCache.getInstance()
            .put("notification_engine", flutterPluginBinding.flutterEngine)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(TAG, "Killing Engine: $binding")
        FlutterEngineCache.getInstance().remove("notification_engine")
        channel.setMethodCallHandler(null)
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

        fun startEngine(context: Context, args: List<String> = listOf()) {
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
//        val entryPoint = DartEntrypoint(loader.findAppBundlePath(),"other_entrypoint")
            dartExecutor.executeDartEntrypoint(entryPoint, args)
        }

        fun recordFeelings(context: Context, code: Int, emoji: String) {
            startEngine(context, listOf("recordFeelingEvent=$emoji"))
            instance?.channel?.invokeMethod("feelings_event", listOf(code, emoji))
            Log.d(TAG, "Attempted dart method feelings from Native")
        }

        fun recordPicture(context: Context, uri: String) {
            startEngine(context, listOf("recordPictureEvent=$uri"))
            instance?.channel?.invokeMethod("picture_event", uri)
            Log.d(TAG, "Attempted dart method picture_event from Native")
        }

        fun editPicture(context: Context, uri: String) {
            startEngine(context, listOf("editPictureEvent=$uri"))
            instance?.channel?.invokeMethod("edit_picture", uri)
            Log.d(TAG, "Attempted dart method edit_picture from Native")
        }

        private fun createPI(context: Context): PendingIntent {
            val requestCode = 10 //Camera
            val intent = Intent(context, NotificationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("Source", requestCode)
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context, requestCode,
                intent, PendingIntent.FLAG_IMMUTABLE
            )
            return pendingIntent

        }

        private fun createPIBroadcast(context: Context, requestCode: Int): PendingIntent {

            val snoozeIntent = Intent(context, MyBroadcastReceiver::class.java).apply {
                action = "RECORD_FEELINGS"
                putExtra(Notification.EXTRA_NOTIFICATION_ID, requestCode)
            }
            val snoozePendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    snoozeIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            return snoozePendingIntent
        }

        fun createNotification(context: Context, update_text: String = "") {
            createNotificationChannel(context)
            val notificationView =
                RemoteViews(
                    "com.ourdatamine.picture_storage",
                    R.layout.notification_layout
                )

            notificationView.setCharSequence(R.id.notifcationText, "setText", update_text)

            val cameraIntent = createPI(context)

            for (ii in 1..5)
                notificationView.setOnClickPendingIntent(
                    smile_buttons[ii - 1],
                    createPIBroadcast(context, ii)
                )

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
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(cameraIntent)
//            .setCustomContentView(notificationView)
                .setCustomBigContentView(notificationView)

            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
            Log.d("Notification", "Create Notification with text: $update_text")
        }

        private fun createNotificationChannel(context: Context) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Quick Record"
                val importance = NotificationManager.IMPORTANCE_MAX
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
