package iridesdesign.app.handsoff.budgeDetector

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import iridesdesign.app.handsoff.R

object BudgeDetectorNotificationManager {
  internal const val NOTIFICATION_CHANNEL_ID = "414";
  internal const val NOTIFICATION_ID = 534;
  internal const val BUDGE_DETECTOR_SERVICE_STOPPER_RECEIVER_INTENT_REQUEST_CODE = 534;

  @RequiresApi(Build.VERSION_CODES.O)
  fun createNotificationChannel(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val notificationChannel = NotificationChannel(
      NOTIFICATION_CHANNEL_ID,
      context.getString(R.string.budge_detection_notifications),
      NotificationManager.IMPORTANCE_DEFAULT
    )

    notificationManager.createNotificationChannel(notificationChannel)
  }
}