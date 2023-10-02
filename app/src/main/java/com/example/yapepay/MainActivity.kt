package com.example.yapepay

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.DialogFragment

import com.example.yapepay.services.Connectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
     lateinit var connectivity :Connectivity
        lateinit var  sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isLogin(this)
        setContentView(R.layout.activity_main)
        sharedPreferences=getSharedPreferences("settings", MODE_PRIVATE)
        connectivity= Connectivity()
        var email :EditText=findViewById(R.id.email)
        var pass :EditText=findViewById(R.id.pass)
        var btnLogin: Button = findViewById(R.id.login)
        var nologin:TextView=findViewById(R.id.nologin)

        btnLogin.setOnClickListener {

            if(connectivity.verifiConnectivity(this)==false){

                notNetwork()
            }else{

                if(email.text.toString()=="" || pass.text.toString()=="") {

                    dataIncomplete()
                }else{

                    try {
                         connectivity.login(email.text.toString(),pass.text.toString(), this,this@MainActivity,sharedPreferences )

                    }catch (ex:Exception){
                        Toast.makeText(this,"ERROR" +ex.message.toString(),Toast.LENGTH_LONG).show()
                    }

                }
            }

        }

        nologin.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                connectivity.openWhatsapp(this@MainActivity)
            }
        }

        )
    }

    fun isLogin(context: Context){
        val scope = CoroutineScope(Dispatchers.Main)
        val res = scope.async {
            connectivity.verificatePreferences(sharedPreferences)
        }

        scope.launch {
            val result = res.await() // Esperar y obtener el resultado

            if (result == "si") {
                val intent = Intent(context, principal::class.java)
                startActivity(intent)
                finish()
            }
        }

    }
    fun notNetwork() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Error")
            .setIcon(R.drawable.ofline)
            .setMessage("SIN CONEXION A INTERNET")
            .create()
        dialog.show()
    }

    fun dataIncomplete(){
        val dialog=AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage("Por Favor Complete todos los Campos")
            .create()
        dialog.show()

    }


}



