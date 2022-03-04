package com.example.apppresidenta

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.LoadingScreen
import com.example.apppresidenta.generales.ValGlobales
import com.example.apppresidenta.navegacion.Navegacion
import com.example.apppresidenta.utils.GeneralUtils
import com.example.apppresidenta.utils.GeneralUtils.Companion.creacionComplementosApp
import org.json.JSONObject
import org.json.JSONTokener

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)
        FuncionesGlobales.guardarPestanaSesion(this, "MainActivity")
        supportActionBar?.hide()
        findViewById<Button>(R.id.btnIniSesion).setOnClickListener { iniciarSesion() }
        /*findViewById<Button>(R.id.btnIniSesion).setOnClickListener {
            FuncionesGlobales.guardarVariableSesion(this,"Int","CREDITO_ID","197223")
            val inicio = Intent(this, Navegacion::class.java)
            startActivity(inicio)
            finish()
        }*///para pruebas en lo que funciona el ws
        findViewById<Button>(R.id.btnRegistro).setOnClickListener { realizarRegistro() }
        findViewById<TextView>(R.id.txtNoCliente).requestFocus()
        findViewById<TextView>(R.id.txtOlvida).paintFlags = Paint.UNDERLINE_TEXT_FLAG
        findViewById<TextView>(R.id.txtOlvida).setOnClickListener { recuperarNip() }
    }

    //MD FUNCION QUE EJECUTA UNA ACTCION DE ACUERDO ALA TECLA PRECIONADA
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> {//AL PRECIONAR ENTER INICIA SESION
                iniciarSesion()
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }

    private fun recuperarNip() {
        val inicio = Intent(this, MainActivity::class.java)
        inicio.putExtra("recuperarNip",true)
        startActivity(inicio)
        finish()
    }

    private fun iniciarSesion() {
        if (ValGlobales.validarConexion(this)) {
            val idCliente: String = findViewById<EditText>(R.id.txtNoCliente).text.toString()
            val nip: String = findViewById<EditText>(R.id.txtNip).text.toString()
            var respuesta = ""
            var continua = false
            val regIdCliente = Regex(getString(R.string.rexUSUARIO))
            val regNip = Regex(getString(R.string.rexNIP))

            if (idCliente.isNotEmpty() && nip.isNotEmpty()) {
                if (!regIdCliente.containsMatchIn(idCliente)) {
                    respuesta =
                        "El Usuario $idCliente no cumple con el formato requerido, favor de validarlo."
                } else {
                    if (nip.length == 4) {
                        if (regNip.containsMatchIn(nip) && regIdCliente.containsMatchIn(idCliente)) {
                            continua = true
                        } else {
                            respuesta =
                                "El NIP $nip no cumple con el formato requerido, favor de validarlo."
                        }
                    } else {
                        respuesta = "El NIP debe capturarse a 4 dígitos, favor de validarlo."
                    }
                }
            } else {
                respuesta = "Ambos datos son requeridos"
            }

            if (continua) {
                loginWS(idCliente, nip)
            } else {
                FuncionesGlobales.mostrarAlert(this,"error",true,"Iniciar Sesión",respuesta,false).show()
            }
        }
    }
    fun loginWS(idCliente: String, nip: String) {
        LoadingScreen.displayLoadingWithText(this,"Validando información...",false)
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alerError = FuncionesGlobales.mostrarAlert(this,"error",true,"Iniciar Sesión",getString(R.string.error),false)

        //Se obtiene de las variables de sesion el token de firebase
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString(getString(R.string.token), "")

        Log.d("Token","Token firebase: $token")

        //Se encripta el nip para enviar al WS
        val nipEncriptado = GeneralUtils.encriptacion(this, nip, idCliente, 1)!!

        Log.d("Token","Nip encriptado: $nipEncriptado $nip")

        var json = ""
        val valSolicitud = "{" +
                " \"customer_id\" :$idCliente," +
                " \"nip\" : \"$nipEncriptado\","
        val valDevice = "\"device\" : { " +
                " \"token\" :\"$token\" }"
        json = "$valSolicitud $valDevice }"
        val jsonParametros = JSONObject(json)

        val request = object : JsonObjectRequest(
            Method.POST,
            getString(R.string.urlLogin),
            jsonParametros,
            Response.Listener { response ->
                try {
                    //Obtiene su respuesta json
                    val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject
                    if (jsonData.getInt("code") == 200)//si la peticion fue correcta se continua con el login
                    {
                        // Toast.makeText(this, "Respuesta: ${response.getString("data")}", Toast.LENGTH_SHORT).show()
                        val jsonResults =
                            JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                        //se guarda en sesion el numero de prestamo
                        FuncionesGlobales.guardarVariableSesion(this,"Int","CREDITO_ID",jsonResults.getString("credit_id"))
                        FuncionesGlobales.guardarVariableSesion(this,"String","NOMBRE_GPO",jsonResults.getString("group_name"))
                        FuncionesGlobales.guardarVariableSesion(this,"String","PRESIDENTA",jsonResults.getString("customer_name"))
                        FuncionesGlobales.guardarVariableSesion(this,"String","ID_PRESIDENTA",idCliente)
                        /*Si el logueo es exitoso se trata de obtener el token para envio de notificacion de Firebase
                        y este se registra/actualiza en nuestro servidor junto con el numero imei*/
                        //GeneralUtils.obtenerTokenNotificaciones(this, idCliente, numeroCelular)
                        //findViewById<TextView>(R.id.textView).text = response.getString("data")

                        //Si el login es correcto se inscribe a los temas de las notificaciones de firebase
                        //y se crean los canales para las notificaciones
                        creacionComplementosApp(this)

                        val inicio = Intent(this, Navegacion::class.java)
                        startActivity(inicio)
                        finish()
                        LoadingScreen.hideLoading()
                    } else {
                        alerError.show()
                        LoadingScreen.hideLoading()
                    }
                } catch (e: Exception) {
                    LoadingScreen.hideLoading()
                    alerError.show()
                }
            },
            Response.ErrorListener { error ->
                val responseError = String(error.networkResponse.data)
                val dataError = JSONObject(responseError)
                try {
                    val jsonData = JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                    val jResul = JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                    val codigo = error.networkResponse.statusCode
                    if(codigo == 422){
                      //  alerError.setMessage("Credenciales invalidas. Favor de verificarlo")
                        alerError.setMessage(jResul.toString() + " parametros "+jsonParametros)
                    }
                    alerError.setMessage(jResul.toString() + " parametros "+jsonParametros)
                }catch (e: Exception){ }
                alerError.show()
                LoadingScreen.hideLoading()
            })
        {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = getString(R.string.content_type)
                headers["X-Header-Email"] = getString(R.string.header_email)
                headers["X-Header-Password"] = getString(R.string.header_password)
                headers["X-Header-Api-Key"] = getString(R.string.header_api_key)
                return headers
            }
        }
        try {
            val queue = Volley.newRequestQueue(this)
            //primero borramos el cache y enviamos despues la peticion
            queue.cache.clear()
            queue.add(request)
        }catch(e: Exception){
            alerError.show()
        }
        /*******  FIN ENVIO   *******/
    }
    private fun iniciarSesionWS(idCliente: String, nip: String) {
        LoadingScreen.displayLoadingWithText(this,"Validando información...",false)
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alerError = FuncionesGlobales.mostrarAlert(this,"error",true,"Iniciar Sesión",getString(R.string.error),false)

        val jsonParametros = JSONObject()
        jsonParametros.put("customer_id", "164492")
        jsonParametros.put("cell_phone", "7752344223")/*
        jsonParametros.put("customer_id", "171199")
        jsonParametros.put("cell_phone", "2461072260")*/

        val request = object : JsonObjectRequest(
            Method.POST,
            getString(R.string.urlLogin),
            jsonParametros,
            Response.Listener { response ->
                try {
                    //Obtiene su respuesta json
                    val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject
                    if (jsonData.getInt("code") == 200)//si la peticion fue correcta se continua con el login
                    {
                        // Toast.makeText(this, "Respuesta: ${response.getString("data")}", Toast.LENGTH_SHORT).show()
                        val jsonResults =
                            JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                        //se guarda en sesion el numero de prestamo
                        FuncionesGlobales.guardarVariableSesion(this,"Int","CREDITO_ID",jsonResults.getString("credit_id"))

                        val inicio = Intent(this, Navegacion::class.java)
                        startActivity(inicio)
                        finish()
                        LoadingScreen.hideLoading()
                    } else {
                        alerError.show()
                        LoadingScreen.hideLoading()
                    }
                } catch (e: Exception) {
                    LoadingScreen.hideLoading()
                    alerError.show()
                }
            },
            Response.ErrorListener { error ->
                val responseError = String(error.networkResponse.data)
                val dataError = JSONObject(responseError)
                try {
                    val jsonData = JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                    var mensaje: String
                    val jResul = JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                    val esCell_phone = jsonData.getString("results").contains("cell_phone")
                    val esCustomer_id = jsonData.getString("results").contains("customer_id")

                    if(esCell_phone){
                        mensaje = jResul.getString("cell_phone")
                    } else if(esCustomer_id){
                        mensaje = jResul.getString("customer_id")
                    }else{
                        mensaje = getString(R.string.error)
                    }
                    alerError.setMessage(mensaje)
                }catch (e: Exception){ }
                alerError.show()
                LoadingScreen.hideLoading()
            })
        {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = getString(R.string.content_type)
                headers["X-Header-Email"] = getString(R.string.header_email)
                headers["X-Header-Password"] = getString(R.string.header_password)
                headers["X-Header-Api-Key"] = getString(R.string.header_api_key)
                return headers
            }
        }
        try {
            val queue = Volley.newRequestQueue(this)
            //primero borramos el cache y enviamos despues la peticion
            queue.cache.clear()
            queue.add(request)
        }catch(e: Exception){
            alerError.show()
        }
        /*******  FIN ENVIO   *******/
    }
    private fun iniciarSesionWSO(idCliente: String, nip: String) {
        //SE ENVIAN LOS DATOS PARA SU VALIDACION
        //SI SON VALIDOS SE INICIA SESION
        val inicio = Intent(this, Navegacion::class.java)
        startActivity(inicio)
        finish()
        LoadingScreen.displayLoadingWithText(this,"Validando información...",false)
        /*   /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/

           val dialogNo = AlertDialog.Builder(this, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
               .setTitle(Html.fromHtml("<font color='#3C8943'>Ingresar</font>"))
               .setMessage("OCURRIO UN ERROR, FAVOR DE INTENTARLO MAS TARDE.")
               .setPositiveButton("Aceptar") { dialog, which ->
                   dialog.cancel()
               }
           val jsonParametros = JSONObject()
           jsonParametros.put("customer_id", idCliente)
           jsonParametros.put("cell_phone", numeroCelular)
           //jsonParametros.put("customer_id", "53557")
           //.put("cell_phone", "9631350808")

           val request = object : JsonObjectRequest(
               Method.POST,
               //getString(R.string.urlApi) + getString(R.string.metodoLogin),
               getString(R.string.urlLogin),
               jsonParametros,
               Response.Listener { response ->
                   try {
                       //Obtiene su respuesta json
                       //Toast.makeText(this, "Respuesta: $response", Toast.LENGTH_SHORT).show()
                       val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject
                       findViewById<TextView>(R.id.txtPruebas).text = jsonData.getString("message")
                       if (jsonData.getInt("code") == 200)//si la peticion fue correcta se continua con el login
                       {
                           val jsonResults =
                               JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                           //findViewById<TextView>(R.id.txtPruebas).text = jsonResults.getString("credit_id") +"\n"+response.getString("data")
                           //se guarda en sesion el numero de prestamo
                           val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                           val editor = prefs.edit()
                           editor.putInt("CREDITO_ID", jsonResults.getInt("credit_id"))
                           editor.apply()
                           //Toast.makeText(this, "INICIA SESION", Toast.LENGTH_SHORT).show()

                           /*Si el logueo es exitoso se trata de obtener el token para envio de notificacion de Firebase
                           y este se registra/actualiza en nuestro servidor junto con el numero imei*/
                           GeneralUtils.obtenerTokenNotificaciones(this, idCliente, numeroCelular)

                           val home = Intent(this, Navegacion::class.java)
                           startActivity(home)
                           finish()
                       } else {
                           dialogNo.show()
                           LoadingScreen.hideLoading()
                       }
                       LoadingScreen.hideLoading()
                   } catch (e: Exception) {
                       LoadingScreen.hideLoading()
                       dialogNo.show()
                   }
               },
               Response.ErrorListener { error ->
                   //val errorD = VolleyError(String(error.networkResponse.data))
                   val responseError = String(error.networkResponse.data)
                   val dataError = JSONObject(responseError)
                   try {
                       val jsonData = JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                       val code = jsonData.getInt("code")
                       val message = jsonData.getString("message")
                       var mensaje = getString(R.string.error)
                       val jResul = JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                       val esCell_phone = jsonData.getString("results").contains("cell_phone")
                       val esCustomer_id = jsonData.getString("results").contains("customer_id")

                       if(esCell_phone){
                           mensaje = jResul.getString("cell_phone")
                       } else if(esCustomer_id){
                           mensaje = jResul.getString("customer_id")
                       }else{
                           mensaje = message
                       }
                       //findViewById<TextView>(R.id.txtPruebas).text = mensaje
                       dialogNo.setMessage(mensaje)
                   }catch (e: Exception){
                       //findViewById<TextView>(R.id.txtPruebas).text = e.toString()
                       dialogNo.setMessage(getString(R.string.error))
                   }
                   dialogNo.show()
                   LoadingScreen.hideLoading()
               })
           {
               override fun getHeaders(): Map<String, String> {
                   val headers = HashMap<String, String>()
                   headers["Content-Type"] = getString(R.string.content_type)
                   headers["X-Header-Email"] = getString(R.string.header_email)
                   headers["X-Header-Password"] = getString(R.string.header_password)
                   headers["X-Header-Api-Key"] = getString(R.string.header_api_key)
                   return headers
               }
           }
           try {
               val queue = Volley.newRequestQueue(this)
               //primero borramos el cache y enviamos despues la peticion
               queue.cache.clear()
               queue.add(request)
           }catch(e: Exception){
               //dialogNo.setMessage("Ocurrio un error ${e.message}")
               dialogNo.setMessage(getString(R.string.error))
               dialogNo.show()
           }
           /*******  FIN ENVIO   *******/
           */
    }

    private fun realizarRegistro() {
        val registro = Intent(this, MainActivity::class.java)
        registro.putExtra("recuperarNip",false)
        startActivity(registro)
        finish()
    }
}