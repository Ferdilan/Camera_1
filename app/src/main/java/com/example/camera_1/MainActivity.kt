package com.example.camera_1

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.camera_1.ui.CameraControls
import com.example.camera_1.ui.CameraPreview
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private var mCamera: Camera? = null
    private var mPicture: Camera.PictureCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var cameraState by remember { mutableStateOf<Camera?>(null) }
            var cameraId by remember { mutableIntStateOf(Camera.CameraInfo.CAMERA_FACING_BACK) }
            var flashMode by remember { mutableStateOf("off") }
            var zoomLevel by remember { mutableIntStateOf(0) }
            var currentPreviewState by remember { mutableStateOf("PREVIEW") }

            // Initialize Picture Callback
            LaunchedEffect(Unit) {
                mPicture = Camera.PictureCallback { data, _ ->
                    try {
                        // Fix orientation and save
                        saveImageToGallery(data, cameraId)
                        
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Gambar tersimpan di Galeri", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Gagal simpan: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // Camera initialization and switching
            LaunchedEffect(cameraId) {
                if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cekIzinDitolak())) {
                    releaseCamera()
                    mCamera = getCameraInstance(cameraId)
                    cameraState = mCamera
                    applyCameraSettings(mCamera, flashMode, zoomLevel)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, 1)
                }
            }
            
            // Settings update
            LaunchedEffect(flashMode, zoomLevel) {
                applyCameraSettings(mCamera, flashMode, zoomLevel)
            }

            DisposableEffect(Unit) {
                onDispose {
                    releaseCamera()
                }
            }

            Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Black) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CameraPreview(
                        camera = cameraState,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    CameraControls(
                        flashMode = flashMode,
                        onShutterClick = {
                            when (currentPreviewState) {
                                "BUSY" -> {
                                    try {
                                        mCamera?.startPreview()
                                        currentPreviewState = "PREVIEW"
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Gagal preview: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                "PREVIEW" -> {
                                    try {
                                        mCamera?.takePicture(null, null, mPicture)
                                        currentPreviewState = "BUSY"
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Gagal potret: ${e.message}", Toast.LENGTH_SHORT).show()
                                        try { mCamera?.startPreview() } catch (ex: Exception) {}
                                    }
                                }
                            }
                        },
                        onSwitchClick = {
                            cameraId = if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                Camera.CameraInfo.CAMERA_FACING_FRONT
                            } else {
                                Camera.CameraInfo.CAMERA_FACING_BACK
                            }
                        },
                        onFlashClick = {
                            flashMode = when (flashMode) {
                                "off" -> "on"
                                "on" -> "auto"
                                else -> "off"
                            }
                        },
                        onZoomIn = { if (zoomLevel < 100) zoomLevel += 5 },
                        onZoomOut = { if (zoomLevel > 0) zoomLevel -= 5 },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    private fun applyCameraSettings(camera: Camera?, flash: String, zoom: Int) {
        camera?.let {
            try {
                val params = it.parameters
                
                // Set Preview and Picture Size to prevent stretching
                val supportedPreviewSizes = params.supportedPreviewSizes
                val supportedPictureSizes = params.supportedPictureSizes
                
                if (supportedPreviewSizes != null && supportedPictureSizes != null) {
                    // Try to find a size that matches a 4:3 aspect ratio as it's standard
                    val targetRatio = 4.0 / 3.0
                    val previewSize = supportedPreviewSizes.minByOrNull { size ->
                        val ratio = if (size.width > size.height) size.width.toDouble() / size.height else size.height.toDouble() / size.width
                        Math.abs(ratio - targetRatio)
                    }
                    
                    val pictureSize = supportedPictureSizes.filter { size ->
                        val ratio = if (size.width > size.height) size.width.toDouble() / size.height else size.height.toDouble() / size.width
                        Math.abs(ratio - targetRatio) < 0.1
                    }.maxByOrNull { it.width * it.height } ?: supportedPictureSizes.maxByOrNull { it.width * it.height }

                    previewSize?.let { size -> params.setPreviewSize(size.width, size.height) }
                    pictureSize?.let { size -> params.setPictureSize(size.width, size.height) }
                }

                if (params.supportedFlashModes != null) {
                    params.flashMode = when(flash) {
                        "on" -> Camera.Parameters.FLASH_MODE_ON
                        "auto" -> Camera.Parameters.FLASH_MODE_AUTO
                        else -> Camera.Parameters.FLASH_MODE_OFF
                    }
                }
                if (params.isZoomSupported) {
                    params.zoom = (zoom * params.maxZoom / 100).coerceIn(0, params.maxZoom)
                }
                if (params.supportedFocusModes != null) {
                    if (params.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                    }
                }
                it.parameters = params
            } catch (e: Exception) {}
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            recreate()
        }
    }

    private fun cekIzinDitolak(): Boolean {
        return PERMISSIONS.any { checkSelfPermission(it) == PackageManager.PERMISSION_DENIED }
    }

    private fun getCameraInstance(id: Int): Camera? {
        return try { Camera.open(id) } catch (e: Exception) { null }
    }

    private fun releaseCamera() {
        mCamera?.apply {
            stopPreview()
            release()
        }
        mCamera = null
    }

    private fun saveImageToGallery(data: ByteArray, id: Int) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val name = "IMG_$timestamp.jpg"
        
        // Decode to bitmap to fix orientation if needed
        var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        
        // Fix rotation
        val matrix = Matrix()
        if (id == Camera.CameraInfo.CAMERA_FACING_BACK) {
            matrix.postRotate(90f)
        } else {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f) // Mirror front camera
        }
        
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Camera_1")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let {
            val outputStream: OutputStream? = resolver.openOutputStream(it)
            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
            
            // Legacy broadcast for older apps
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, it))
        }
        
        bitmap.recycle()
    }

    companion object {
        private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
