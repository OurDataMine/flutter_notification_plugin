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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

private const val TAG = "NotificationActivity"
const val EXTRA_ACTION = "path_to_photo"

class NotificationActivity : AppCompatActivity() {
    private fun getIntentToOpenMainActivity(uri: String): Intent? {
        val packageName: String = packageName
        return packageManager
            .getLaunchIntentForPackage(packageName)
            ?.setAction(Intent.ACTION_RUN)
            ?.putExtra(EXTRA_ACTION, uri)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var pictureSelected = false
        val photoFile: File = File.createTempFile("tmp", null, cacheDir)
        val camUri = FileProvider.getUriForFile(
            this, "com.ourdatamine.fileProvider",
            photoFile
        )
        val camPath : String = photoFile.absolutePath

        var buttonNum = intent.getIntExtra("Source", -1)
        Log.d(TAG, "Creating Photo Activity from button $buttonNum")

        setContentView(R.layout.activity_annotate_picture)

        val imageView : ImageView = findViewById(R.id.imageView)
        imageView.setImageResource(R.drawable.ic_launcher_background)

        val button : Button = findViewById(R.id.buttonLoadPicture)
        button.setOnClickListener {
            startActivity(getIntentToOpenMainActivity(camPath))
            // This occurs long before the activity is visible.  Probably not very useful.
            // Also not sure how to make the activity visible from a manually started engine.
//            LockScreenNotificationPlugin.editPicture(this, camPath)
            finish()
        }

        val textview = findViewById<TextView>(R.id.textView3)
        textview.text = "Arrived from $buttonNum"

        val registerTakePicture: ActivityResultLauncher<Uri?> =
            registerForActivityResult(CustomTakePicture())
            { success: Boolean ->
                if (success) {
                    imageView.setImageURI(camUri)
                    pictureSelected = true
                    Log.d(TAG, "Picture has been set to $camUri ($camPath)")
                    LockScreenNotificationPlugin.recordPicture(this, camPath)
                }
                else
                {
                    finish()
                }
            }
        buttonNum = 1
        if (buttonNum == 10 && !pictureSelected) {
            Log.d(TAG, "Picture has been NOT been set, launching camera")
            registerTakePicture.launch(camUri)
        }
        else {
            Log.d(TAG, "Camera not working. URI set to $camUri ($camPath)")
            LockScreenNotificationPlugin.recordPicture(this, camPath)
        }
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