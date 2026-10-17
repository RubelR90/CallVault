package com.callvault
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.IBinder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
class RecorderService:Service(){
 private var rec:MediaRecorder?=null; private var file:File?=null
 override fun onBind(i:Intent?):IBinder?=null
 override fun onStartCommand(i:Intent?,f:Int,id:Int):Int{
  val nm=getSystemService(NotificationManager::class.java)
  nm.createNotificationChannel(NotificationChannel("rec","Recording",NotificationManager.IMPORTANCE_LOW))
  val n=Notification.Builder(this,"rec").setContentTitle("CallVault is recording this call")
   .setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).build()
  startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
  val name=SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date())+".m4a"
  file=File(Store.dir(this),name)
  rec=runCatching{MediaRecorder(this).apply{
   setAudioSource(MediaRecorder.AudioSource.MIC) // 3rd-party apps usually capture only the mic side on Android 10+
   setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
   setOutputFile(file!!.path);prepare();start()}}.getOrNull()
  if(rec==null)stopSelf()
  return START_NOT_STICKY
 }
 override fun onDestroy(){
  runCatching{rec?.stop()};rec?.release();rec=null
  file?.takeIf{it.exists()}?.let{Store.setStatus(this,it,"Pending");UploadWorker.enqueue(this)}
  super.onDestroy()
 }
}
