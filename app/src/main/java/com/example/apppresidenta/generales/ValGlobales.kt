package com.example.apppresidenta.generales

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity


class ValGlobales {

    companion object{
        fun validarConexion(activity: AppCompatActivity): Boolean {
            //MD FUNCION PARA VALIDAR LA CONEXION DE INTERNET
            val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

            capabilities.also {
                if (it != null){
                    // Si hay conexión a Internet en este momento
                    if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                        return true
                    else if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                        return true
                    }
                }
            }
            // No hay conexión a Internet en este momento
            FuncionesGlobales.mostrarAlert(activity,"advertencia",true,"CONEXION","No cuenta con conexión a Internet",false).show()
            return false
        }
        fun validarConexionObsoleto(activity: AppCompatActivity):Boolean{
            val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo

            return if (networkInfo != null && networkInfo.isConnected) {
                true
                // Si hay conexión a Internet en este momento
                //Toast.makeText(this, "SI HAY CONEXION", Toast.LENGTH_SHORT).show()
            } else {
                FuncionesGlobales.mostrarAlert(activity,"advertencia",true,"CONEXION","No cuenta con conexión a Internet",false).show()
                false
                // No hay conexión a Internet en este momento
            }
        }
    }

}