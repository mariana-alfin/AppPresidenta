package com.example.apppresidenta.generales

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class ValGlobales {

    companion object{
        fun validarConexion(activity: AppCompatActivity): Boolean {
            //MD FUNCION PARA VALIDAR LA CONEXION DE INTERNET
            try {
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
            }catch (e: Exception){
                //Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
                return false
            }
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

        /**FUNCIONES QUE ESTABAN EN EL ARCHIVO PermisosUtils.kt**/

//        private fun versionAndroidPreguntarPermisos() =
//            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

        /**REVISAR AJUSTE DE QUITAR IF DE LA VERSION**/
        private fun validaPermisos(contexto: Context, permisos: Array<String>): Boolean {
            //if (versionAndroidPreguntarPermisos()) {
                for (permiso in permisos) {
                    if (ActivityCompat.checkSelfPermission(
                            contexto,
                            permiso
                        ) != PackageManager.PERMISSION_GRANTED
                    )
                        return true
                }
            //}
            return false
        }

        fun preguntarPorPermisos(
            contexto: Context,
            permisos: Array<String>,
            actividad: Activity,
            codigoSolicitud: Int
        ): Boolean {
            if (validaPermisos(contexto, permisos)) {
                if (solicitudPermisos(contexto, permisos)) {
                    ActivityCompat.requestPermissions(actividad, permisos, codigoSolicitud)
                    return false
                } else {
                    ActivityCompat.requestPermissions(actividad, permisos, codigoSolicitud)
                    return false
                }
            } else {
                return true
            }
        }

        private fun solicitudPermisos(contexto: Context, permisos: Array<String>): Boolean {
            for (permiso in permisos) {
                if ((contexto as Activity).shouldShowRequestPermissionRationale(permiso)) {
                    return true
                }
            }
            return false
        }

    }

}