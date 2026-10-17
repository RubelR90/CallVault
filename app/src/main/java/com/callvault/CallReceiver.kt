package com.callvault
import android.content.*
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
class CallReceiver:BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent){
  if(!Store.get(c,"auto",true))return
  val s=Intent(c,RecorderService::class.java)
  when(i.getStringExtra(TelephonyManager.EXTRA_STATE)){
   TelephonyManager.EXTRA_STATE_OFFHOOK->runCatching{ContextCompat.startForegroundService(c,s)}
   TelephonyManager.EXTRA_STATE_IDLE->c.stopService(s)
  }
 }
}
