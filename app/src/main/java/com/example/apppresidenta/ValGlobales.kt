package com.example.apppresidenta

import android.content.Context
import android.net.ConnectivityManager
import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ValGlobales {

    companion object{
        fun validarConexion(activity: AppCompatActivity):Boolean{
            val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo

            if (networkInfo != null && networkInfo.isConnected) {
                return true
                // Si hay conexión a Internet en este momento
                //Toast.makeText(this, "SI HAY CONEXION", Toast.LENGTH_SHORT).show()
            } else {
                FuncionesGlobales.mostrarAlert(activity,"advertencia",true,"CONEXION","No cuenta con conexión a Internet",false).show()
                return false
                // No hay conexión a Internet en este momento
            }
        }

    }

}