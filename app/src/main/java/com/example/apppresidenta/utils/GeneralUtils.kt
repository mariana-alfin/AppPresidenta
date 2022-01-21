package com.example.apppresidenta.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.example.apppresidenta.R
import java.util.*

class GeneralUtils {

    companion object {

        const val ASK_FOR_PERMISSION_CAMERA = 300
        const val ASK_FOR_PERMISSION_GPS = 100
        const val PHOTO_CODE = 200

        fun nombreRandom(): String = UUID.randomUUID().toString()

        fun validarAplicacionInstalada(packageName: String, contexto: Context?): Boolean {
            val packageManager = contexto?.packageManager
            return try {
                packageManager?.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                true
            } catch (notFoundException: PackageManager.NameNotFoundException) {
                false
            }
        }

        fun mostrarAlertInstalarApp(contexto: Context?, packageName: String){
            val alert = AlertDialog.Builder(contexto)
            alert.setMessage(R.string.instalacion_app)
            alert.setPositiveButton(android.R.string.ok) { _, _ ->
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(
                    contexto?.getString(R.string.play_store) + packageName)
                contexto?.startActivity(i)
            }
            alert.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            alert.show()
        }

        fun mostrarAlertActivarPermisos(contexto: Context?, packageName: String) {
            val alert = AlertDialog.Builder(contexto)
            alert.setTitle(R.string.permisos_denegados)
            alert.setMessage(R.string.mensaje_permisos_denegados)
            alert.setPositiveButton(android.R.string.ok) { _, _ ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                contexto?.startActivity(intent)
            }
            alert.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            alert.show()
        }

        fun mostrarAlertActivacionGPS(contexto: Context?,actividad: Activity) {
            val alert = AlertDialog.Builder(contexto)
            alert.setMessage(contexto?.getString(R.string.estatus_gps))
            alert.setPositiveButton(android.R.string.ok) { _, _ ->
                //En caso de que el usuario no acepte prender la ubicacion se cierra la actividad de la junta
                actividad.finish()
            }
            alert.show()
        }

        fun enviarMensajeWhatsApp(context: Context?, message: String, number: String) {
            val sendIntent = Intent(Intent.ACTION_VIEW)
            val uri = "https://wa.me/52"+number+"/?text="+message
            sendIntent.setData(Uri.parse(uri))
            context?.startActivity(sendIntent)
        }

        fun llamarContacto(context: Context?, telefono : String) {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:+52$telefono")
            context?.startActivity(intent)
        }
    }
}