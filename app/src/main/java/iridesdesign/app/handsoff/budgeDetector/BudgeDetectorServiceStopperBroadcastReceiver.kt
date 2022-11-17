package iridesdesign.app.handsoff.budgeDetector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BudgeDetectorServiceStopperBroadcastReceiver: BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    BudgeDetectorService.stop()
  }
}