package com.example.yapepay.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.yapepay.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.LinkedList
import java.util.Queue


class ListenerService : NotificationListenerService() {
    private var cantidad_notificaciones=0
    private val requestQueue: Queue<Call<Void>> = LinkedList()
    private val CHANNEL_ID="1"
    lateinit var listenerStatus:ListenerStatus
    private val CHANNEL_NEW="2"
    private val CHANNEL_NEW_EXIT="3"
    private lateinit var sharedPreferences: SharedPreferences
    private val NOTIFICATION_ID = 1




    override fun onCreate() {
        super.onCreate()
        listenerStatus=ListenerStatus()
        newChannelNotification()
        newChannelNotificationExit()
        sharedPreferences = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        //notification()
        statuApiStart()
        newNotification()
        Start(this)
        return START_STICKY
    }

    private fun newChannelNotification(){
        val name ="newNotificacionStartService"
        val importance=NotificationManager.IMPORTANCE_DEFAULT
        val channel=NotificationChannel(CHANNEL_NEW,name, importance)

        val notificationManager:NotificationManager=getSystemService(Context.NOTIFICATION_SERVICE)as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun newNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_NEW)
            .setSmallIcon(R.drawable.woo_logo_icon_249164)
            .setContentTitle("SERVICIO DE YAPE PAY INICIADO")
            .setColorized(true)
            .setColor(ContextCompat.getColor(this,R.color.light_blue_A200))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText("Todas las Operaciones de Yape seran Procesadas y enviadas al Ecommerce")

            )
            .setContentText("Todos los Servicios Iniciados")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        /*var notificationManager=getSystemService(Context.NOTIFICATION_SERVICE)as NotificationManager
        notificationManager.notify(2,builder.build())*/
        startForeground(2,builder.build())
    }
    fun statuApiStart() {
        listenerStatus.updateStateApi(this, true, sharedPreferences)
    }
    fun statuApiStop() {
        listenerStatus.updateStateApi(this, false, sharedPreferences)
    }

    fun Start(context: Context){

           Toast.makeText(context,"SERVICIO INICIADO", Toast.LENGTH_LONG).show()


    }

    fun domainCheck(preferences: SharedPreferences):String{

        return preferences.getString("domain","NO CONFIGURADO").toString()
    }
    fun apikeycheck(preferences: SharedPreferences):String{
        return preferences.getString("apikey","null").toString()
    }
    fun getBaseUrl(urlString: String): String {
        val indexOfSlash = urlString.indexOf('/')
        return if (indexOfSlash != -1) {
            urlString.substring(0, indexOfSlash)
        } else {
            urlString
        }
    }
    fun getPathAfterSlash(urlString: String): String {
        val indexOfSlash = urlString.indexOf('/')
        return if (indexOfSlash != -1 && indexOfSlash < urlString.length - 1) {
            urlString.substring(indexOfSlash + 1)
        } else {
            ""
        }
    }
    fun capitalizarCadena(input: String): String {
        val palabras = input.split(" ")

        val resultado = palabras.joinToString(" ") { palabra ->
            palabra.toLowerCase().capitalize()
        }

        return resultado
    }

    @SuppressLint("SuspiciousIndentation")
    fun sendData(sharedPreferences: SharedPreferences, sbn:StatusBarNotification?) {
        var pref =sharedPreferences
        val domain = domainCheck(pref)
        val apikey= apikeycheck(pref)
        if(sbn?.packageName =="com.bcp.innovacxion.yapeapp"){
            var title=sbn?.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.trim()
            if(title=="Confirmación de Pago"){
                try {
                    val text = sbn?.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    val regex = Regex("""!(.*?)te""")
                    val matchResult = text?.let { regex.find(it) }
                    var pagador: String
                    var payer=""
                    if (matchResult != null) {
                        pagador = matchResult.groupValues[1].trim()
                        payer=capitalizarCadena(pagador)
                    }
                    var amount =""
                    val regex2 = Regex("""S/ ([\d.]+)""")
                    val result=text?.let { regex2.find(it) }
                    if(result!=null)
                    {
                        amount=result.groupValues[1].trim()
                    }
                    var url=getBaseUrl(domain)
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://$url/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val apiService = retrofit.create(ApiRetrofit::class.java)
                    val requestBody=RequestBody("Yape",payer,amount)
                    try {
                        val call = apiService.sendData(getPathAfterSlash(domain),apikey,requestBody)
                        enqueueRequest(call)
                    } catch (ex: Exception) {
                        Log.e("Error en la Solicitud", ex.message.toString())
                    }
                }catch (ex:Exception){
                    ex.printStackTrace()
                    Log.e("Error", ex.message.toString())

                }                }

        }
    }
    private fun enqueueRequest(call: Call<Void>) {
        requestQueue.add(call)
        Actualizar_Notificaciones(sharedPreferences)

        processNextRequest()
    }


    private fun processNextRequest() {
        if (requestQueue.isNotEmpty()) {
            val nextRequest = requestQueue.poll()
            nextRequest?.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        updateQuantySent(sharedPreferences)
                        Log.d("EXITO",response.message())
                    } else {
                        Log.d("SOLICITUDERROR",response.message())
                        Toast.makeText(this@ListenerService,"Error" + response.message(),Toast.LENGTH_LONG).show()

                    }
                    cantidad_notificaciones--
                    processNextRequest()
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    val message = t.message.toString()
                    Log.d("ERROR", "$message ${call.request().url()}")
                    cantidad_notificaciones--
                    processNextRequest()
                }
            })
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


    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sendData(sharedPreferences,sbn)



    }


    override fun onDestroy() {
        super.onDestroy()
        newNotificationExit()
        statuApiStop()
        stopSelf()

    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        newNotificationExit()
        statuApiStop()
        stopForeground(true)
        stopSelf()

    }
    private fun newChannelNotificationExit(){
        val name="newNotificationStopService"
        val importance=NotificationManager.IMPORTANCE_DEFAULT
        val channel=NotificationChannel(CHANNEL_NEW_EXIT,name,importance)
        val notificationManager:NotificationManager=getSystemService(Context.NOTIFICATION_SERVICE)as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun newNotificationExit(){
        var builder=NotificationCompat.Builder(this,CHANNEL_NEW_EXIT)
            .setSmallIcon(R.drawable.woo_logo_icon_249164)
            .setContentTitle("SERVICIO DE YAPE PAY DETENIDO")
            .setContentText("Todos los Servicios Detenidos")

        val NotificationManager:NotificationManager=getSystemService(Context.NOTIFICATION_SERVICE)as NotificationManager
        NotificationManager.notify(CHANNEL_NEW_EXIT.toInt(),builder.build())

    }
    fun Actualizar_Notificaciones(preferences: SharedPreferences){
        cantidad_notificaciones++
        with(preferences.edit()){
            putInt("cantidad_notificaciones",cantidad_notificaciones)
            apply()
        }
        Log.d("Actualizado notificacion",cantidad_notificaciones.toString())
    }


}