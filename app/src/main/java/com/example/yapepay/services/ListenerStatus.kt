package com.example.yapepay.services

import android.app.Notification
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yapepay.principal
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class ListenerStatus {
    val principal =principal()
     fun updateDomain(context: Context,domain:String,apikey:String ,preferences: SharedPreferences){
         with(preferences.edit()){
             putString("domain",domain)
             putString("apikey",apikey)
             apply()
         }

    }

     fun updateStateApi(context:Context,newstate: Boolean,preferences: SharedPreferences){
       with(preferences.edit()) {
            putBoolean("stateapi",newstate)
           apply()
        }
    }

    fun  StateApiCheck(preferences: SharedPreferences):Boolean{

        return preferences.getBoolean("stateapi",false)
    }



    fun  domainCheck(preferences: SharedPreferences):String{

        return preferences.getString("domain","NO CONFIGURADO").toString()
    }

    fun testTheDomain(domain: String,apikey:String): String {
         try {
            val url = URL("https://$domain/")
            // Si la URL es válida, continuar con el código de conexión
            val urlConnection = url.openConnection() as HttpURLConnection
            try {
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("apikey",apikey)
                urlConnection.doOutput = true
                urlConnection.setChunkedStreamingMode(0)
                val responseCode = urlConnection.responseCode
               return responseCode.toString()
            } finally {
                urlConnection.disconnect()
            }
        } catch (ex:Exception){
            return ex.message.toString()
        }
    }
    fun getQuantySent(sharedPreferences: SharedPreferences):Int{
        val pref = sharedPreferences
        return pref.getInt("cantidad_enviados",0)
    }
    fun updateQuantySent(preferences: SharedPreferences){
        val quanty=getQuantySent(preferences)
        with(preferences.edit()){
            putInt("cantidad_enviados",quanty+1)
            apply()
        }

    }

    fun Cantidad_Notificaciones(preferences: SharedPreferences): Int {
        val cantidad=preferences
        return cantidad.getInt("cantidad_notificaciones",0)
    }




}