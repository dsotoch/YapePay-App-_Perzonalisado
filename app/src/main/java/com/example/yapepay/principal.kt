package com.example.yapepay


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.example.yapepay.services.ListenerService
import com.example.yapepay.services.ListenerStatus
import com.example.yapepay.services.dataBody
import com.example.yapepay.services.servidorEstado
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class principal : AppCompatActivity() {
    private lateinit var  notificationLiveData:ListenerService
    lateinit var listener:ListenerStatus
    lateinit var pref:SharedPreferences
    lateinit var txtdomain:TextView
    lateinit var  btnaction:Button
     var verifierExitState: String="no"
    lateinit var  state:TextView
    lateinit var sent:TextView
    var estado =0
    private val CHANNEL_ID="1"
    private lateinit var semaforo:ImageView
    private val handlerThread =HandlerThread("Verificar estado de cola del Servidor")
    private lateinit var handler:Handler

    fun iniciarVerificacionEnOnCreate() {
        handlerThread.start()
        handler= Handler(handlerThread.looper)
        handler.post(VerificarCambiosSemaforo())
    }
    fun EnviarEstadoServidor( estado_cola:Int ){
        var estado_colaverde=""
        var estado_colanaranja=""
        var estado_colarojo=""
        var estadoStr=when(estado_cola){
            500->estado_colaverde="500"
            1000->estado_colanaranja="1000"
            else ->estado_colarojo="0"

        }
        var estadoString=when(estado_cola){
            500->"Verde"
            1000->"Amarillo"
            else ->"Rojo"

        }
        val rf=Retrofit.Builder()
            .baseUrl("https://uygczevnxayqgfaiuyuy.supabase.co")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val request=rf.create(servidorEstado::class.java)
        val estadoColaVerdeInt = try {
            estado_colaverde.toInt()
        } catch (e: NumberFormatException) {
            null
        }

        val estadoColaNaranjaInt = try {
            if (estado_colanaranja.isNotEmpty()) estado_colanaranja.toInt() else null
        } catch (e: NumberFormatException) {
            // Manejar la excepción, por ejemplo, asignar un valor predeterminado o lanzar un error
            // En este ejemplo, se asigna null como predeterminado para indicar que no hay valor
            null
        }

        val estadoColaRojoInt = try {
            estado_colarojo.toInt()
        } catch (e: NumberFormatException) {
            // Manejar la excepción, por ejemplo, asignar un valor predeterminado o lanzar un error
            // En este ejemplo, se asigna el valor 0 como predeterminado
            null
        }
        val cal:Call<Void> = request.enviarData("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV5Z2N6ZXZueGF5cWdmYWl1eXV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MDA5OTkwNDksImV4cCI6MjAxNjU3NTA0OX0.hgb12daiZVI3p6d8xAY-jyYwY3VmSQNp9nGeS0cymRo",
            dataBody(estadoColaVerdeInt, estadoColaNaranjaInt, estadoColaRojoInt))
        cal.enqueue(object:Callback<Void>{
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    actualizarServidor(pref,estadoString)
                    val estado = response.code()
                    Log.d("Exito al enviar estado",estado.toString())

                } else {
                    Log.d("Error al enviar estado","El servidor si respondio,pero erroneo ${response.body().toString()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Error al enviar estado",t.message.toString())
            }

        })
    }
    fun detenerVerificacion() {
        handler.removeCallbacksAndMessages(null)
        handlerThread.quit()
    }
    fun estadoServidor(sharedPreferences: SharedPreferences):String{
        return sharedPreferences.getString("cola","nulo").toString()
    }
    fun actualizarServidor(sharedPreferences: SharedPreferences,cola:String){
        with(sharedPreferences.edit()){
            putString("cola",cola)
            apply()
        }
    }
   private fun VerificarCambiosSemaforo(): Runnable {


       return object :Runnable {
           override fun run() {
               val  cantidad_notificaciones =listener.Cantidad_Notificaciones(pref)
               var estado_cola = when {
                   cantidad_notificaciones <= 500 -> 500
                   cantidad_notificaciones in 501 .. 1000 ->1000
                   else->0

               }
               val estadoActual=estadoServidor(pref)
               Log.d("ESTADO",estadoActual)
               var enviar=true
               if(estado_cola==500 && estadoActual.equals("Verde")){
                   enviar=false
               }else{
                   if(estado_cola==1000 && estadoActual.equals("Amarillo") ){
                       enviar=false
                   }else{
                       if(estado_cola==0 && estadoActual.equals("Rojo") ){
                           enviar=false
                       }
                   }
               }
               updateSemaphore(cantidad_notificaciones)
               if(enviar){
                   EnviarEstadoServidor(estado_cola)
               }
               handler.postDelayed(this, 1000)
           }
       }
   }

    @SuppressLint("SuspiciousIndentation")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)
        semaforo=findViewById(R.id.imagensemaforo)
        listener= ListenerStatus()
        pref=this.getSharedPreferences("settings", MODE_PRIVATE)

        iniciarVerificacionEnOnCreate()
        txtdomain=findViewById(R.id.domain)
        btnaction=findViewById(R.id.btnaction)
        sent=findViewById(R.id.send)
        notificationLiveData= ListenerService()
        estado++
        state=findViewById(R.id.state)
        notificationchannel()

        updateQuantyView()
        val floatingActionButton:FloatingActionButton=findViewById(R.id.floatingActionButton)
        floatingActionButton.setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog.setContentView(R.layout.settings)

            // Buscar las vistas dentro del bottomSheetDialog
            val domain: EditText? = bottomSheetDialog.findViewById(R.id.editTextdomain)
            val apikey: EditText?=bottomSheetDialog.findViewById(R.id.editTextapikey)
            val btndomain: Button? = bottomSheetDialog.findViewById(R.id.btndomain)
            val detail:TextView?=bottomSheetDialog.findViewById(R.id.txtdetails)
            bottomSheetDialog.show()
            btndomain?.setOnClickListener {
                if(domain?.text.toString()==""){
                  val dialog=  AlertDialog.Builder(bottomSheetDialog.context)
                        .setTitle("ERROR")
                        .setMessage("NO HAS INGRESADO EL DOMINIO . INTENTALO NUEVAMENTE!")
                        .create()
                    dialog.show()

                }else{
                    if(apikey?.text.toString()==""){
                        val dialog=  AlertDialog.Builder(bottomSheetDialog.context)
                            .setTitle("ERROR")
                            .setMessage("NO HAS INGRESADO LA APIKEY . INTENTALO NUEVAMENTE!")
                            .create()
                        dialog.show()

                    }else{
                        btndomain?.text="Conectando..."
                        listener.updateDomain(this@principal ,domain?.text.toString(),apikey?.text.toString(), pref)
                        val domainText = domain?.text.toString()
                        val apikeyText=apikey?.text.toString()
                        GlobalScope.launch(Dispatchers.IO) {
                            val res = listener.testTheDomain(domainText,apikeyText)
                            withContext(Dispatchers.Main) {
                                if (res == "200" || res == "201") {
                                    detail?.text = "CONEXION ESTABLECIDA"
                                    btndomain?.text="REGISTRAR"

                                } else {
                                    val dialog = AlertDialog.Builder(bottomSheetDialog.context)
                                        .setTitle("Ocurrió un Error")
                                        .setMessage(res)
                                        .create()
                                    dialog.show()
                                    detail?.text = "SIN CONEXION"
                                    btndomain?.text="REGISTRAR"
                                }
                            }
                        }

                        var res= listener.domainCheck(pref)
                        txtdomain.text=res
                    }




                }
            }
        }

        btnaction.setOnClickListener {
            if( btnaction.text=="INICIAR"){

                    if(listener.domainCheck(pref)=="NO CONFIGURADO"){
                        val permissionExplanation = "Dominio no Configurado.. Configura tu Dominio y vuelve a intentarlo."
                        val dialogBuilder = AlertDialog.Builder(this)
                        dialogBuilder.setTitle("Error de Configuración")
                        dialogBuilder.setMessage(permissionExplanation)
                        dialogBuilder.create().show()
                    }else{
                        if(checkNotificationListenerPermission(this)==false){
                            updateStop()
                            listener.updateStateApi(this,false,pref)
                            requestNotificationListenerPermission()

                        }else{

                            if(isAppExcludedFromBatteryOptimization(this)==true){
                                startService(Intent(this,ListenerService::class.java))
                                updatestart()
                            }else{
                                showBatteryOptimizationDialog(this)
                            }

                        }
                    }



            }else{
                verifierExitState="si"

                val stopIntent = Intent(this, ListenerService::class.java)
                stopService(stopIntent)
                StopService()
            }
        }
    }


    fun modificarSemaforo(tipo:String){
       try {
           when(tipo){
               "verde"->semaforo.setImageResource(R.drawable.verde)
               "amarillo"->semaforo.setImageResource(R.drawable.amarillo)
               "rojo"->semaforo.setImageResource(R.drawable.rojo)
           }
       }catch (ex:Exception){
           Log.d("Error en Cambiar Semaforo",ex.message.toString())
       }
    }
    fun showBatteryOptimizationDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Exclusión de optimización de batería")
        builder.setMessage("Para garantizar el correcto funcionamiento de la aplicación en segundo plano, se recomienda excluir la aplicación de la optimización de batería. ¿Desea realizar esta configuración ahora?")
        builder.setPositiveButton("Sí") { _, _ ->
            addAppToBatteryOptimizationExclusionList(context)
        }
        builder.setNegativeButton("No") { _, _ ->

        }
        builder.show()
    }
    fun addAppToBatteryOptimizationExclusionList(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)

        }
    }
    fun isAppExcludedFromBatteryOptimization(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
        fun updateStop(){
            state.text="INACTIVO"
            btnaction.text="INICIAR"
        }
        fun updatestart(){
            state.text="ACTIVO"
            btnaction.text="DETENER"
        }

    private fun checkNotificationListenerPermission(context: Context): Boolean {
        val componentName = ComponentName(context, ListenerService::class.java)
        val enabledListenerPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledListenerPackages.contains(componentName.packageName)
    }

    private fun requestNotificationListenerPermission() {
        val permissionExplanation = "Para brindarte una experiencia completa, necesitamos acceder a tus notificaciones. Esto nos permite realizar ciertas acciones basadas en las notificaciones de Yape que recibes."
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Permiso de Notificación")
        dialogBuilder.setMessage(permissionExplanation)
        dialogBuilder.setPositiveButton("Configuración") { _, _ ->
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
        dialogBuilder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = dialogBuilder.create()
        dialog.show()
    }
    private fun notificationchannel(){
        val name = "chanelyapepay"
        val descriptionText ="notificaciondeyapepay"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }





    private fun StopService(){
        val permissionExplanation = "Quita el Permiso de Notificaciones para YapePay para Detener el Servicio Completamente"
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Quitar Permiso de Notificación")
        dialogBuilder.setMessage(permissionExplanation)
        dialogBuilder.setPositiveButton("Configuración") { _, _ ->
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
            dialogBuilder.create().show()
    }

    fun updateQuantyView(){
        sent.text=listener.getQuantySent(pref).toString()
    }
    fun updateQuantyView2(){
        listener.updateQuantySent(pref)
        sent.text=listener.getQuantySent(pref).toString()
    }

    override fun onResume() {
        super.onResume()
        var res= listener.domainCheck(pref)
        txtdomain.text=res
        if(listener.StateApiCheck(pref)==true){
            updatestart()
        }else{
            updateStop()
        }
        updateQuantyView()

        if(listener.StateApiCheck(pref)==true){
            if(isAppExcludedFromBatteryOptimization(this)==true){
                startService(Intent(this,ListenerService::class.java))
                updatestart()
            }else{
                showBatteryOptimizationDialog(this)
            }
        }

    }

    fun updateSemaphore(cant:Int) {

        try {
            Log.d("Semaforo",cant.toString())
           when {
                 cant in 1..500 -> {
                      modificarSemaforo("verde")
                  }
                cant in 501..1000 -> {
                    modificarSemaforo("amarillo")
                }
                cant > 1000 -> {
                    modificarSemaforo("rojo")

                }
            }


        }catch (ex:Exception){
            Log.d("Error en updatE Semaphore",ex.message.toString())
        }
    }


}