package com.ourdatamine.lock_screen_notification

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ourdatamine.lock_screen_notification.databinding.ActivityCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Camera : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var binding: ActivityCameraBinding
    private var location : Location? = null
    private var cameraController: LifecycleCameraController? = null


    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "Starting Camera onCreate")
        binding = ActivityCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        getLocation()

        // hide the action bar
        supportActionBar?.hide()

        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_CAMERA_PERMISSIONS)
        }

        // set on click listener for the button of capture photo
        // it calls a method which is implemented below
        findViewById<View>(R.id.camera_layout).setOnClickListener {
            takePhoto()
        }
        findViewById<View>(R.id.viewFinder).setOnClickListener {
            takePhoto()
        }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }



    private fun isGranted(perm : String): Boolean {
        return ActivityCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(request: Boolean = true) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Log.e(LTAG, "Checking location permissions")
        if (isGranted(Manifest.permission.ACCESS_FINE_LOCATION) && isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Log.e(LTAG, "Location permission granted...")

            // Get location fast, then try to get a new one.
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (this.location == null) {
                    this.location = location
                }
            }
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d(LTAG, "Button appear!")
                    }
                    val msg = "Location = $location"
                    Log.d(LTAG, msg)
                    // Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    this.location = location
                }
        }
        else if (request) {
            Log.e(LTAG, "location denied! Requesting...")
            ActivityCompat.requestPermissions(
                this,
                LOCATION_PERMISSIONS,
                REQUEST_CODE_LOCATION_PERMISSIONS
            )
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the
        // modifiable image capture use case
        val cameraController = cameraController ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val metadata = ImageCapture.Metadata()
        metadata.location = location

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.
        Builder(photoFile)
            .setMetadata(metadata)
            .build()

        // Set up image capture listener,
        // which is triggered after photo has
        // been taken
        cameraController.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)

                    // set the saved uri to the image view
                    findViewById<ImageView>(R.id.iv_capture).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.iv_capture).setImageURI(savedUri)

                    val msg = "Photo capture succeeded: $savedUri"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    Log.d(TAG, msg)
                    vibrate()
                }
            })
    }

    fun vibrate(){
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @SuppressLint("WrongConstant")
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE) )
        }else{
            @Suppress("DEPRECATION")
            vib.vibrate(300)
        }
    }

    private fun startCamera() {

        cameraController = LifecycleCameraController(this)
        val cameraController = cameraController ?: return

        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraController.imageCaptureFlashMode = ImageCapture.FLASH_MODE_OFF
        cameraController.imageCaptureTargetSize = CameraController.OutputSize(
            android.util.Size(1024,1024))
        cameraController.isPinchToZoomEnabled = true


        val preview  : PreviewView = binding.viewFinder
        preview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        preview.controller = cameraController

        try {
            cameraController.bindToLifecycle(this)
        } catch (exc: IllegalStateException) {
            Log.e(TAG, "Use case binding failed", exc)
        }

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // creates a folder inside internal storage
    private fun getOutputDirectory(): File {
        return File(filesDir, "quickpics").apply{ mkdirs() }
    }

    // checks the permissions
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSIONS) {
            // If all permissions granted , then start Camera
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // If permissions are not granted,
                // present a toast to notify the user that
                // the permissions were not granted.
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS) {
            if (grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                getLocation(false)
            } else {
                val msg = "Location permission denied: $grantResults"
                Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                Log.e(LTAG, msg)
            }
        }
    }


    companion object {
        private const val TAG = "CameraXGFG"
        private const val LTAG = "Location"
        private const val FILENAME_FORMAT = "yyyy-MM-dd'T'HHmmss.SSSZ"
        private const val REQUEST_CODE_CAMERA_PERMISSIONS = 20
        private const val REQUEST_CODE_LOCATION_PERMISSIONS = 30
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
