package com.example.apppresidenta.utils

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.R
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileFilter
import java.text.SimpleDateFormat
import java.util.*

class GeneralUtils {

    companion object {

        const val ASK_FOR_PERMISSION_CAMERA = 300
        const val ASK_FOR_PERMISSION_GPS = 100
        const val PHOTO_CODE = 200

        private val canalIDRecordatorios = "recordatorioID"
        private val nombreCanalRecordatorio = "Avisos y Recordatorios"

        private val canalIDGeneral = "generalID"
        private val nombreCanalGeneral = "General"

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

        /*Funciones para notificaciones (registro de token, temas, canales de notificaciones)*/
        private fun crearCanalesNotificaciones(contexto: Context?) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                val importancia = NotificationManager.IMPORTANCE_HIGH

                val canalRecordatorios = NotificationChannel(canalIDRecordatorios,
                    nombreCanalRecordatorio,importancia).apply {
                    lightColor = Color.RED
                    enableLights(true)
                }

                val canalGeneral = NotificationChannel(canalIDGeneral,
                    nombreCanalGeneral,importancia).apply {
                    lightColor = Color.RED
                    enableLights(true)
                }

                val manager = contexto!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                manager.createNotificationChannel(canalRecordatorios)
                manager.createNotificationChannel(canalGeneral)
            }
        }

        fun obtenerTokenNotificaciones(contexto: Context?,idCliente: String, numeroCelular: String) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Installations", "Installation auth token: " + task.result)
                    registroTokenIdDispositivo(contexto,idCliente,numeroCelular,task.result)
                } else {
                    Log.e("Installations", "Unable to get Installation auth token")
                }

            })
        }

        private fun registrarTema(){
            FirebaseMessaging.getInstance().subscribeToTopic("Generales").addOnCompleteListener(OnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Suscripcion", "Suscripcion al tema, todo ok")
                } else {
                    Log.e("Suscripcion", "No fue posible suscribir al tema")
                }
            })
        }

        private fun obtenerIdDispositivo(contexto: Context?):
                String = Settings.Secure.getString(contexto!!.contentResolver, Settings.Secure.ANDROID_ID)

        private fun registroTokenIdDispositivo(contexto: Context?,idCliente: String, numeroCelular: String, token: String ){

            //Se crean los canales de notificaciones
            crearCanalesNotificaciones(contexto)

            //Se registra el dispositivo al tema de generales (notificaciones para cuestiones generales)
            registrarTema()

            //Se obtiene el id del dispositivo
            val id_device = obtenerIdDispositivo(contexto)
            Log.d("Installations", "Installation id device: $id_device")


            if(!id_device.isNullOrEmpty()){

                val jsonParametros = JSONObject()
                jsonParametros.put("id_cliente", idCliente)
                jsonParametros.put("celular", numeroCelular)
                jsonParametros.put("token", token)
                jsonParametros.put("id_device", id_device)

                val request = JsonObjectRequest(
                    Request.Method.POST,
                    contexto!!.getString(R.string.url_registro_token),
                    jsonParametros,
                    Response.Listener { response ->
                        try {
                            //Obtiene su respuesta json
                            Log.d("Registro Token-ID","Respuesta: $response")
                            //Toast.makeText(contexto, "Respuesta: $response", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e("Registro Token-ID","Ocurrio un error en el registro: $e")
                        }
                    },
                    Response.ErrorListener { error ->
                        val responseError = String(error.networkResponse.data)
                        val dataError = JSONObject(responseError)
                        try {
                            Log.d("Registro Token-ID","Respuesta error: $dataError")
                            //Toast.makeText(contexto, "Respuesta: $dataError", Toast.LENGTH_SHORT).show()
                        }catch (e: Exception){
                            Log.e("Registro Token-ID","Ocurrio un error en el registro: $e")
                        }
                    })
                try {
                    val queue = Volley.newRequestQueue(contexto)
                    //primero borramos el cache y enviamos despues la peticion
                    queue.cache.clear()
                    queue.add(request)
                }catch(e: Exception){
                    Log.e("Registro Token-ID","Ocurrio un error en el registro: $e")
                }
            }
            else{
                Log.d("Registro Token-ID"
                    , "Ocurrio un error en el registro un valor es null, token: $token , id_device: $id_device")
            }
        }

        /*Funciones para manejo de la fotografia*/

        fun asignarNombreFoto(contexto: Context?) : String{
            val prefs = PreferenceManager.getDefaultSharedPreferences(contexto)
            val prestamo = prefs.getInt("CREDITO_ID", 0)
            val tz: TimeZone = TimeZone.getTimeZone("GMT-06:00")
            val calendario: Calendar = Calendar.getInstance(tz)

            val dia = calendario.get(Calendar.DAY_OF_MONTH)
            val mes = calendario.get(Calendar.MONTH) + 1
            val ano = calendario.get(Calendar.YEAR)

            return "${prestamo}_$dia$mes$ano"
        }

        fun obtenerCadenaB64DeImagen(ruta: String): String? {
            val imagenExtension = ruta.split(".").last()
            if( extensionImagenes.indexOf(imagenExtension) >= 0 ){
                val bitmap = BitmapFactory.decodeFile(ruta)
                if (bitmap != null) return codificaBase64aBitmap(bitmap)
            }

            return if (ruta.isEmpty()) null else ruta

        }

        fun codificaBase64aBitmap(bitmap: Bitmap): String {
            var base64 = Base64.encodeToString(obtenerArregloByteDeImagen(bitmap), Base64.DEFAULT)
            base64 = base64.replace("\n", "")

            return base64.trim()
        }

        private val extensionImagenes = arrayOf("jpg", "jpeg", "png", "tiff", "tif", "bmp", "sgv")

        private fun obtenerArregloByteDeImagen(bitmap: Bitmap): ByteArray? {
            val byteArrayOutputSteam = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputSteam)

            return byteArrayOutputSteam.toByteArray()
        }

        /*Funcion para registrar variables de sesion*/
        fun registrarVariableSesion(contexto: Context?,nombreVariable: String,valorVariable: String)
        {
            val preferencias = PreferenceManager.getDefaultSharedPreferences(contexto)
            val editor = preferencias.edit()
            editor.putString(nombreVariable,valorVariable)
            editor.apply()
        }

        /*Funcion para registrar variables de sesion*/
        fun eliminaVariableSesion(contexto: Context?,nombreVariable: String)
        {
            val preferencias = PreferenceManager.getDefaultSharedPreferences(contexto)
            val editor = preferencias.edit()
            editor.remove(nombreVariable)
            editor.apply()
        }

        /*Funcion para eliminar fotos*/
        fun eliminarFotos(ruta: String) {
            val archivos: Array<File> = File(ruta).listFiles()
            for (archivo in archivos)
                archivo.delete()
        }
    }
}