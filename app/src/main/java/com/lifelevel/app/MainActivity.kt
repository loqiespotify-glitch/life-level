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
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
class MainActivity : ComponentActivity() {
 private lateinit var web:WebView
 private var pageReady=false
 private val scope=MainScope()
 private val stepPermissions=setOf(HealthPermission.getReadPermission(StepsRecord::class))
 private val healthPermissionLauncher=registerForActivityResult(PermissionController.createRequestPermissionResultContract()){granted:Set<String>->if(granted.containsAll(stepPermissions))readTodaySteps() else js("healthConnectResult(false,'Доступ не разрешён')")}
 private val activityPermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)startNativeStepService() else js("nativeStepStatus(false,'Разрешение на физическую активность не выдано')")}
 private val notificationPermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted:Boolean->js("nativeNotificationPermission($granted)")}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);createNotificationChannel();web=WebView(this);web.settings.javaScriptEnabled=true;web.settings.domStorageEnabled=true;web.settings.allowFileAccess=true;web.webChromeClient=WebChromeClient();web.webViewClient=object:WebViewClient(){override fun onPageFinished(view:WebView?,url:String?){pageReady=true;refreshSteps()}};web.addJavascriptInterface(AndroidBridge(),"AndroidBridge");setContentView(web);web.loadUrl("file:///android_asset/web/index.html");ReminderScheduler.scheduleAll(this);if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)}
 private fun js(s:String){runOnUiThread{if(::web.isInitialized&&pageReady)web.evaluateJavascript(s,null)}}
 private fun refreshSteps(){if(!pageReady)return;js("applyNativeSteps(${StepCounterService.todaySteps(this)})");js("nativeStepStatus(${StepCounterService.isRunning},'${if(StepCounterService.isRunning) "Шагомер активен" else "Шагомер выключен"}')")}
 override fun onResume(){super.onResume();refreshSteps()}
 private fun requestNativeSteps(){if(Build.VERSION.SDK_INT>=29&&ContextCompat.checkSelfPermission(this,Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED)activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) else startNativeStepService()}
 private fun startNativeStepService(){try{ContextCompat.startForegroundService(this,Intent(this,StepCounterService::class.java));js("nativeStepStatus(true,'Шагомер запускается…')")}catch(e:Exception){js("nativeStepStatus(false,'Не удалось запустить шагомер')")}}
 private fun requestHealthConnect(){val status=HealthConnectClient.getSdkStatus(this);if(status!=HealthConnectClient.SDK_AVAILABLE){js("healthConnectResult(false,'Health Connect не установлен или требует обновления')");try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.google.android.apps.healthdata")))}catch(_:Exception){};return};scope.launch{try{val c=HealthConnectClient.getOrCreate(this@MainActivity);val granted=c.permissionController.getGrantedPermissions();if(granted.containsAll(stepPermissions))readTodaySteps() else healthPermissionLauncher.launch(stepPermissions)}catch(_:Exception){js("healthConnectResult(false,'Не удалось открыть Health Connect')")}}}
 private fun readTodaySteps(){scope.launch{try{val c=HealthConnectClient.getOrCreate(this@MainActivity);val start=LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();val result=c.aggregate(AggregateRequest(metrics=setOf(StepsRecord.COUNT_TOTAL),timeRangeFilter=TimeRangeFilter.between(start,Instant.now())));val n=result[StepsRecord.COUNT_TOTAL]?:0L;js("healthConnectResult(true,'Health Connect синхронизирован');applyNativeSteps($n)")}catch(_:Exception){js("healthConnectResult(false,'Ошибка чтения Health Connect')")}}}
 inner class AndroidBridge{
  @JavascriptInterface fun checkForUpdates(){
   Thread {
    var connection:HttpURLConnection?=null
    try {
     connection=(URL("https://api.github.com/repos/loqiespotify-glitch/life-level/releases/latest").openConnection() as HttpURLConnection).apply { requestMethod="GET"; connectTimeout=10000; readTimeout=10000; setRequestProperty("Accept","application/vnd.github+json"); setRequestProperty("User-Agent","LIFE-LEVEL-Android") }
     if(connection.responseCode !in 200..299) throw Exception("HTTP ${connection.responseCode}")
     val data=JSONObject(connection.inputStream.bufferedReader().use{it.readText()})
     val tag=data.optString("tag_name","").removePrefix("v")
     val latest=data.optString("html_url","https://github.com/loqiespotify-glitch/life-level/releases/latest")
     val current=BuildConfig.VERSION_NAME
     fun parts(v:String)=v.split(".").map{it.takeWhile{ch->ch.isDigit()}.toIntOrNull()?:0}
     val x=parts(tag);val y=parts(current);var newer=false
     for(i in 0 until maxOf(x.size,y.size)){val xv=x.getOrElse(i){0};val yv=y.getOrElse(i){0};if(xv!=yv){newer=xv>yv;break}}
     val payload=JSONObject().put("ok",true).put("current",current).put("latest",tag).put("update",newer).put("url",latest)
     runOnUiThread{js("onUpdateCheck(${JSONObject.quote(payload.toString())})")}
    } catch(e:Exception){
     val payload=JSONObject().put("ok",false).put("error","Не удалось проверить обновления. Проверь интернет.")
     runOnUiThread{js("onUpdateCheck(${JSONObject.quote(payload.toString())})")}
    } finally { connection?.disconnect() }
   }.start()
  }
  @JavascriptInterface fun openUpdatePage(url:String){runOnUiThread{try{val uri=Uri.parse(url);if(uri.scheme=="https"&&uri.host=="github.com")startActivity(Intent(Intent.ACTION_VIEW,uri))}catch(_:Exception){}}}
  @JavascriptInterface fun startStepCounter(){runOnUiThread{requestNativeSteps()}}
  @JavascriptInterface fun stopStepCounter(){runOnUiThread{stopService(Intent(this@MainActivity,StepCounterService::class.java));StepCounterService.isRunning=false;js("nativeStepStatus(false,'Шагомер остановлен')")}}
  @JavascriptInterface fun getTodaySteps():Int=StepCounterService.todaySteps(this@MainActivity)
  @JavascriptInterface fun isStepCounterRunning():Boolean=StepCounterService.isRunning
  @JavascriptInterface fun syncSteps(){runOnUiThread{requestNativeSteps()}}
  @JavascriptInterface fun requestStepsPermission(){runOnUiThread{requestNativeSteps()}}
  @JavascriptInterface fun syncHealthConnect(){runOnUiThread{requestHealthConnect()}}
  @JavascriptInterface fun saveReminderSettings(waterEveryHours:Int,waterEnabled:Boolean,eatStart:String,eatEnd:String,fastingEnabled:Boolean){getSharedPreferences("lifelevel",Context.MODE_PRIVATE).edit().putInt("waterEvery",waterEveryHours.coerceIn(1,8)).putBoolean("waterEnabled",waterEnabled).putString("eatStart",eatStart).putString("eatEnd",eatEnd).putBoolean("fastingEnabled",fastingEnabled).apply();ReminderScheduler.scheduleAll(this@MainActivity)}
  @JavascriptInterface fun requestNotificationPermission(){runOnUiThread{if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else js("nativeNotificationPermission(true)")}}
 }
 private fun createNotificationChannel(){val nm=getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel("lifelevel_reminders","Напоминания LIFE LEVEL",NotificationManager.IMPORTANCE_DEFAULT));nm.createNotificationChannel(NotificationChannel(StepCounterService.CHANNEL_ID,"Шагомер LIFE LEVEL",NotificationManager.IMPORTANCE_LOW))}
 override fun onDestroy(){scope.cancel();if(::web.isInitialized)web.destroy();super.onDestroy()}
}