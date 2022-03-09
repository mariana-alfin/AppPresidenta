package com.example.apppresidenta.submenu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.R
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.FuncionesGlobales.Companion.encriptacion
import com.example.apppresidenta.generales.LoadingScreen
import org.json.JSONObject
import org.json.JSONTokener

class RecuperarNIPActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recuperar_nip_activity)
        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "true")
        /*SE AGREGA LOGO Y TITULO DEL LA ACTIVIDAD*/
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = HtmlCompat.fromHtml("<font color='#FFFFFF'>Actualizar NIP</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_v6))
        textChangueNip()
        findViewById<Button>(R.id.btnConfirmarNip).setOnClickListener { confirmarNIP() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return false
    }

    fun confirmarNIP() {
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
        if (nip.length == 4 && nipC.length == 4){
            //SON IGUALES
            if (nip == nipC) {
                actualizarNIPWS(nip)
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
                    "Actualización NIP",
                    "El NIP y la Confirmación deben de ser iguales.",
                    false
                ).show()
            }
        } else {
            FuncionesGlobales.mostrarAlert(
                this,
                "error",
                true,
                "Actualización NIP",
                "El NIP debe de ser a 4 dígitos.",
                false
            ).show()
        }
    }

    //SE ENVIAN DATO Y SI ES CORRECTO ENVIA A LOGIN
    private fun actualizarNIPWS(nip: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val idCliente = prefs.getString("ID_PRESIDENTA", "--")
        LoadingScreen.displayLoadingWithText(this, "Registrando información...", false)

        val token = prefs.getString(getString(R.string.token), "")

        Log.d("Token","Token firebase: $token")

        //Se encripta el nip para enviar al WS
        val nipEncriptado = encriptacion(this, nip, idCliente!!, 1)!!

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
                    //findViewById<TextView>(R.id.textView9).text = response.toString()
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
                            val activity = Intent(this, MiCuentaActivity::class.java)
                            startActivity(activity)
                            finish()
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
}