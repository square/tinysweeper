package com.tinysweeper.tsengine.detections

import android.content.Context
import android.provider.Settings
import com.squareup.protos.tinysweeper.android.Tinysweeper.Detection
import com.tinysweeper.tsengine.Detector
import org.threeten.bp.Clock

class DeveloperModeDetection(
  private var appContext: Context,
  clock: Clock,
) : Detector(clock = clock) {
  init {
    state.setType(Detection.Type.DEVELOPER_MODE_ENABLED)
  }

  override suspend fun evaluate(): Boolean {
    return Settings.Global.getInt(
      this.appContext.contentResolver,
      Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
      0,
    ) != 0
  }
}
