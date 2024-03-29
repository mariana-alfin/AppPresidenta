package mx.com.presidentasalfin

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import mx.com.presidentasalfin.generales.FuncionesGlobales
import mx.com.presidentasalfin.generales.LoadingScreen
import mx.com.presidentasalfin.generales.ValGlobales
import mx.com.presidentasalfin.navegacion.Navegacion
import org.json.JSONObject
import org.json.JSONTokener


class MainActivity : AppCompatActivity() {
    var recuperarNip: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        setContentView(R.layout.main_activity)
        //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "MainActivity")
        supportActionBar?.hide()
        findViewById<TextView>(R.id.txtUsuario).requestFocus()
        val parametros = this.intent.extras
        recuperarNip = parametros!!.getBoolean("recuperarNip", false)
        //Toast.makeText(this, "SE RECUPERARA NIP $recuperarNip", Toast.LENGTH_SHORT).show()
        if (recuperarNip) {
            //Toast.makeText(this, "SE RECUPERARA NIP", Toast.LENGTH_SHORT).show()
            findViewById<Button>(R.id.btnLogin).text = "RECUPERAR NIP"
            findViewById<Button>(R.id.btnLogin).setOnClickListener { validarFormulario(recuperarNip) }
        } else {
            findViewById<Button>(R.id.btnLogin).setOnClickListener { validarFormulario(false) }
        }
        /*Se agrega el hipervinculo*/
        val textView = findViewById<View>(R.id.textView6) as TextView
        textView.isClickable = true
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    //MD FUNCION QUE EJECUTA UNA ACTCION DE ACUERDO ALA TECLA PRECIONADA
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> {//AL PRECIONAR ENTER VALIDA EL FORMULARIO DE REGISTRO
                validarFormulario(recuperarNip)
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }

    override fun onBackPressed() {
        val home = Intent(this, LoginActivity::class.java)
        startActivity(home)
        finish()
    }

    private fun validarFormulario(esRecuperarNIP: Boolean) {
        if (ValGlobales.validarConexion(this)) {

            val idCliente: String = findViewById<EditText>(R.id.txtUsuario).text.toString()
            val numeroCelular: String = findViewById<EditText>(R.id.txtCelular).text.toString()

            var respuesta = ""
            var continua = false
            val regIdCliente = Regex(getString(R.string.rexUSUARIO))
            val regCel = Regex(getString(R.string.rexCELULAR))

            if (idCliente.isNotEmpty() && numeroCelular.isNotEmpty()) {
                if (!regIdCliente.containsMatchIn(idCliente)) {
                    respuesta =
                        "El Usuario $idCliente no cumple con el formato requerido, favor de validarlo."
                } else {
                    if (numeroCelular.length == 10) {
                        if (regCel.containsMatchIn(numeroCelular) && regIdCliente.containsMatchIn(
                                idCliente
                            )
                        ) {
                            continua = true
                        } else {
                            respuesta =
                                "El CELULAR $numeroCelular no cumple con el formato requerido, favor de validarlo."
                        }
                    } else {
                        respuesta = "El CELULAR debe capturarse a 10 dígitos, favor de validarlo."
                    }
                }
            } else {
                respuesta = "Ambos datos son requeridos"
            }

            if (continua) {
                if (esRecuperarNIP) { //ES RECUPERACION DE NIP
                    recuperarNip(idCliente, numeroCelular)
                } else {//ES REGISTRO
                    registroClienteWS(idCliente, numeroCelular)
                }
            } else {
                FuncionesGlobales.mostrarAlert(this, "error", true, "Registro", respuesta, false)
                    .show()
            }
        }
    }

    private fun registroClienteWS(idCliente: String, numeroCelular: String) {
        LoadingScreen.displayLoadingWithText(this, "Registrando información...", false)
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alerError = FuncionesGlobales.mostrarAlert(
            this,
            "error",
            true,
            "Registro Cliente",
            getString(R.string.error),
            false
        )
        val jsonParametros = JSONObject()
        jsonParametros.put("customer_id", idCliente)
        jsonParametros.put("cell_phone", numeroCelular)

        val request = object : JsonObjectRequest(
            Method.POST,
            //getString(R.string.urlValicionLogin),
            getString(R.string.url)+getString(R.string.metValicionLogin),
            jsonParametros,
            Response.Listener { response ->
                try {
                    //Obtiene su respuesta json
                    //Toast.makeText(this, "Respuesta: $response", Toast.LENGTH_SHORT).show()
                    val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject
                    //findViewById<TextView>(R.id.txtPruebas).text = jsonData.getString("message")
                    if (jsonData.getInt("code") == 200)//si la peticion fue correcta indica que ya esta registrado por lo tanto no podemos continuar
                    {
                        val alert = FuncionesGlobales.mostrarAlert(
                            this,
                            "advertencia",
                            true,
                            "Registro",
                            "El Cliente ya se encuentra registrado, debe de iniciar sesión para continuar.",
                            true
                        )
                        alert.setPositiveButton("Aceptar") { _, _ ->
                            //SE REALIZA LA PETICION DEL LOGIN
                            val inicio = Intent(this, LoginActivity::class.java)
                            startActivity(inicio)
                            finish()
                        }
                        alert.setNegativeButton("Cancelar") { dialog, _ ->
                            dialog.cancel()
                        }
                        alert.create()
                        alert.show()

                    } else {
                        alerError.show()
                        LoadingScreen.hideLoading()
                    }
                    LoadingScreen.hideLoading()
                } catch (e: Exception) {
                    LoadingScreen.hideLoading()
                    alerError.show()
                }
            },
            Response.ErrorListener { error ->
                //val errorD = VolleyError(String(error.networkResponse.data))
                val responseError = String(error.networkResponse.data)
                var mensaje = getString(R.string.error)
                try {
                    val dataError = JSONObject(responseError)
                    val jsonData =
                        JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                    val code = jsonData.getInt("code")
                    val message = jsonData.getString("message")
                    mensaje = getString(R.string.error)
                    val jResul =
                        JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                    val esCell_phone = jsonData.getString("results").contains("cell_phone")
                    val esCustomer_id = jsonData.getString("results").contains("customer_id")

                    if (esCell_phone) {
                        mensaje = jResul.getString("cell_phone")
                    } else if (esCustomer_id) {
                        mensaje = jResul.getString("customer_id")
                    } else {
                        mensaje = message
                    }
                    //SI ES REGISTRO NO SE MUESTRA EL ALERT Y SE REALIZA EL REGISTRO DE LO CONTRARIO SI ES RECUPERACION DE NIP SI SE ENVIA ALA RECUERPACION DE NIP
                    if (mensaje == "El ID del cliente no esta registrado.") {
                        //<- ES REGISTRO
                        enviarARegistro(numeroCelular, idCliente, false)
                    } else {
                        alerError.setMessage(mensaje)
                        alerError.show()
                    }

                } catch (e: Exception) {
                    //findViewById<TextView>(R.id.txtPruebas).text = e.toString()
                    val codigo = error.networkResponse.statusCode
                    mensaje = "Error: $codigo \n${getString(R.string.errorServidor)}"
                    alerError.setMessage(mensaje)
                }
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
    }

    private fun recuperarNip(idCliente: String, numeroCelular: String) {
        LoadingScreen.displayLoadingWithText(this, "Validando información...", false)

        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alerError = FuncionesGlobales.mostrarAlert(
            this,
            "error",
            true,
            "Recuperar NIP",
            getString(R.string.error),
            false
        )
        val jsonParametros = JSONObject()
        jsonParametros.put("customer_id", idCliente)
        jsonParametros.put("cell_phone", numeroCelular)

        val request = object : JsonObjectRequest(
            Method.POST,
            //getString(R.string.urlLogin),
            getString(R.string.url)+getString(R.string.metValicionLogin),
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
                        //se guarda en sesion el numero de prestamo
                        FuncionesGlobales.guardarVariableSesion(this,
                            "Int",
                            "CREDITO_ID",
                            jsonResults.getString("credit_id"))
                        //Toast.makeText(this, "INICIA SESION", Toast.LENGTH_SHORT).show()

                        /*SI LA PETICION RETORNA UN SUCCESS YA ESTA REGISTRADO POR LO TANTO PODEMOS CONTINUAR CON LA RECUPERACION DEL NIP*/
                        enviarARegistro(numeroCelular, idCliente, true)
                        LoadingScreen.hideLoading()
                    } else {
                        alerError.show()
                        LoadingScreen.hideLoading()
                    }
                    LoadingScreen.hideLoading()
                } catch (e: Exception) {
                    LoadingScreen.hideLoading()
                    alerError.show()
                }
            },
            Response.ErrorListener { error ->
                //val errorD = VolleyError(String(error.networkResponse.data))

                /*  try {
                      val responseError = String(error.networkResponse.data)
                      val dataError = JSONObject(responseError)
                      val jsonData =
                          JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                      val code = jsonData.getInt("code")
                      val message = jsonData.getString("message")
                      var mensaje = getString(R.string.error)
                      val jResul =
                          JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                      val esCell_phone = jsonData.getString("results").contains("cell_phone")
                      val esCustomer_id = jsonData.getString("results").contains("customer_id")

                      if (esCell_phone) {
                          mensaje = jResul.getString("cell_phone")
                      } else if (esCustomer_id) {
                          mensaje = jResul.getString("customer_id")
                      } else {
                          mensaje = message
                      }
                          alerError.setMessage(mensaje)
                          alerError.show()

                      findViewById<TextView>(R.id.txtPruebas).text = mensaje

                  } catch (e: Exception) {
                      //findViewById<TextView>(R.id.txtPruebas).text = e.toString()
                  }
                  LoadingScreen.hideLoading()*/
                var mensaje = getString(R.string.error)
                try {
                    val responseError = String(error.networkResponse.data)
                    val dataError = JSONObject(responseError)
                    val jsonData =
                        JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                    val code = jsonData.getInt("code")
                    val message = jsonData.getString("message")
                    var mensaje = getString(R.string.error)
                    val jResul =
                        JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                    val esCell_phone = jsonData.getString("results").contains("cell_phone")
                    val esCustomer_id = jsonData.getString("results").contains("customer_id")

                    if (esCell_phone) {
                        mensaje = jResul.getString("cell_phone")
                    } else if (esCustomer_id) {
                        mensaje = jResul.getString("customer_id")
                    } else {
                        mensaje = message
                    }
                    alerError.setMessage(mensaje)
                    alerError.show()
                } catch (e: Exception) {
                    try {
                        val codigo = error.networkResponse.statusCode
                        mensaje = "Error: $codigo \n${getString(R.string.errorServidor)}"
                        alerError.setMessage(mensaje)
                        alerError.show()
                    } catch (e: Exception) {
                        alerError.setMessage(getString(R.string.errorServidor))
                        alerError.show()
                    }
                    LoadingScreen.hideLoading()
                }
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
    }

    private fun enviarARegistro(numeroCelular: String, idCliente: String, recuperarNip: Boolean) {
        val registro = Intent(this, RegistroActivity::class.java)
        registro.putExtra("celular", numeroCelular)
        registro.putExtra("idCliente", idCliente)
        registro.putExtra("recuperarNip", recuperarNip)
        startActivity(registro)
        finish()
    }


    private fun iniciarSesionWS(idCliente: String, numeroCelular: String) {
        LoadingScreen.displayLoadingWithText(this, "Validando información...", false)
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alerError = FuncionesGlobales.mostrarAlert(
            this,
            "error",
            true,
            "Registro",
            getString(R.string.error),
            false
        ).show()

        val jsonParametros = JSONObject()
        jsonParametros.put("customer_id", idCliente)
        jsonParametros.put("cell_phone", numeroCelular)
        //jsonParametros.put("customer_id", "53557")
        //.put("cell_phone", "9631350808")

        val request = object : JsonObjectRequest(
            Method.POST,
            //getString(R.string.urlApi) + getString(R.string.metodoLogin),
            //getString(R.string.urlLogin),
            getString(R.string.url)+getString(R.string.metLogin),
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
                        FuncionesGlobales.guardarVariableSesion(this,
                            "Int",
                            "CREDITO_ID",
                            jsonResults.getString("credit_id"))
                        //Toast.makeText(this, "INICIA SESION", Toast.LENGTH_SHORT).show()

                        /*Si el logueo es exitoso se trata de obtener el token para envio de notificacion de Firebase
                        y este se registra/actualiza en nuestro servidor junto con el numero imei*/
                        //obtenerTokenNotificaciones(this, idCliente, numeroCelular)

                        val home = Intent(this, Navegacion::class.java)
                        startActivity(home)
                        finish()
                    } else {
                        alerError.show()
                        LoadingScreen.hideLoading()
                    }
                    LoadingScreen.hideLoading()
                } catch (e: Exception) {
                    LoadingScreen.hideLoading()
                    alerError.show()
                }
            },
            Response.ErrorListener { error ->
                //val errorD = VolleyError(String(error.networkResponse.data))
                val responseError = String(error.networkResponse.data)
                val dataError = JSONObject(responseError)
                try {
                    val jsonData =
                        JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                    val code = jsonData.getInt("code")
                    val message = jsonData.getString("message")
                    var mensaje = getString(R.string.error)
                    val jResul =
                        JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                    val esCell_phone = jsonData.getString("results").contains("cell_phone")
                    val esCustomer_id = jsonData.getString("results").contains("customer_id")

                    if (esCell_phone) {
                        mensaje = jResul.getString("cell_phone")
                    } else if (esCustomer_id) {
                        mensaje = jResul.getString("customer_id")
                    } else {
                        mensaje = message
                    }
                    //findViewById<TextView>(R.id.txtPruebas).text = mensaje
                    alerError.setMessage(mensaje)
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
    }

    fun solicitudWs1() {
        val URL = "https://api.sample.com/Api/user_id"
        val json = JSONObject()
        json.put("user_id", "1")
        json.put("code", "AB")

        val jsonOblect = object : JsonObjectRequest(
            Method.POST,
            URL,
            json,
            Response.Listener { response ->
                // Get your json response and convert it to whatever you want.
                Toast.makeText(this, "You Clicked: $response", Toast.LENGTH_SHORT).show()
                findViewById<TextView>(R.id.txtPruebas).text = "Respuesta: $response"
            },
            Response.ErrorListener {
                Toast.makeText(this, "Error $it .message", Toast.LENGTH_SHORT).show()
                findViewById<TextView>(R.id.txtPruebas).text = "Error $it"
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Content-Type", "application/json")
                return headers
            }
        }

        val queue = Volley.newRequestQueue(this)
        queue.add(jsonOblect)
    }

    fun solicitudWs() {
        val dialogNo = AlertDialog.Builder(this, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
            .setTitle(HtmlCompat.fromHtml("<font color='#3C8943'>Ingresar</font>",
                HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setMessage("OCURRIO UN ERROR, FAVOR DE INTENTARLO MAS TARDE.")
            .setPositiveButton("Aceptar") { dialog, which ->
                dialog.cancel()
            }
        val jsonParametros = JSONObject()
        jsonParametros.put("customer_id", "53557")
        jsonParametros.put("cell_phone", "9631350804")

        val request = object : JsonObjectRequest(
            Method.POST,
            //getString(R.string.urlLogin),
            getString(R.string.url)+getString(R.string.metLogin),
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
                        findViewById<TextView>(R.id.txtPruebas).text =
                            jsonResults.getString("credit_id") + "\n" + response.getString("data")
                        //se guarda en sesion el numero de prestamo
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                        val editor = prefs.edit()
                        editor.putInt("CREDITO_ID", jsonResults.getInt("credit_id"))
                        editor.apply()
                        Toast.makeText(this, "Respuesta: correcto", Toast.LENGTH_SHORT).show()
                        Toast.makeText(this, "Respuesta: $response", Toast.LENGTH_SHORT).show()
                    } else if (jsonData.getInt("code") == 422) {
                        val jsonResults =
                            JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                        findViewById<TextView>(R.id.txtPruebas).text =
                            jsonResults.getString("customer_id")
                    }
                } catch (e: Exception) {
                    dialogNo.show()
                    Toast.makeText(this, "Respuesta: Exception", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener {
                dialogNo.show()
                Toast.makeText(this, "Respuesta: error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["X-Header-Email"] = "apicartera@alfin.presidentasalfin"
                headers["X-Header-Password"] = "password"
                headers["X-Header-Api-Key"] = "1df6a7c1-3057-4dcd-a73a-26bbc46bc860"
                return headers
            }
        }
        try {
            val queue = Volley.newRequestQueue(this)
            queue.add(request)
        } catch (e: Exception) {
            dialogNo.show()
            Toast.makeText(this, "Peticion: Exception", Toast.LENGTH_SHORT).show()
        }
    }
}
/* MD PARA VER QUE TAMAÑO DE PANTALLA ES
       val prefs = PreferenceManager.getDefaultSharedPreferences(this)
       val prestamo = prefs.getInt("CREDITO_ID", 0)
       Toast.makeText(this, "CREDITO_ID : $prestamo", Toast.LENGTH_SHORT).show()
       var densidad = ""
           when (resources.displayMetrics.densityDpi) {
               DisplayMetrics.DENSITY_LOW -> densidad = "ldpi"
               DisplayMetrics.DENSITY_MEDIUM -> densidad = "mdpi"
               DisplayMetrics.DENSITY_HIGH -> densidad ="hdpi"
               DisplayMetrics.DENSITY_XHIGH, DisplayMetrics.DENSITY_280 -> densidad = "xhdpi"
               DisplayMetrics.DENSITY_XXHIGH, DisplayMetrics.DENSITY_360, DisplayMetrics.DENSITY_400, DisplayMetrics.DENSITY_420,DisplayMetrics.DENSITY_440,DisplayMetrics.DENSITY_450 -> densidad = "xxhdpi"
               DisplayMetrics.DENSITY_XXXHIGH, DisplayMetrics.DENSITY_560 -> densidad = "xxxhdpi"
           }
       findViewById<EditText>(R.id.txtUsuario).hint = "Ingresar $densidad"
       */