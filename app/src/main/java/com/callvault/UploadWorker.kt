package com.callvault
import android.content.Context
import androidx.work.*
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
const val SCOPE="https://www.googleapis.com/auth/drive.file" // minimum scope
class UploadWorker(c:Context,p:WorkerParameters):CoroutineWorker(c,p){
 override suspend fun doWork():Result=withContext(Dispatchers.IO){
  val c=applicationContext
  if(!Store.get(c,"backup",true))return@withContext Result.success()
  val acc=GoogleSignIn.getLastSignedInAccount(c)?.account?:return@withContext Result.success()
  try{
   val tok=GoogleAuthUtil.getToken(c,acc,"oauth2:$SCOPE")
   val folder=Store.str(c,"folder")?:create(tok,"CallVault",null,"application/vnd.google-apps.folder").also{Store.putStr(c,"folder",it)}
   for(f in Store.files(c)){
    if(Store.status(c,f)=="Uploaded")continue // duplicate prevention
    Store.setStatus(c,f,"Uploading")
    try{ create(tok,f.name,folder,"audio/mp4",f.readBytes()); Store.setStatus(c,f,"Uploaded") }
    catch(e:Exception){Store.setStatus(c,f,"Failed");throw e}
   }
   Result.success()
  }catch(e:Exception){Result.retry()}
 }
 private fun create(tok:String,name:String,parent:String?,mime:String,data:ByteArrayOrNull=null):String{
  val meta=JSONObject().put("name",name).put("mimeType",mime)
  if(parent!=null)meta.put("parents",org.json.JSONArray().put(parent))
  val b="cvb";val o=ByteArrayOutputStream()
  o.write("--$b\r\nContent-Type: application/json\r\n\r\n$meta\r\n--$b\r\nContent-Type: $mime\r\n\r\n".toByteArray())
  data?.let{o.write(it)};o.write("\r\n--$b--".toByteArray())
  val u=URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
  val h=(u.openConnection() as HttpURLConnection).apply{requestMethod="POST";doOutput=true
   setRequestProperty("Authorization","Bearer $tok");setRequestProperty("Content-Type","multipart/related; boundary=$b")}
  h.outputStream.use{it.write(o.toByteArray())}
  check(h.responseCode in 200..299){"Drive ${h.responseCode}"}
  return JSONObject(h.inputStream.bufferedReader().readText()).getString("id") // id returned = verified
 }
 companion object{
  fun enqueue(c:Context){
   val w=Store.get(c,"wifi",true);val m=Store.get(c,"mobile",true)
   val net=if(!m)NetworkType.UNMETERED else if(!w)NetworkType.METERED else NetworkType.CONNECTED
   WorkManager.getInstance(c).enqueueUniqueWork("upload",ExistingWorkPolicy.APPEND_OR_REPLACE,
    OneTimeWorkRequestBuilder<UploadWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(net).build())
     .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
  }
 }
}
typealias ByteArrayOrNull=ByteArray?
