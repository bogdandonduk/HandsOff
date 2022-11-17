package iridesdesign.app.handsoff

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import iridesdesign.app.handsoff.budgeDetector.BudgeDetectorService
import iridesdesign.app.handsoff.core.extension.getConfiguredResources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue

class HomeActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
      ) {
        val isBudgeDetectorRunning by BudgeDetectorService.lvdIsRunning.observeAsState()

        val localContext = LocalContext.current

        var hasNotificationPermission by remember {
          val value =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
              ContextCompat.checkSelfPermission(localContext, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            else true

          mutableStateOf(value)
        }

        val permissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { isGranted ->
            hasNotificationPermission = isGranted
          }
        )

        Text(
          text = getConfiguredResources().getString(
            if (isBudgeDetectorRunning!!) {
              R.string.stop_budge_detection
            }
            else
              R.string.start_budge_detection
          ),
          fontSize = 22.sp,
          modifier = Modifier
            .clickable {
              if (isBudgeDetectorRunning!!)
                BudgeDetectorService.stop()
              else {
                if (!hasNotificationPermission)
                  permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                else
                  BudgeDetectorService.start(this@HomeActivity)
              }
            }
            .padding(20.dp)
        )
      }
    }
  }
}