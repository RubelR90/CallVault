package com.callvault
import android.content.Context
import java.io.File
object Store {
 private fun p(c:Context)=c.getSharedPreferences("cv",0)
 fun get(c:Context,k:String,d:Boolean)=p(c).getBoolean(k,d)
 fun set(c:Context,k:String,v:Boolean)=p(c).edit().putBoolean(k,v).apply()
 fun str(c:Context,k:String)=p(c).getString(k,null)
 fun putStr(c:Context,k:String,v:String?)=p(c).edit().putString(k,v).apply()
 fun dir(c:Context)=File(c.filesDir,"rec").apply{mkdirs()}
 fun files(c:Context)=dir(c).listFiles()?.sortedByDescending{it.lastModified()}?:emptyList()
 fun status(c:Context,f:File)=str(c,"st_"+f.name)?:"Pending"
 fun setStatus(c:Context,f:File,s:String)=putStr(c,"st_"+f.name,s)
}
