package com.example.camera_1.ui

import android.content.Context
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(
    camera: Camera?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PreviewSurfaceView(context, camera)
        },
        modifier = modifier,
        update = { view ->
            (view as PreviewSurfaceView).setCamera(camera)
        }
    )
}

private class PreviewSurfaceView(context: Context, private var mCamera: Camera?) : 
    SurfaceView(context), SurfaceHolder.Callback {

    private var mAspectRatio = 4.0 / 3.0

    init {
        holder.addCallback(this)
        @Suppress("DEPRECATION")
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        // In portrait, the aspect ratio is height / width
        if (width < height) {
            if (width * mAspectRatio < height) {
                setMeasuredDimension(width, (width * mAspectRatio).toInt())
            } else {
                setMeasuredDimension((height / mAspectRatio).toInt(), height)
            }
        } else {
            if (height * mAspectRatio < width) {
                setMeasuredDimension((height * mAspectRatio).toInt(), height)
            } else {
                setMeasuredDimension(width, (width / mAspectRatio).toInt())
            }
        }
    }

    fun setCamera(camera: Camera?) {
        if (mCamera == camera) return
        
        mCamera = camera
        if (mCamera != null) {
            try {
                val params = mCamera?.parameters
                params?.let {
                    val size = it.previewSize
                    mAspectRatio = if (size.width > size.height) {
                        size.width.toDouble() / size.height
                    } else {
                        size.height.toDouble() / size.width
                    }
                }
                requestLayout()
                
                mCamera?.setPreviewDisplay(holder)
                mCamera?.startPreview()
                mCamera?.setDisplayOrientation(90)
            } catch (e: Exception) {
                Toast.makeText(context, "Error setting camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            mCamera?.apply {
                val params = parameters
                params?.let {
                    val size = it.previewSize
                    mAspectRatio = if (size.width > size.height) {
                        size.width.toDouble() / size.height
                    } else {
                        size.height.toDouble() / size.width
                    }
                }
                requestLayout()
                
                setPreviewDisplay(holder)
                startPreview()
                setDisplayOrientation(90)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to Open Camera $e", Toast.LENGTH_SHORT).show()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Camera release is handled in Activity onPause or by the host
    }
}
