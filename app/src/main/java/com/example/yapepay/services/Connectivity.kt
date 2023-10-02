package com.example.yapepay.services

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.yapepay.principal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Connectivity {

    private lateinit var auth: FirebaseAuth


    fun verifiConnectivity(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

            return networkCapabilities != null &&
                    (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } else {
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
            val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

            return isConnected
        }
    }


    fun login(email: String, pass: String, context: Context, app: ComponentActivity,sharedPreferences: SharedPreferences):Boolean {
                var  response=false
        try {
            auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser == null) {
                try {
                    auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            savePreferences(sharedPreferences  )
                            response=true
                            val intent = Intent(context, principal::class.java)
                            app.startActivity(intent)
                            app.finish()



                        } else {
                            response=false
                            val dialog = AlertDialog.Builder(context)
                                .setTitle("Error")
                                .setMessage(task.exception?.message ?: "Error Desconocido")
                                .create()
                            dialog.show()


                        }
                    }
                } catch (ex: FirebaseAuthException) {
                }
            } else {
                response=false
                val dialog = AlertDialog.Builder(context)
                    .setTitle("Error")
                    .setMessage("Usuario ya Autenticado")
                    .create()
                dialog.show()
            }
        } catch (ex: Exception) {
            println(ex.message)
        }
        return response

    }

    fun openWhatsapp(app: ComponentActivity) {
        val phoneNumber = "51916715991"
        val message =
            "Hola, tengo problemas para Iniciar Sesion ... A continuacion adjunto la captura de mi compra en Viru Tec."

        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://wa.me/$phoneNumber/?text=${Uri.encode(message)}")
        app.startActivity(intent)
    }

    fun savePreferences(sharedPreferences: SharedPreferences) {
        with(sharedPreferences.edit()) {
            putString("login", "si")
            apply()
        }

    }

    fun verificatePreferences(sharedPreferences: SharedPreferences): String {

       return sharedPreferences.getString("login","no").toString()
    }


}
