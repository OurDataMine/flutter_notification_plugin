package com.ourdatamine.lock_screen_notification

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.android.FlutterFragment
import io.flutter.embedding.android.FlutterFragmentActivity
import java.io.File

private const val TAG = "NotificationActivity"
const val EXTRA_ACTION = "path_to_photo"

class NotificationActivity : AppCompatActivity() {
    companion object {
        // Define a tag String to represent the FlutterFragment within this
        // Activity's FragmentManager. This value can be whatever you'd like.
        private const val TAG_FLUTTER_FRAGMENT = "flutter_fragment"
    }

    private fun getIntentToOpenMainActivity(uri: String): Intent? {
        val packageName: String = packageName
        return packageManager
            .getLaunchIntentForPackage(packageName)
            ?.setAction(Intent.ACTION_RUN)
            ?.putExtra(EXTRA_ACTION, uri)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    fun setupGUI() {
        setContentView(R.layout.activity_annotate_picture)

        val imageView : ImageView = findViewById(R.id.imageView)
        imageView.setImageResource(R.drawable.ic_launcher_background)

        val button : Button = findViewById(R.id.buttonLoadPicture)
        button.setOnClickListener {
//            startActivity(getIntentToOpenMainActivity(camPath))
//            finish()

            // This occurs long before the activity is visible.  Probably not very useful.
            // Also not sure how to make the activity visible from a manually started engine.
            val i1 = packageManager.getLaunchIntentForPackage(packageName)
            i1?.putExtra("cached_engine_id", "notification_engine")

            Log.w(TAG, i1.toString())
            val intent = FlutterActivity
                .withCachedEngine("notifcation_engine")
                .build(this)
//            Log.w(TAG,intent.toString())
            intent
                //.putExtra(EXTRA_ACTION, camPath)
                .setClassName(this, ".MainActivity")
                .setAction(Intent.ACTION_RUN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage("com.ourdatamine.picture_storage")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            Log.w(TAG,intent.toString())
            Log.w(TAG, intent.extras.toString())
            //startActivity(intent)
            intent.extras?.let { it1 -> i1?.putExtras(it1) }
            startActivity(i1)
//            LockScreenNotificationPlugin.editPicture(this, camPath)
            finish()
        }

        var buttonNum = intent.getIntExtra("Source", -1)
        val textview = findViewById<TextView>(R.id.textView3)
        textview.text = "Arrived from $buttonNum"

    }

    fun setupFragment(camPath: String){
        val fragmentManager = supportFragmentManager
        setContentView(R.layout.fragment_holder)
        flutterFragment = fragmentManager
            .findFragmentByTag(TAG_FLUTTER_FRAGMENT) as FlutterFragment?
        if (flutterFragment == null) {
            var newFlutterFragment  : FlutterFragment = FlutterFragment
//                .withCachedEngine("notification_engine")
                .withNewEngine()
                .dartEntrypoint("mySpecialEntryPoint")
                .dartEntrypointArgs(listOf(camPath))
                .build()
            flutterFragment = newFlutterFragment
            fragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container,
                    newFlutterFragment,
                    TAG_FLUTTER_FRAGMENT
                )
                .commit()
        }
    }

    private var flutterFragment: FlutterFragment? = null

    fun pictureSuccess(camUri : Uri, camPath: String) {
        // val imageView : ImageView = findViewById(R.id.imageView)
        // imageView.setImageURI(camUri)
        pictureSelected = true
        Log.d(TAG, "Picture has been set to $camUri ($camPath)")
        LockScreenNotificationPlugin.recordPicture(this, camPath)
        setupFragment(camPath)
    }

    private var pictureSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoFile: File = File.createTempFile("tmp", null, cacheDir)
        val camUri = FileProvider.getUriForFile(
            this, "com.ourdatamine.fileProvider",
            photoFile
        )
        val camPath : String = photoFile.absolutePath

//        setupFragment()

        val registerTakePicture: ActivityResultLauncher<Uri?> =
            registerForActivityResult(CustomTakePicture())
            { success: Boolean ->
                if (success) {
                    pictureSuccess(camUri, camPath)
                } else {
                    finish()
                }
            }

        var buttonNum = intent.getIntExtra("Source", -1)
        Log.d(TAG, "Creating Photo Activity from button $buttonNum")

//        buttonNum = 1
        LockScreenNotificationPlugin.startEngine(this)
        if (buttonNum == 10 && !pictureSelected) {
            Log.d(TAG, "Picture has been NOT been set, launching camera")
            registerTakePicture.launch(camUri)
        }
        else {
            Log.d(TAG, "Camera not working. URI set to $camUri ($camPath)")
            pictureSuccess(camUri, camPath)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        flutterFragment?.onPostResume()
    }

    override fun onNewIntent(@NonNull intent: Intent) {
        flutterFragment?.onNewIntent(intent)
    }

    override fun onBackPressed() {
        flutterFragment?.onBackPressed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        flutterFragment?.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )
    }

    override fun onUserLeaveHint() {
        flutterFragment?.onUserLeaveHint()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        flutterFragment?.onTrimMemory(level)
    }
    
    open class CustomTakePicture : ActivityResultContracts.TakePicture() {
        @CallSuper
        override fun createIntent(context: Context, input: Uri): Intent {
            val action: String = MediaStore.ACTION_IMAGE_CAPTURE_SECURE
            val intent : Intent = Intent(action)
                .putExtra(MediaStore.EXTRA_OUTPUT, input)
//            intent.clipData = ClipData.newRawUri("picture_path", input)
            return intent
        }
    }

}