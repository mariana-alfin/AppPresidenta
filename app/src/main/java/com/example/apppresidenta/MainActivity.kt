package com.example.apppresidenta

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONObject
import org.json.JSONTokener


class MainActivity : AppCompatActivity() {
    lateinit var progressBar: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this,"MainActivity")

        supportActionBar?.hide()
        //findViewById<Button>(R.id.btnLogin).setOnClickListener { solicitudWs() }
        findViewById<Button>(R.id.btnLogin).setOnClickListener { validarFormulario() }
        //findViewById<Button>(R.id.btnLogin).setOnClickListener { errores() }
        //pone el baner en la barra de la aplicacion
        //supportActionBar?.setBackgroundDrawable(getResources().getDrawable(R.drawable.bannerprueba))
        /*
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val prestamo = prefs.getInt("CREDITO_ID", 0)
        Toast.makeText(this, "CREDITO_ID : $prestamo", Toast.LENGTH_SHORT).show()
        */
    }
    override fun onBackPressed() {
        //FUNCION QUE SE EJECUTA AL PERSIONAR EL BOTON ATRAS DE MOMENTO NO DEBE DE HACER NADA
        //Toast.makeText(this, "PULSO HACIA ATRAS", Toast.LENGTH_SHORT).show()
    }
    private fun validarFormulario() {
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

                iniciarSesionWS(idCliente, numeroCelular)
            } else {
                AlertDialog.Builder(this, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
                    .setTitle(Html.fromHtml("<font color='#3C8943'>Ingresar</font>"))
                    .setMessage(respuesta)
                    .setPositiveButton("ACEPTAR", null)
                    .create()
                    .show()
            }
        }
    }

    private fun iniciarSesionWS(idCliente: String, numeroCelular: String) {
        progressBar = this.findViewById(R.id.cargando)
        //se muestra loading
        progressBar.visibility = View.VISIBLE

        //se oculta boton
        findViewById<Button>(R.id.btnLogin).visibility = View.GONE
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/

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
                        val home = Intent(this, Navegacion::class.java)
                        startActivity(home)
                    } else {
                        dialogNo.show()
                    }
                    ocultarCargando()
                } catch (e: Exception) {
                    ocultarCargando()
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
                ocultarCargando()
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
    }

    fun ocultarCargando() {
        progressBar = this.findViewById(R.id.cargando)
        //se muestra loading
        progressBar.visibility = View.GONE
        //se oculta boton
        findViewById<Button>(R.id.btnLogin).visibility = View.VISIBLE
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
            .setTitle(Html.fromHtml("<font color='#3C8943'>Ingresar</font>"))
            .setMessage("OCURRIO UN ERROR, FAVOR DE INTENTARLO MAS TARDE.")
            .setPositiveButton("Aceptar") { dialog, which ->
                dialog.cancel()
            }
        val jsonParametros = JSONObject()
        jsonParametros.put("customer_id", "53557")
        jsonParametros.put("cell_phone", "9631350804")

        val request = object : JsonObjectRequest(
            Method.POST,
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
                headers["X-Header-Email"] = "apicartera@alfin.com"
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

fun errores(){

    //val stringRequest = object : JsonObjectRequest(
        val jsonParametros = JSONObject()
    jsonParametros.put("customer_id", "53557")
    jsonParametros.put("cell_phone", "9631350884")

    val request = object : JsonObjectRequest(
        Method.POST,
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
                Toast.makeText(this, "Respuesta: Exception", Toast.LENGTH_SHORT).show()
            }
        },
        Response.ErrorListener { error ->
            //val errorD = VolleyError(String(error.networkResponse.data))
            val responseBody = String(error.networkResponse.data)
            val data = JSONObject(responseBody)
            try {
                val jsonData = JSONTokener(data.getString("error")).nextValue() as JSONObject
                val code = jsonData.getInt("code")
                val message = jsonData.getString("message")
                var mensaje = "Ocurrio un error"
                val jResul = JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                val esCell_phone = jsonData.getString("results").contains("cell_phone")
                val esCustomer_id = jsonData.getString("results").contains("customer_id")

                if(code == 422 && esCell_phone){
                    mensaje = jResul.getString("cell_phone")
                } else if(code == 422 && esCustomer_id){
                    mensaje = jResul.getString("customer_id")
                }else{
                    mensaje = message
                }
                findViewById<TextView>(R.id.txtPruebas).text = mensaje
            }catch (e: Exception){
                findViewById<TextView>(R.id.txtPruebas).text = e.toString()
            }


            //val errors = data.getJSONArray("error")
            //val jsonMessage = errors.getJSONObject(0)
            //val message = jsonMessage.getString("message")

            //errorD.localizedMessage.toString()
        }
        /* override fun onErrorResponse(error: VolleyError) {
             Toast.makeText(this@MainActivity, "Register Error!$error", Toast.LENGTH_SHORT)
                 .show()
             var body: String

             //get status code here
             //get status code here
             val statusCode = java.lang.String.valueOf(error.networkResponse.statusCode)
             val data = java.lang.String.valueOf(error.networkResponse.data)
             val errorD = VolleyError(String(error.networkResponse.data))
             //get response body and parse with appropriate encoding
             //get response body and parse with appropriate encoding
             try {
                 body = String(error.networkResponse.data)
             } catch (e: UnsupportedEncodingException) {
                 // exception
             }
             findViewById<TextView>(R.id.txtPruebas).text = errorD.toString()
         }*/) {
        override fun getHeaders(): Map<String, String> {
            val headers = HashMap<String, String>()
            headers["Content-Type"] = "application/json"
            headers["X-Header-Email"] = "apicartera@alfin.com"
            headers["X-Header-Password"] = "password"
            headers["X-Header-Api-Key"] = "1df6a7c1-3057-4dcd-a73a-26bbc46bc860"
            return headers
        }
    }
    try {
        val queue = Volley.newRequestQueue(this)
        queue.cache.clear()
        queue.add(request)
    } catch (e: Exception) {
        Toast.makeText(this, "Peticion: Exception", Toast.LENGTH_SHORT).show()
    }
}
}
