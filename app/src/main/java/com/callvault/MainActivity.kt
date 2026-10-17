package com.callvault
import android.Manifest
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.Scope
import java.io.File
class MainActivity:ComponentActivity(){
 override fun onCreate(s:Bundle?){super.onCreate(s);setContent{
  MaterialTheme(colorScheme=if(isSystemInDarkTheme())darkColorScheme()else lightColorScheme()){App()}}}
}
@Composable fun App(){
 val c=LocalContext.current
 var consent by remember{mutableStateOf(Store.get(c,"consent",false))}
 var tab by remember{mutableStateOf(0)}; var tick by remember{mutableStateOf(0)}
 val perms=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){tick++}
 if(!consent){ AlertDialog(onDismissRequest={},title={Text("Recording disclosure")},
  text={Text("CallVault records phone calls on this device and uploads them to your Google Drive. Laws on call recording vary; you must get consent from all parties where required. A notification is shown while recording. Support depends on your Android version and phone maker.")},
  confirmButton={TextButton(onClick={Store.set(c,"consent",true);consent=true
   perms.launch(arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_PHONE_STATE,Manifest.permission.POST_NOTIFICATIONS))}){Text("I understand")}}); return }
 Scaffold(bottomBar={NavigationBar{
  listOf("Dashboard" to Icons.Default.Home,"Library" to Icons.Default.Mic,"Settings" to Icons.Default.Settings).forEachIndexed{i,(t,ic)->
   NavigationBarItem(tab==i,{tab=i;tick++},{Icon(ic,t)},label={Text(t)})}}}){p->
  Box(Modifier.padding(p).padding(16.dp)){ key(tick){ when(tab){0->Dashboard();1->Library{tick++};else->Settings{tick++}} } }}
}
@Composable fun Stat(l:String,v:String){Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Row(Modifier.padding(16.dp).fillMaxWidth(),Arrangement.SpaceBetween){Text(l);Text(v,style=MaterialTheme.typography.titleMedium)}}}
@Composable fun Dashboard(){
 val c=LocalContext.current; val fs=Store.files(c); val st=fs.map{Store.status(c,it)}
 val acc=GoogleSignIn.getLastSignedInAccount(c)
 Column{ Text("CallVault",style=MaterialTheme.typography.headlineMedium)
  Stat("Automatic recording",if(Store.get(c,"auto",true))"Enabled" else "Disabled")
  Stat("Total recordings","${fs.size}"); Stat("Uploaded","${st.count{it=="Uploaded"}}")
  Stat("Pending uploads","${st.count{it!="Uploaded"}}")
  Stat("Google Drive",acc?.email?:"Not connected")
  Stat("Last backup",fs.filter{Store.status(c,it)=="Uploaded"}.maxOfOrNull{it.lastModified()}?.let{java.util.Date(it).toString()}?:"—")
  Stat("Storage used","${fs.sumOf{it.length()}/1024} KB") }
}
@Composable fun Library(refresh:()->Unit){
 val c=LocalContext.current; var q by remember{mutableStateOf("")}; var mp by remember{mutableStateOf<MediaPlayer?>(null)}
 val fs=Store.files(c).filter{it.name.contains(q,true)}
 Column{ OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),label={Text("Search")},leadingIcon={Icon(Icons.Default.Search,null)})
  if(fs.isEmpty())Text("No recordings yet.",Modifier.padding(24.dp))
  LazyColumn{items(fs){f:File->
   val s=if(!Store.get(c,"backup",true)&&Store.status(c,f)!="Uploaded")"Paused" else Store.status(c,f)
   ListItem(headlineContent={Text(f.name.removeSuffix(".m4a"))},supportingContent={Text("${f.length()/1024} KB · $s")},
    trailingContent={Row{
     IconButton({mp?.release();mp=MediaPlayer().apply{setDataSource(f.path);prepare();start()}}){Icon(Icons.Default.PlayArrow,"Play")}
     IconButton({mp?.pause()}){Icon(Icons.Default.Pause,"Pause")}
     IconButton({f.delete();refresh()}){Icon(Icons.Default.Delete,"Delete")}}})}}}
}
@Composable fun Settings(refresh:()->Unit){
 val c=LocalContext.current
 val gso=GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(Scope(SCOPE)).build()
 val client=GoogleSignIn.getClient(c,gso)
 val signIn=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){refresh();UploadWorker.enqueue(c)}
 @Composable fun Sw(l:String,k:String,d:Boolean=true){var v by remember{mutableStateOf(Store.get(c,k,d))}
  Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text(l);Switch(v,{v=it;Store.set(c,k,it);if(k!="auto")UploadWorker.enqueue(c)})}}
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text("Settings",style=MaterialTheme.typography.headlineMedium)
  Sw("Automatic recording","auto");Sw("Automatic backup","backup");Sw("Upload via Wi-Fi","wifi");Sw("Upload via mobile data","mobile");Sw("Mobile data usage warning","warn")
  Button({signIn.launch(client.signInIntent)}){Text(if(GoogleSignIn.getLastSignedInAccount(c)==null)"Connect Google Drive" else "Switch Google account")}
  OutlinedButton({UploadWorker.enqueue(c)}){Text("Retry failed / pending uploads")}
  OutlinedButton({client.signOut().addOnCompleteListener{Store.putStr(c,"folder",null);refresh()}}){Text("Disconnect Google account")}
  Text("Recording may be limited by your Android version and phone maker. You are responsible for obtaining consent.",style=MaterialTheme.typography.bodySmall)}
}
