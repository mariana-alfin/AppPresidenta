package com.example.apppresidenta.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

@RequiresApi(Build.VERSION_CODES.M)
class PermisosUtils {

    companion object {

        private fun versionAndroidPreguntarPermisos() =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

        private fun validaPermisos(contexto: Context, permisos: Array<String>): Boolean {
            if (versionAndroidPreguntarPermisos()) {
                for (permiso in permisos) {
                    if (ActivityCompat.checkSelfPermission(
                            contexto,
                            permiso
                        ) != PackageManager.PERMISSION_GRANTED
                    )
                        return true
                }
            }
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