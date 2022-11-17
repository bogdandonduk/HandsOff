package iridesdesign.app.handsoff.budgeDetector

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.*
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import iridesdesign.app.handsoff.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BudgeDetectorService : Service() {

  private lateinit var mSensorManager: SensorManager
  private lateinit var mAccelerometer: Sensor
  private lateinit var mSensorEventListener: SensorEventListener

  var initialSideWardsTilt: Float? = null
  var initialForthBackWardsTilt: Float? = null

  val sensitivity = 0.03f

  private lateinit var sirenSoundMediaPlayer: MediaPlayer
  private lateinit var audioManager: AudioManager

  private var latestInitialVolumeLevel: Int? = null

  private var volumeForceControlRoutine: Job? = null

  private var maxMusicStreamVolumeLevel: Int = 0

  override fun onCreate() {
    super.onCreate()

    instance = this

    sirenSoundMediaPlayer = MediaPlayer.create(this, R.raw.sound_siren)
      .apply { isLooping = true }

    audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    maxMusicStreamVolumeLevel = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    mSensorEventListener = object : SensorEventListener {
      override fun onSensorChanged(sensorEvent: SensorEvent?) {
        sensorEvent?.values?.apply {
          val sideWardsTilt = this[0]
          val forthBackWardsTilt = this[1]

          var initializationCycle = false

          if (initialSideWardsTilt == null) {
            initialSideWardsTilt = sideWardsTilt
            initializationCycle = true
          }
          if (initialForthBackWardsTilt == null) {
            initialForthBackWardsTilt = forthBackWardsTilt
            initializationCycle = true
          }

          if (initializationCycle)
            return

          if (sideWardsTilt > initialSideWardsTilt!! + sensitivity || sideWardsTilt < initialSideWardsTilt!! - sensitivity) {
            playSirenSound()
          } else if (forthBackWardsTilt > initialForthBackWardsTilt!! + sensitivity || forthBackWardsTilt < initialForthBackWardsTilt!! - sensitivity) {
            playSirenSound()
          }
        }
      }

      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
  }

  private fun  playSirenSound() {
    if (::sirenSoundMediaPlayer.isInitialized)
      if (!sirenSoundMediaPlayer.isPlaying) {
        startVolumeForceControlRoutine()

        sirenSoundMediaPlayer.start()
      }
  }

  private fun stopSirenSound() {
    if (sirenSoundMediaPlayer.isPlaying) {
      stopVolumeForceControlRoutine()

      sirenSoundMediaPlayer.stop()
    }
  }

  private fun startVolumeForceControlRoutine() {
    volumeForceControlRoutine = CoroutineScope(IO).launch {
      latestInitialVolumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

      while (true) {
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < maxMusicStreamVolumeLevel)
          audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)

        delay(50)
      }
    }
  }

  private fun stopVolumeForceControlRoutine() {
    volumeForceControlRoutine?.cancel()
    volumeForceControlRoutine = null

    if (latestInitialVolumeLevel != null) {
      val currentVolumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

      if (currentVolumeLevel > latestInitialVolumeLevel!!)
        while (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != latestInitialVolumeLevel)
          audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
      else if (currentVolumeLevel < latestInitialVolumeLevel!!)
        while (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != latestInitialVolumeLevel)
          audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
    }
  }

  override fun onBind(p0: Intent?): IBinder? = null

  @SuppressLint("LaunchActivityFromNotification")
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val notification = NotificationCompat.Builder(applicationContext, BudgeDetectorNotificationManager.NOTIFICATION_CHANNEL_ID)
      .setContentTitle(getString(R.string.budge_detection_active))
      .setContentText(getString(R.string.tap_to_stop))
      .setSmallIcon(R.mipmap.ic_launcher)
      .setOngoing(true)
      .setSilent(true)
      .setAutoCancel(true)
      .setContentIntent(
        PendingIntent.getBroadcast(
          applicationContext,
          BudgeDetectorNotificationManager.BUDGE_DETECTOR_SERVICE_STOPPER_RECEIVER_INTENT_REQUEST_CODE,
          Intent(applicationContext, BudgeDetectorServiceStopperBroadcastReceiver::class.java),
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE
          else
            PendingIntent.FLAG_ONE_SHOT
        )
      )
      .build()

    startForeground(BudgeDetectorNotificationManager.NOTIFICATION_ID, notification)

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    instance = null

    mSensorManager.unregisterListener(mSensorEventListener)

    stopSirenSound()
  }

  companion object {
    private var instance: BudgeDetectorService? = null
      set (value) {
        field = value
        lvdpIsRunning.postValue(field != null)
      }

    private var lvdpIsRunning: MutableLiveData<Boolean> = MutableLiveData(instance != null)

    val lvdIsRunning = lvdpIsRunning as LiveData<Boolean>

    fun stop() {
      instance?.stopSelf()
    }

    fun start(context: Context) {
      val intent = Intent(context, BudgeDetectorService::class.java)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        context.startForegroundService(intent)
      else
        context.startService(intent)
    }
  }
}