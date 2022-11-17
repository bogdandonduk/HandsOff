package iridesdesign.app.handsoff.core.extension

import android.content.Context
import android.content.res.Configuration

fun Context.getConfiguredResources() =
  createConfigurationContext(
    Configuration(resources.configuration).apply {

    }
  ).resources