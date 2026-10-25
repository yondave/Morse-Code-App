package com.example.core.hardware

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log

class TorchController(private val context: Context) {

    companion object {
        private const val TAG = "TorchController"
    }

    private val cameraManager: CameraManager? = try {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    } catch (e: Exception) {
        null
    }

    private var cameraId: String? = null
    var hasFlash: Boolean = false
        private set

    var isTorchOn: Boolean = false
        private set

    init {
        detectFlashlight()
    }

    private fun detectFlashlight() {
        val manager = cameraManager ?: return
        try {
            for (id in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(id)
                val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (flashAvailable && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    hasFlash = true
                    break
                }
            }
            // Fallback to any camera with flash
            if (cameraId == null) {
                for (id in manager.cameraIdList) {
                    val characteristics = manager.getCameraCharacteristics(id)
                    val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    if (flashAvailable) {
                        cameraId = id
                        hasFlash = true
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Flash detection error: ${e.message}")
        }
    }

    fun setTorch(enabled: Boolean) {
        val manager = cameraManager
        val id = cameraId
        if (manager == null || id == null || !hasFlash) {
            isTorchOn = enabled
            return
        }

        try {
            manager.setTorchMode(id, enabled)
            isTorchOn = enabled
        } catch (e: CameraAccessException) {
            Log.w(TAG, "Failed to toggle torch: ${e.message}")
            isTorchOn = false
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error toggling torch: ${e.message}")
            isTorchOn = false
        }
    }

    fun turnOff() {
        if (isTorchOn) {
            setTorch(false)
        }
    }
}
