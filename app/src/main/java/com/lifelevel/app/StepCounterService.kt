package com.lifelevel.app
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.ZoneId
class StepCounterService:Service(),SensorEventListener{
 private lateinit var manager:SensorManager
 private val prefs by lazy{getSharedPreferences(PREFS,Context.MODE_PRIVATE)}
 override fun onCreate(){super.onCreate();isRunning=true;createChannel();val n=NotificationCompat.Builder(this,CHANNEL_ID).setSmallIcon(android.R.drawable.ic_menu_directions).setContentTitle("LIFE LEVEL · шагомер активен").setContentText("Подсчитываю шаги в течение дня").setOngoing(true).setCategory(NotificationCompat.CATEGORY_SERVICE).build();startForeground(NOTIFICATION_ID,n);manager=getSystemService(Context.SENSOR_SERVICE) as SensorManager;val sensor=manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);if(sensor==null){prefs.edit().putString("stepStatus","В телефоне нет аппаратного датчика шагов").apply();stopSelf();return};manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL)}
 override fun onSensorChanged(event:SensorEvent?){val raw=event?.values?.firstOrNull()?.toLong()?:return;val today=LocalDate.now(ZoneId.systemDefault()).toString();val oldDay=prefs.getString(KEY_DAY,null);val last=prefs.getLong(KEY_RAW,-1L);var count=prefs.getInt(KEY_STEPS,0);if(oldDay!=today){prefs.edit().putString(KEY_DAY,today).putInt(KEY_STEPS,0).putLong(KEY_RAW,raw).apply();return};if(last<0){prefs.edit().putLong(KEY_RAW,raw).apply();return};if(raw<last){prefs.edit().putLong(KEY_RAW,raw).apply();return};val delta=raw-last;if(delta>0){count=(count+delta.coerceAtMost(1000L)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt();prefs.edit().putLong(KEY_RAW,raw).putInt(KEY_STEPS,count).apply();val n=NotificationCompat.Builder(this,CHANNEL_ID).setSmallIcon(android.R.drawable.ic_menu_directions).setContentTitle("LIFE LEVEL · $count шагов").setContentText("Шагомер работает в фоне").setOngoing(true).build();getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID,n)}}
 override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int)=Unit
 private fun createChannel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID,"Шагомер LIFE LEVEL",NotificationManager.IMPORTANCE_LOW))}
 override fun onDestroy(){isRunning=false;if(::manager.isInitialized)manager.unregisterListener(this);super.onDestroy()}
 override fun onBind(intent:Intent?):IBinder?=null
 companion object{const val CHANNEL_ID="lifelevel_step_counter";private const val NOTIFICATION_ID=701;private const val PREFS="lifelevel_steps";private const val KEY_DAY="day";private const val KEY_RAW="lastRaw";private const val KEY_STEPS="todaySteps";@Volatile var isRunning=false;fun todaySteps(c:Context):Int{val p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);return if(p.getString(KEY_DAY,null)==LocalDate.now(ZoneId.systemDefault()).toString())p.getInt(KEY_STEPS,0) else 0}}
}