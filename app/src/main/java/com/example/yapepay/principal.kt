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
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.example.yapepay.services.ListenerService
import com.example.yapepay.services.ListenerStatus
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
    @SuppressLint("SuspiciousIndentation")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)
        listener= ListenerStatus()
        pref=this.getSharedPreferences("settings", MODE_PRIVATE)
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
                    btndomain?.text="Conectando..."
                    listener.updateDomain(this@principal ,domain?.text.toString(),pref)
                    val domainText = domain?.text.toString()

                    GlobalScope.launch(Dispatchers.IO) {
                        val res = listener.testTheDomain(domainText)
                        withContext(Dispatchers.Main) {
                            if (res == "200") {
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


}