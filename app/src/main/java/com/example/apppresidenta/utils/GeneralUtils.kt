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
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.LoginActivity
import com.example.apppresidenta.R
import com.example.apppresidenta.RegistroActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileFilter
import java.security.Key
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

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

        fun mostrarAlertErrorSms(contexto: Context?) {
            val alert = AlertDialog.Builder(contexto)
            alert.setMessage(contexto?.getString(R.string.error_sms))
            alert.setPositiveButton(android.R.string.ok) { _, _ ->
                //En caso de que el sms no se pueda enviar desde el servidor.
                val intent = Intent(contexto,LoginActivity::class.java)
                contexto!!.startActivity(intent)
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

        fun obtenerTokenNotificaciones(contexto: Context?) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Installations", "Installation auth token: " + task.result)

                    //En cuanto se obtiene el token para envio de notificaciones se guarda en una variable de sesion
                    registrarVariableSesion(contexto,contexto!!.getString(R.string.token),task.result)
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

        fun creacionComplementosApp(contexto: Context?){

            //Se crean los canales de notificaciones
            crearCanalesNotificaciones(contexto)

            //Se registra el dispositivo al tema de generales (notificaciones para cuestiones generales)
            registrarTema()

            //Se obtiene el id del dispositivo
            val id_device = obtenerIdDispositivo(contexto)
            Log.d("Installations", "Installation id device: $id_device")


//            if(!id_device.isNullOrEmpty()){
//
//                val jsonParametros = JSONObject()
//                jsonParametros.put("id_cliente", idCliente)
//                jsonParametros.put("celular", numeroCelular)
//                jsonParametros.put("token", token)
//                jsonParametros.put("id_device", id_device)
//
//                val request = JsonObjectRequest(
//                    Request.Method.POST,
//                    contexto!!.getString(R.string.url_registro_token),
//                    jsonParametros,
//                    { response ->
//                        try {
//                            //Obtiene su respuesta json
//                            Log.d("Registro Token-ID","Respuesta: $response")
//                            //Toast.makeText(contexto, "Respuesta: $response", Toast.LENGTH_SHORT).show()
//                        } catch (e: Exception) {
//                            Log.e("Registro Token-ID","Ocurrio un error en el registro: $e")
//                        }
//                    },
//                    { error ->
//                        try {
//                            val responseError = String(error.networkResponse.data)
//                            val dataError = JSONObject(responseError)
//                            val messageError = dataError.getString("message")
//                            Log.d("Registro Token-ID","Respuesta error: $dataError")
//                            Toast.makeText(contexto, messageError, Toast.LENGTH_SHORT).show()
//                        }catch (e: Exception){
//                            Toast.makeText(contexto, "Ocurrio un error en el registro del Token", Toast.LENGTH_LONG).show()
//                            Log.e("Registro Token-ID","Ocurrio un error en el registro: $e")
//                        }
//                    })
//                try {
//                    val queue = Volley.newRequestQueue(contexto)
//                    //primero borramos el cache y enviamos despues la peticion
//                    queue.cache.clear()
//                    queue.add(request)
//                }catch(e: Exception){
//                    Log.e("Registro Token-ID","Ocurrio un error en el registro: $e")
//                }
//            }
//            else{
//                Log.d("Registro Token-ID"
//                    , "Ocurrio un error en el registro un valor es null, token: $token , id_device: $id_device")
//            }
        }

        /*Envio de SMS y Encriptacion*/
        fun envioSms(contexto: Context?, numeroCelular: String, codigo: String, appSignature: String){

            val mensaje = "<#> Su codigo App Alfin es :$codigo $appSignature"
            val appId = contexto!!.getString(R.string.idApp).toInt()
            val jsonParametros = JSONObject()
            jsonParametros.put("aplicacionId", appId)
            jsonParametros.put("celular", numeroCelular)
            //jsonParametros.put("celular", "5611363168")
            jsonParametros.put("mensaje", mensaje)

            val request = object : JsonObjectRequest(
                Method.POST,
                contexto!!.getString(R.string.url_envio_sms),
                jsonParametros,
                { response ->
                    try {
                        //Obtiene su respuesta json
                        Log.d("Envio SMS","Respuesta: $response")
                        //Toast.makeText(contexto, "Respuesta: $response", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("Envio SMS","Ocurrio un error en el envio sms: $e")
                    }
                },
                { error ->
                    try {
                        val responseError = String(error.networkResponse.data)
                        val dataError = JSONObject(responseError)
                        val messageError = dataError.getString("message")
                        Log.d("Envio SMS","Respuesta error: $dataError")
                        //Toast.makeText(contexto, messageError, Toast.LENGTH_SHORT).show()
                        mostrarAlertErrorSms(contexto)
                    }catch (e: Exception){
                        Log.e("Envio SMS","Ocurrio un error en el envio sms: $e")
                        mostrarAlertErrorSms(contexto)
                        //Toast.makeText(contexto, "Ocurrio un error en el envio de SMS, favor de intetarlo nuevamente.", Toast.LENGTH_LONG).show()
                    }
                })
            {
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] = contexto!!.getString(R.string.content_type)
                    headers["Authorization"] = "${encriptacion(contexto,"${appId}-${numeroCelular}","",2)}"
                    //Log.d("Encriptacion",encriptacion(contexto,"2604","167600",1)!!)
                    //headers["Authorization"] = ""
                    headers["Header-Identification"] = "${encriptacion(contexto,appId.toString(),"",2)}"
                    headers["Header-Data"] = "${encriptacion(contexto,numeroCelular,"",2)}"
                    return headers
                }
            }
            try {
                val queue = Volley.newRequestQueue(contexto)
                //primero borramos el cache y enviamos despues la peticion
                queue.cache.clear()
                queue.add(request)
            }catch(e: Exception){
                Log.e("Envio SMS","Ocurrio un error en el envio sms: $e")
            }
        }

        private fun obtenerKeyEncriptacion(contexto: Context?,idCliente: String,opcion: Int):String
        {
            var keyCompartida = ""

            if (opcion == 1)
            {
                val packagename = contexto!!.applicationContext.packageName;

                val packageNamePartes = packagename.split(".")

                val idClienteReverse = idCliente.reversed()

                keyCompartida = "${packageNamePartes[0]}-" +
                        "${idClienteReverse}-" +
                        "${packageNamePartes[1]}-" +
                        "${idCliente}-" +
                        packageNamePartes[2]

                Log.d("Encriptacion","$keyCompartida ${keyCompartida.substring(0,32)}")
            }
            else
            {
                val tz: TimeZone = TimeZone.getTimeZone("GMT-06:00")
                val calendario: Calendar = Calendar.getInstance(tz)

                val diaMes = calendario.get(Calendar.DAY_OF_MONTH)
                val diaAno = calendario.get(Calendar.DAY_OF_YEAR)
                //val semanaAno = calendario.get(Calendar.WEEK_OF_YEAR)
                val ano = calendario.get(Calendar.YEAR)
                var diaLetra = ""
                val numeroDia: Int = calendario.get(Calendar.DAY_OF_WEEK)
                when (numeroDia) {
                    1 -> diaLetra = "DOM"
                    2 -> diaLetra = "LUN"
                    3 -> diaLetra = "MAR"
                    4 -> diaLetra = "MIE"
                    5 -> diaLetra = "JUE"
                    6 -> diaLetra = "VIE"
                    7 -> diaLetra = "SAB"
                }

                val diaParte1 = convertirCadenaToHex(diaLetra.substring(0,1))
                val diaParte2 = convertirCadenaToHex(diaLetra.substring(1,2))
                val diaParte3 = convertirCadenaToHex(diaLetra.substring(2,3))

                val sumaNumeros = (ano-(diaMes+diaAno))

                val keyNormal = "$diaMes$diaLetra$diaParte1$diaAno$diaParte2$sumaNumeros$diaParte3"

                val keyAlreves = keyNormal.reversed()

                val divisorKey = keyNormal.length/2

                keyCompartida = "${keyNormal.substring(0,divisorKey)}-" +
                        "${keyNormal.substring(divisorKey,keyNormal.length)}" +
                        "${keyAlreves.substring(divisorKey,keyNormal.length)}-" +
                        "${keyAlreves.substring(0,divisorKey)}"

                Log.d("Encriptacion","$keyCompartida ${keyCompartida.substring(0,32)}")
            }

            return keyCompartida.substring(0,32)
        }

        fun validaIntentosSms(contexto: Context?) : Boolean{
            val prefs = PreferenceManager.getDefaultSharedPreferences(contexto)

            var intentos = prefs.getString("INTENTOS", "0")!!.toInt()

            if (prefs.getString("FECHA_ACTUAL", "") == ""){
                registrarVariableSesion(contexto,"FECHA_ACTUAL", validaFechaActual())
            }
            else if(prefs.getString("FECHA_ACTUAL", "") != validaFechaActual())
            {
                registrarVariableSesion(contexto,"FECHA_ACTUAL", validaFechaActual())
                intentos = 0
            }

            intentos += 1
            registrarVariableSesion(contexto,"INTENTOS", intentos.toString())

            return intentos <= 3
        }

        fun validaFechaActual() : String{
            val tz: TimeZone = TimeZone.getTimeZone("GMT-06:00")
            val calendario: Calendar = Calendar.getInstance(tz)

            val diaActual = calendario.get(Calendar.DAY_OF_MONTH)
            val anoActual = calendario.get(Calendar.YEAR)
            val mesActual = calendario.get(Calendar.MONTH)

            return "$diaActual-$mesActual-$anoActual"
        }

        fun encriptacion(contexto: Context?,textoEncriptar: String,idCliente: String,opcion: Int): String? {
            val aesKey: Key = SecretKeySpec(obtenerKeyEncriptacion(contexto,idCliente,opcion).toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, aesKey)
            val encriptado = cipher.doFinal(textoEncriptar.toByteArray())
            return Base64.encodeToString(encriptado, Base64.DEFAULT)
            //return Base64.encodeBytes(encrypted)
        }

        private fun convertirCadenaToHex(str: String): String? {
            var st = ""
            val chars: ByteArray = str.toByteArray()
            for (b in chars) {
                st = String.format("%02X", b)
            }
            return st
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