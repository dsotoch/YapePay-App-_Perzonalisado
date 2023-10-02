package com.example.yapepay.services

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
import com.example.yapepay.R
import com.example.yapepay.principal
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class ListenerService : NotificationListenerService() {

    private val CHANNEL_ID="1"
     lateinit var listenerStatus:ListenerStatus
    private val CHANNEL_NEW="2"
    private val CHANNEL_NEW_EXIT="3"
    private lateinit var  principal: principal
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate() {
        super.onCreate()
        listenerStatus=ListenerStatus()
        principal= principal()
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
            .setStyle(
                NotificationCompat.BigTextStyle().bigText("Todas las Operaciones de Yape seran Procesadas y enviadas al Ecommerce")
            )
            .setContentText("Todos los Servicios Iniciados")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        var notificationManager=getSystemService(Context.NOTIFICATION_SERVICE)as NotificationManager
        notificationManager.notify(2,builder.build())
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


    fun sendData( sharedPreferences: SharedPreferences,sbn:StatusBarNotification?) {
        var pref =sharedPreferences
        val domain = domainCheck(pref)
        val url = "https://$domain/"
        if(sbn?.packageName =="com.bcp.innovacxion.yapeapp"){
            var title=sbn?.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.trim()
            if(title=="Confirmación de Pago"){

                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl(url)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val apiService = retrofit.create(ApiRetrofit::class.java)

                    val text = sbn?.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    val regex = Regex("""!(.*?)te""")
                    val matchResult = text?.let { regex.find(it) }
                    var payer=""
                    if (matchResult != null) {
                        payer = matchResult.groupValues[1].replace(" ", "").trim()
                    }
                    var amount =""
                    val regex2 = Regex("""S/ ([\d.]+)""")
                    val result=text?.let { regex2.find(it) }
                    if(result!=null)
                    {
                        amount=result.groupValues[1].trim()
                    }
                    val requestBody=RequestBody("CONFIRMACIONDEPAGO",payer,amount)

                    val call = apiService.sendData(requestBody)
                    call.enqueue(object : retrofit2.Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                            if (response.isSuccessful) {
                                val responseBody = response.body()
                                updateQuantySent(sharedPreferences)
                                Log.d("EXITO",response.message())


                            } else {
                                Log.d("SOLICITUDERROR",response.message())
                                Toast.makeText(this@ListenerService,"Error" + response.message(),Toast.LENGTH_LONG).show()

                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            val message = t.message.toString()
                            Log.d("ERROR", "$message ${call.request().url()}")

                        // Toast.makeText(this@ListenerService,"Error" + message +" "+t.message.toString(),Toast.LENGTH_LONG).show()
                        }
                    })

                } catch (e: Exception) {
                    e.message?.let { Log.d("ERROR", it) };
                }
            }else{
                Log.d("ERROR",title.toString());

            }

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
     fun  notification(){
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.woo_logo_icon_249164)
            .setContentTitle("YAPE PAY")
            .setContentText("Yape Pay WOO..")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
         startForeground(1,builder)

    }


}