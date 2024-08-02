package com.squareup.tinysweeper.tsengine.detections

import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.AvailabilityCallback
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.util.Log
import com.squareup.tinysweeper.tsengine.Detector
import org.threeten.bp.Clock
import java.util.concurrent.Executor

class CameraDetection(
  private var appContext: Context,
  clock: Clock,
) : Detector(clock = clock) {
  private val executor: Executor
  init {
    // state.setType(Detection.Type.DEVELOPER_MODE_ENABLED)
    executor = DirectExecutor()
  }

  override suspend fun evaluate(): Boolean {
    val manager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      manager.registerAvailabilityCallback(executor, object : AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
          super.onCameraAvailable(cameraId)
          Log.i("cameraCheck", "Camera is available")
        }

        override fun onCameraUnavailable(cameraId: String) {
          super.onCameraUnavailable(cameraId)
          Log.i("cameraCheck", "Camera is in use")
        }
      })
    }
    return Settings.Global.getInt(
      this.appContext.contentResolver,
      Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
      0,
    ) != 0
  }

  class DirectExecutor : Executor {
    override fun execute(r: Runnable) {
      r.run()
    }
  }
}
