package com.lifelevel.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private var pageReady = false
    private val scope = MainScope()
    private val stepPermissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    private val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted: Set<String> ->
        if (granted.containsAll(stepPermissions)) readTodaySteps() else js("healthConnectResult(false, 'Доступ к шагам не разрешён')")
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Set<String> -> js("nativeNotificationPermission($granted)") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pageReady = true
                refreshStepsIfGranted()
            }
        }
        web.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        setContentView(web)
        web.loadUrl("file:///android_asset/web/index.html")
        ReminderScheduler.scheduleAll(this)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun js(script: String) = runOnUiThread { if (::web.isInitialized) web.evaluateJavascript(script, null) }

    private fun refreshStepsIfGranted() {
        if (!pageReady) return
        scope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(this@MainActivity)
                val granted = client.permissionController.getGrantedPermissions()
                if (granted.containsAll(stepPermissions)) readTodaySteps()
            } catch (_: Exception) {
                // Keep the manual sync action available; background permission may be absent.
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStepsIfGranted()
    }

    private fun requestHealthConnect() {
        val status = HealthConnectClient.getSdkStatus(this)
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            js("healthConnectResult(false, 'Health Connect не установлен или требует обновления')")
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))) } catch (_: Exception) {}
            return
        }
        scope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(this@MainActivity)
                val granted = client.permissionController.getGrantedPermissions()
                if (granted.containsAll(stepPermissions)) readTodaySteps()
                else healthPermissionLauncher.launch(stepPermissions)
            } catch (e: Exception) {
                js("healthConnectResult(false, 'Не удалось открыть Health Connect')")
            }
        }
    }

    private fun readTodaySteps() {
        scope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(this@MainActivity)
                val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
                val end = Instant.now()
                val result = client.aggregate(AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                ))
                val steps = result[StepsRecord.COUNT_TOTAL] ?: 0L
                js("healthConnectResult(true, 'Шаги синхронизированы'); applyNativeSteps($steps)")
            } catch (e: Exception) {
                js("healthConnectResult(false, 'Ошибка чтения шагов. Проверь разрешение Health Connect.')")
            }
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface fun requestStepsPermission() { runOnUiThread { requestHealthConnect() } }
        @JavascriptInterface fun syncSteps() { runOnUiThread { requestHealthConnect() } }
        @JavascriptInterface fun saveReminderSettings(waterEveryHours: Int, waterEnabled: Boolean, eatStart: String, eatEnd: String, fastingEnabled: Boolean) {
            getSharedPreferences("lifelevel", Context.MODE_PRIVATE).edit()
                .putInt("waterEvery", waterEveryHours.coerceIn(1, 8))
                .putBoolean("waterEnabled", waterEnabled)
                .putString("eatStart", eatStart)
                .putString("eatEnd", eatEnd)
                .putBoolean("fastingEnabled", fastingEnabled).apply()
            ReminderScheduler.scheduleAll(this@MainActivity)
        }
        @JavascriptInterface fun requestNotificationPermission() {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else js("nativeNotificationPermission(true)")
            }
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("lifelevel_reminders", "Напоминания LIFE LEVEL", NotificationManager.IMPORTANCE_DEFAULT))
    }

    override fun onDestroy() { scope.cancel(); web.destroy(); super.onDestroy() }
}
