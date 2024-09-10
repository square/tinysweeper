package com.squareup.tinysweeper

import android.content.Context
import android.content.Context.MODE_PRIVATE
import java.util.UUID

/*
 * Unique random ID generated and stored within SharedPreferences.
 */
class InstallationIdProvider(private val context: Context) : IdProvider {
  /**
   * Generates and stores an ID unique to this installation of this app.
   */

  private val sharedPreferences
    get() = context.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)

  val installationId: String by lazy {
    if (PREFERENCES_NAME !in sharedPreferences) {
      initializeId()
    }
    return@lazy sharedPreferences.getString(PREFERENCES_NAME, null)!!
  }

  private fun initializeId() {
    val id = UUID.randomUUID()
    sharedPreferences.edit()
      .putString(PREFERENCES_NAME, id.toString())
      .apply()
  }

  override fun getLabel(): String {
    return "DeviceID"
  }

  override fun getId(): String {
    return installationId
  }

  public companion object {
    const val PREFERENCES_NAME = "installation_id"
  }
}
