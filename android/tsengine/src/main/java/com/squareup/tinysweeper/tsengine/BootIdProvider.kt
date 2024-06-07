package com.squareup.tinysweeper.tsengine

import android.content.Context
import android.provider.Settings
import java.io.File

const val UNKNOWN_BOOTID = "unknown-bootid"

class RealBootIdProvider(private val ctx: Context) : IdProvider {
  fun getBootCount(): Int {
    return Settings.Global.getInt(
      ctx.contentResolver,
      Settings.Global.BOOT_COUNT
    )
  }

  fun getBootId(): String {
    val bootIdFile = File("/proc/sys/kernel/random/boot_id")
    if (bootIdFile.exists()) {
      return bootIdFile.readText().trim()
    }
    throw NoSuchFileException(bootIdFile)
  }

  override fun getId(): String {
    try {
      return getBootId()
    } catch (e: NoSuchFileException) {
      return getBootCount().toString()
    } catch (e: Exception) {
      return UNKNOWN_BOOTID
    }
  }

  override fun getLabel(): String {
    return "BootID"
  }
}
