package iridesdesign.app.handsoff

import android.app.Application
import android.os.Build
import iridesdesign.app.handsoff.budgeDetector.BudgeDetectorNotificationManager

class HandsOff : Application() {

  override fun onCreate() {
    super.onCreate()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      BudgeDetectorNotificationManager.createNotificationChannel(this)
  }
}
