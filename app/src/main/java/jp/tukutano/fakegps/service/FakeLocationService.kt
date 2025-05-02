package jp.tukutano.fakegps.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import jp.tukutano.fakegps.R

class FakeLocationService : Service() {

    companion object {
        const val EXTRA_LAT = "EXTRA_LAT"
        const val EXTRA_LNG = "EXTRA_LNG"
        private const val PROVIDER = LocationManager.GPS_PROVIDER
        private const val CHANNEL_ID = "fake_gps_channel"
        private const val NOTIF_ID = 1
        // 注入間隔（ミリ秒）
        private const val INTERVAL_MS = 1000L
    }

    private lateinit var locMgr: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private var currentLat = Double.NaN
    private var currentLng = Double.NaN

    private val injectRunnable = object : Runnable {
        override fun run() {
            if (!currentLat.isNaN() && !currentLng.isNaN()) {
                injectLocation(currentLat, currentLng)
            }
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        locMgr = getSystemService<LocationManager>()!!
        // テストプロバイダ登録
        try {
            locMgr.addTestProvider(
                PROVIDER,
                false,false,false,false,
                true,true,true,
                android.location.provider.ProviderProperties.POWER_USAGE_LOW,
                android.location.provider.ProviderProperties.ACCURACY_FINE
            )
        } catch (_: Exception){ }
        locMgr.setTestProviderEnabled(PROVIDER, true)

        // 通知チャンネル作成（API26+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Fake GPS Service",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val lat = it.getDoubleExtra(EXTRA_LAT, Double.NaN)
            val lng = it.getDoubleExtra(EXTRA_LNG, Double.NaN)
            if (!lat.isNaN() && !lng.isNaN()) {
                currentLat = lat
                currentLng = lng
            }
        }

        // まだ Foreground 起動していなければ通知を出して起動
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || startId == 1) {
            startForeground(NOTIF_ID, makeNotification())
        }

        // 定期注入を開始
        handler.removeCallbacks(injectRunnable)
        handler.post(injectRunnable)

        return START_STICKY
    }

    private fun makeNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fake GPS is running")
            .setContentText("Mock location: $currentLat, $currentLng")
            .setSmallIcon(R.drawable.ic_share)  // 任意のアイコン
            .setOngoing(true)
            .build()
    }

    private fun injectLocation(lat: Double, lng: Double) {
        val loc = Location(PROVIDER).apply {
            latitude = lat; longitude = lng
            accuracy = 1f
            time = System.currentTimeMillis()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                verticalAccuracyMeters = 1f
                bearingAccuracyDegrees = 0.1f
                speedAccuracyMetersPerSecond = 0.1f
            }
        }
        locMgr.setTestProviderLocation(PROVIDER, loc)
    }

    override fun onDestroy() {
        handler.removeCallbacks(injectRunnable)
        try { locMgr.removeTestProvider(PROVIDER) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}