package com.example.apppresidenta

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.FuncionesGlobales.Companion.creacionComplementosApp
import com.example.apppresidenta.generales.FuncionesGlobales.Companion.encriptacion
import com.example.apppresidenta.generales.LoadingScreen
import com.example.apppresidenta.generales.ValGlobales
import com.example.apppresidenta.navegacion.Navegacion
import org.json.JSONObject
import org.json.JSONTokener

class NIPActivity : AppCompatActivity() {
    var recuperarNip: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nip_activity)
        //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "MainActivity")
        /*MD SE AGREGA LOGO Y TITULO DEL LA ACTIVIDAD*/
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)//flecha atras<
        supportActionBar?.title = HtmlCompat.fromHtml("<font color='#2C3B62'>" + getString(R.string.crea_nip) + "</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_bc6))

        textChangueNip()

        val parametros = this.intent.extras
        recuperarNip = parametros!!.getBoolean("recuperarNip", false)
        findViewById<Button>(R.id.btnConfirmarNip).setOnClickListener { confirmarNIP() }

    }

    //MD FUNCION QUE EJECUTA UNA ACTCION DE ACUERDO ALA TECLA PRECIONADA
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> {
                confirmarNIP()
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }
    override fun onBackPressed() {
        FuncionesGlobales.mostrarAlert(this,"advertencia",true,"Registro","Para continuar debe de terminar el proceso.",false).show()
    }

    fun confirmarNIP(){
        //OBTIENE EL NIP
        val nip = findViewById<EditText>(R.id.nip1).text.toString() +
                findViewById<EditText>(R.id.nip2).text.toString() +
                findViewById<EditText>(R.id.nip3).text.toString() +
                findViewById<EditText>(R.id.nip4).text.toString()

        //OBTIENE EL NIP DE CONFIRMACION
        val nipC = findViewById<EditText>(R.id.nipC1).text.toString() +
                findViewById<EditText>(R.id.nipC2).text.toString() +
                findViewById<EditText>(R.id.nipC3).text.toString() +
                findViewById<EditText>(R.id.nipC4).text.toString()

        if (nip.length == 4 && nipC.length == 4) {
            //SON IGUALES
            findViewById<TextView>(R.id.textView9).text = "nip $nip nipC $nipC"
            if (nip == nipC) {
                if (ValGlobales.validarConexion(this)) {
                    //MD SI ES EL REGISTRO NORMAL CONTINUA CON REGISTRAR NIP DE LO CONTRARIO CON LA ACTUALIZACION DEL NIP
                    if (!recuperarNip) {
                        registrarNip(nip)
                    } else {
                        recuperarNip(nip)
                    }
                }
            } else {
                findViewById<EditText>(R.id.nip1).setText("")
                findViewById<EditText>(R.id.nip3).setText("")
                findViewById<EditText>(R.id.nip4).setText("")
                findViewById<EditText>(R.id.nip2).setText("")
                findViewById<EditText>(R.id.nipC1).setText("")
                findViewById<EditText>(R.id.nipC3).setText("")
                findViewById<EditText>(R.id.nipC4).setText("")
                findViewById<EditText>(R.id.nipC2).setText("")
                findViewById<EditText>(R.id.nip1).requestFocus()
                FuncionesGlobales.mostrarAlert(
                    this,
                    "advertencia",
                    true,
                    "Registro NIP",
                    "El NIP y la Confirmación deben de ser iguales.",
                    false
                ).show()
            }
        } else {
            FuncionesGlobales.mostrarAlert(
                this,
                "error",
                true,
                "Registro NIP",
                "El NIP debe de ser a 4 dígitos.",
                false
            ).show()
        }
    }
    private fun recuperarNip(nip: String) {
        LoadingScreen.displayLoadingWithText(this, "Registrando información...", false)

        //Se obtiene de las variables de sesion el token de firebase
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString(getString(R.string.token), "")

        Log.d("Token","Token firebase: $token")

        val parametros = this.intent.extras
        val idCliente = parametros!!.getString("idCliente", "0")

        //Se encripta el nip para enviar al WS
        val nipEncriptado = encriptacion(this,nip,idCliente,1)!!

        Log.d("Token","Nip encriptado: $nipEncriptado $nip")

        var json = ""
        val valSolicitud = "{" +
                " \"customer_id\" :$idCliente," +
                " \"nip\" : \"$nipEncriptado\","
        val valDevice = "\"device\" : { " +
                " \"token\" :\"$token\"}"
        json = "$valSolicitud $valDevice }"
        val jsonParametros = JSONObject(json)

        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alerError = FuncionesGlobales.mostrarAlert(
            this,
            "error",
            true,
            "Registro",
            getString(R.string.error),
            false
        )

        val request = object : JsonObjectRequest(
            Method.POST,
            getString(R.string.urlActualizarNip),
            jsonParametros,
            Response.Listener { response ->
                try {
                    //Obtiene su respuesta json
                    //Toast.makeText(this, "Respuesta: $response", Toast.LENGTH_SHORT).show()
                    val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject
                    findViewById<TextView>(R.id.textView9).text = response.toString()
                    if (jsonData.getInt("code") == 200)//la peticion fue correcta
                    {
                        val alertCorrecto = FuncionesGlobales.mostrarAlert(
                            this,
                            "correcto",
                            true,
                            "Cambiar NIP",
                            "Su NIP ha sido actualizado correctamente.",
                            false
                        )
                        alertCorrecto.setPositiveButton("Aceptar") { _, _ ->
                            //SE REALIZA LA PETICION DEL LOGIN
                            login(idCliente,nip,false,jsonParametros)
                        }
                        alertCorrecto.setNegativeButton("Cancelar") { dialog, _ ->
                            dialog.cancel()
                        }
                        alertCorrecto.create()
                        alertCorrecto.show()

                    } else {
                        alerError.show()
                        LoadingScreen.hideLoading()
                    }
                    LoadingScreen.hideLoading()
                } catch (e: Exception) {
                    //alerError.setMessage("Excepcion "+e.message)
                    LoadingScreen.hideLoading()
                    alerError.show()
                }
            },
            Response.ErrorListener { error ->
                try {
                    val codigo = error.networkResponse.statusCode
                    var mensaje = "Error: $codigo \n${getString(R.string.error)}"
                    if(codigo == 422){
                        mensaje = "El NIP ingresado no puede ser igual al NIP registrado, ingresa uno diferente."
                    }
                    alerError.setMessage(mensaje)
                } catch (e: Exception) {
                    alerError.setMessage(getString(R.string.error))
                }
                alerError.show()
                LoadingScreen.hideLoading()
            }) {
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
        } catch (e: Exception) {
            //dialogNo.setMessage("Ocurrio un error ${e.message}")
            alerError.show()
        }
        /*******  FIN ENVIO   *******/
        //findViewById<TextView>(R.id.textView).text = jsonParametros.toString()
    }

    private fun login(idCliente: String,nip: String,esRegistro: Boolean, json: JSONObject) {
        LoadingScreen.displayLoadingWithText(this,"Validando información...",false)

        //Se obtiene de las variables de sesion el token de firebase
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString(getString(R.string.token), "")

        Log.d("Token","Token firebase: $token")

        //Se encripta el nip para enviar al WS
        val nipEncriptado = encriptacion(this,nip,idCliente,1)!!

        Log.d("Token","Nip encriptado: $nipEncriptado $nip")

        var jsonParametros = json
        //SI ES REGISTRO SE GENERA EL JSON CON LOS PARAMETRO DE LO CONTRARIO SE USAN LOS QUE LLEGAN
        if(esRegistro){
            var json1 = ""
            val valSolicitud = "{" +
                    " \"customer_id\" :$idCliente," +
                    " \"nip\" : \"$nipEncriptado\","
            val valDevice = "\"device\" : { " +
                    " \"token\" :\"$token\"}"
            json1 = "$valSolicitud $valDevice }"
            jsonParametros = JSONObject(json1)
        }

        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alerError = FuncionesGlobales.mostrarAlert(this,"error",true,"Iniciar Sesión",getString(R.string.error),false)
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
                try {
                    val codigo = error.networkResponse.statusCode
                    if(codigo == 422){
                        alerError.setMessage("Credenciales invalidas. Favor de verificarlo")
                    }
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


    //MD ONCHANGUE DE INPUTS DE NIP PARA QUE AL PONNER UN DIGITO SE PASE AL SIGUIENTE INPUT
    fun textChangueNip() {
        findViewById<EditText>(R.id.nip1).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nip2).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nip1).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nip2).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nip3).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nip1).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nip2).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nip3).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nip4).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nip2).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nip3).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nip4).doAfterTextChanged {

            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nipC1).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nip3).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nip4).requestFocus()
                }
            }
        }

        //INPUTS DE LA CONFIRMACION
        findViewById<EditText>(R.id.nipC1).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nipC2).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nipC1).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nipC2).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nipC3).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nipC1).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nipC2).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nipC3).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nipC4).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nipC2).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nipC3).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nipC4).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<Button>(R.id.btnConfirmarNip).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nipC3).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nipC4).requestFocus()
                }
            }
        }
    }
    private fun registrarNip(nip: String) {
        //SE ENVIAN DATO Y SI ES CORRECTO ENVIA A LOGIN
       /* var alertContinuar = FuncionesGlobales.mostrarAlert(this,"correcto",true,"Registro","Registro exitoso, favor de iniciar sesión para continuar.",true)
        alertContinuar.setPositiveButton("Aceptar") { dialog, which ->
            val home = Intent(this, LoginActivity::class.java)
            startActivity(home)
            finish()
        }*/
        LoadingScreen.displayLoadingWithText(this, "Registrando información...", false)

        //Se obtiene de las variables de sesion el token de firebase
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString(getString(R.string.token), "")

        Log.d("Token","Token firebase: $token")

        val parametros = this.intent.extras
        val idCliente = parametros!!.getString("idCliente", "0")

        //Se encripta el nip para enviar al WS
        val nipEncriptado = encriptacion(this,nip,idCliente,1)!!

        Log.d("Token","Nip encriptado: $nipEncriptado $nip")

        var json = ""
        val valSolicitud = "{" +
                " \"app_id\" :1," +
                " \"customer_id\" :$idCliente," +
                " \"nip\" : \"$nipEncriptado\"," +
                " \"sms_code\" :null," +
                " \"status\" : 1," +
                " \"group_id\" :null,"
        val valDevice = "\"device\" : { " +
                " \"token\" :\"$token\" ,"+
                " \"model\" :null,"+
                " \"brand\" : null }"
        json = "$valSolicitud $valDevice }"
        val jsonParametros = JSONObject(json)

        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alerError = FuncionesGlobales.mostrarAlert(
            this,
            "error",
            true,
            "Registro",
            getString(R.string.error),
            false
        )

        val request = object : JsonObjectRequest(
            Method.POST,
            getString(R.string.urlRegistroCliente),
            jsonParametros,
            Response.Listener { response ->
                try {
                    //Obtiene su respuesta json
                    //Toast.makeText(this, "Respuesta: $response", Toast.LENGTH_SHORT).show()
                    findViewById<TextView>(R.id.textView9).text =
                        "$response $$response"
                    val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject

                    if (jsonData.getInt("code") == 201)//si la peticion fue correcta se continua con el login
                    {
                        login(idCliente,nip,true,jsonParametros)
                    } else {
                        alerError.show()
                        LoadingScreen.hideLoading()
                    }
                    LoadingScreen.hideLoading()
                } catch (e: Exception) {
                    findViewById<TextView>(R.id.textView9).text =
                        "excepcion ${e.message}"
                    LoadingScreen.hideLoading()
                    alerError.show()
                }
            },
            Response.ErrorListener { error ->
                //val errorD = VolleyError(String(error.networkResponse.data))

                try {
                    val responseError = String(error.networkResponse.data)
                    val dataError = JSONObject(responseError)
                    val jsonData =
                        JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                    val code = jsonData.getInt("code")
                    val message = jsonData.getString("message")
                    findViewById<TextView>(R.id.textView9).text =
                        "parametros $jsonParametros $responseError"
                    alerError.setMessage(getString(R.string.error))
                } catch (e: Exception) {
                    //findViewById<TextView>(R.id.txtPruebas).text = e.toString()

                }
                alerError.show()
                LoadingScreen.hideLoading()
            }) {
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
        } catch (e: Exception) {
            //dialogNo.setMessage("Ocurrio un error ${e.message}")
            alerError.show()
        }
        /*******  FIN ENVIO   *******/
        //findViewById<TextView>(R.id.textView).text = jsonParametros.toString()
    }
}