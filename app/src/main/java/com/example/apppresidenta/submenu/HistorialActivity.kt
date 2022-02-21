package com.example.apppresidenta.submenu

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.R
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.ValGlobales
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*

class HistorialActivity : AppCompatActivity() {
    lateinit var progressBar: CircularProgressIndicator
    var listRad: MutableMap<Int, Int> = mutableMapOf(0 to 0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.historial_activity)

        /*MD SE AGREGA LOGO Y TITULO DEL LA ACTIVIDAD*/
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mi Historial"
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_v6))

        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "true")
        //mostrarHistorial()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val presidenta = prefs.getString("PRESIDENTA", "SIN NOMBRE")
        findViewById<TextView>(R.id.txtPresidenta).text = presidenta
        mostrarFormato(false)
        if (ValGlobales.validarConexion(this)) {
            datosHistorial()
        } else {
            findViewById<TextView>(R.id.txtCargando).text = getString(R.string.noConexion)
            findViewById<TextView>(R.id.txtCargando).gravity = Gravity.CENTER
            findViewById<TextView>(R.id.txtCargando).visibility = View.VISIBLE
            progressBar = findViewById(R.id.cargando)
            progressBar.visibility = View.INVISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return false
    }

    // MD FUNCION PARA OCULTAR LOS DATOS Y MOSTRAR CARGANDO
    private fun mostrarFormato(esMostrar: Boolean) {
        var valor = View.VISIBLE
        var valorLoadi = View.INVISIBLE
        progressBar = findViewById(R.id.cargando)

        if (!esMostrar) {
            valor = View.INVISIBLE
            valorLoadi = View.VISIBLE
            //Toast.makeText(this,esMostrar.toString(), Toast.LENGTH_SHORT).show()
        }

        progressBar.visibility = valorLoadi
        findViewById<TextView>(R.id.txtCargando).visibility = valorLoadi
        findViewById<TextView>(R.id.txtHistorial).visibility = valor
        findViewById<TextView>(R.id.txtPresidenta).visibility = valor
        findViewById<TableLayout>(R.id.tlbHistorial).visibility = valor
        findViewById<TextView>(R.id.txtHistorico).visibility = valor
    }

    private fun datosHistorial() {
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/

        val alertError = FuncionesGlobales.mostrarAlert(
            this,
            "error",
            true,
            "Mi Historial",
            getString(R.string.error),
            false
        )
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val idCliente = prefs.getString("ID_PRESIDENTA", "0")
            val jsonParametros = JSONObject()
            jsonParametros.put("customer_id", idCliente)

            val request =
                @SuppressLint("SetTextI18n") //<-- MD SE AGREGA PARA ADMITIR UNA VARIEDAD DE CONFIGURACIONES REGIONALES SIN TENER QUE MODIFICAR CÓDIGO EN LA CONCATENACION DE CADENAS
                object : JsonObjectRequest(
                    Method.POST,
                    getString(R.string.urlHistorialGrupo),
                    jsonParametros,
                    Response.Listener { response ->
                        try {
                            //MD Obtiene su respuesta json
                            val jsonData =
                                JSONTokener(response.getString("data")).nextValue() as JSONObject
                            if (jsonData.getInt("code") == 200) {
                                val jsonResults = jsonData.getJSONArray("results")
                                llenarTablaHistorial(jsonResults)
                            }
                        } catch (e: Exception) {
                            if (e.message != null) {
                                alertError.show()
                            }
                        }
                    }, Response.ErrorListener { error ->
                        // MD MANEJO DE ERRORES EN LA RESPUESTA DE LA PETICION AL WS
                        var mensaje = getString(R.string.error)
                        try {
                            val responseError = String(error.networkResponse.data)
                            val dataError = JSONObject(responseError)
                            val jsonData =
                                JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                            val code = jsonData.getInt("code")
                            val message = jsonData.getString("message")
                            val jResul =
                                JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                            if (code == 422 && jsonData.getString("results")
                                    .contains("customer_id")
                            ) {
                                mensaje = jResul.getString("customer_id")
                                alertError.setMessage("El crédito seleccionado no tiene historial.")
                            } else {
                                mensaje = message
                            }

                        } catch (e: Exception) {
                            val codigo = error.networkResponse.statusCode
                                mensaje = "Error: $codigo \n${getString(R.string.errorServidor)}"
                        }
                        progressBar = findViewById(R.id.cargando)
                        progressBar.visibility = View.INVISIBLE
                        findViewById<TextView>(R.id.txtCargando).text = mensaje
                        alertError.setMessage(mensaje)
                        alertError.show()
                    }
                ) {
                    override fun getHeaders(): Map<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Content-Type"] = getString(R.string.content_type)
                        headers["X-Header-Email"] = getString(R.string.header_email)
                        headers["X-Header-Password"] = getString(R.string.header_password)
                        headers["X-Header-Api-Key"] = getString(R.string.header_api_key)
                        return headers
                    }
                }
            val queue = Volley.newRequestQueue(this)
            //MD PRIMERO BORRAMOS EL CACHE Y ENVIAMOS DESPUES LA PETICION
            queue.cache.clear()
            queue.add(request)
        } catch (e: Exception) {
            // Couldn't properly decode data to string
            alertError.show()
        }
        /*******  FIN ENVIO   *******/

    }

    private fun llenarTablaHistorial(jsonResults: JSONArray) {
        //SE OBTIENE LA TABLA
        val tabla = findViewById<TableLayout>(R.id.tlbHistorial)
        //ENCABEZADO
        val fontTh = 16F
        val fontTr = 14F
        val trEn = TableRow(this)
        trEn.setPadding(0, 20, 0, 20)
        val cliente = TextView(this)
        cliente.text = "CICLO"
        cliente.gravity = Gravity.RIGHT
        cliente.setTextColor(Color.WHITE)
        cliente.setTypeface(null, Typeface.BOLD_ITALIC)
        cliente.textSize = fontTh
        cliente.maxWidth = 130
        trEn.addView(cliente)

        val cuota = TextView(this)
        cuota.text = "  "
        cuota.setTextColor(Color.WHITE)
        cuota.setTypeface(null, Typeface.BOLD_ITALIC)
        cuota.textSize = fontTh
        trEn.addView(cuota)

        val cuot = TextView(this)
        cuot.text = "GRUPO"
        cuot.gravity = Gravity.CENTER
        cuot.setTextColor(Color.WHITE)
        cuot.setTypeface(null, Typeface.BOLD_ITALIC)
        cuot.textSize = fontTh
        trEn.addView(cuot)

        trEn.setBackgroundResource(R.drawable.redondo_verde)
        tabla.addView(trEn)
        val numCreditos = jsonResults.length()
        listRad.clear()
        for (i in 0 until numCreditos) {
            //MD SE GENERA EL OBJETO CLIENTE DEL ARREGLO PARA ACCEDER A SUS DATOS
            val cte: JSONObject = jsonResults.getJSONObject(i)
            val tr = TableRow(this)
            tr.setPadding(10, 30, 0, 30)
            if (i != numCreditos) {
                tr.setBackgroundResource(R.drawable.borde)
            } else {
                tr.setBackgroundResource(R.drawable.borde_redondeado_verde)
            }
            val colorAzul = ContextCompat.getColor(this, R.color.Azul1)

            val l = LinearLayout(this)
            l.gravity = Gravity.CENTER
            val radIdCliente = CheckBox(this)
            val idCredito = cte.getInt("credit_id")
            radIdCliente.id = idCredito
            radIdCliente.gravity = Gravity.CENTER
            radIdCliente.setOnClickListener { mostrarCredito(idCredito) }

            val ciclo = TextView(this)
            ciclo.setTextColor(colorAzul)
            ciclo.textSize = fontTr
            val fInicio = FuncionesGlobales.convertFecha(cte.getString("start_date"), "dd-MMM-yyyy")
                .replace(".-", "-").uppercase()
            val fFin = FuncionesGlobales.convertFecha(cte.getString("end_date"), "dd-MMM-yyyy")
                .replace(".-", "-").uppercase()
            ciclo.text = "Del $fInicio \nal  $fFin "
            ciclo.gravity = Gravity.RIGHT

            val grupo = TextView(this)
            grupo.text = cte.getString("group_name")+"  \n "+HtmlCompat.fromHtml("<font color='#FDCB6E'>  $idCredito</font>",HtmlCompat.FROM_HTML_MODE_LEGACY)

            //"${cte.getString("group_name")} \n $credit"
            grupo.setTextColor(colorAzul)
            grupo.textSize = fontTr

            val esp = TextView(this)
            esp.text = "    "
            esp.setTextColor(colorAzul)
            esp.textSize = fontTr

            l.addView(ciclo)
            //l.addView(radIdCliente)
            tr.addView(l)
            tr.addView(esp)
            tr.addView(grupo)
            tr.addView(radIdCliente)
            listRad.put(cte.getInt("credit_id"), cte.getInt("credit_id"))
            tr.gravity = Gravity.CENTER
            tabla.addView(tr)
        }
        mostrarFormato(true)
    }

    private fun mostrarCredito(idCredito: Int) {
        //findViewById<TextView>(R.id.txtHistorico).text = "Credito seleccionado: $idCredito ${listRad.count()}"
        for (r in listRad) {
            try {
                //MD SE QUITA LA SELECCION A TODOS LOS DEMAS EXCEPTO AL ELEGIDO
                if (r.key != 0 && r.key != idCredito) {
                    findViewById<CheckBox>(r.value).isChecked = false
                }
                findViewById<TextView>(R.id.txtHistorico).text = "Credito seleccionado: $idCredito"
            } catch (e: Exception) {
            }
        }
        /* MD SE ASIGNA A VARIABLE EL VALOR DEL CREDITO SELECCIONADO PARA MOSTAR SU HISTORIAL
           ESTO SE DEBE DE REALIZAR EN AUTOMATICO PUES CON ESTA VARIABLE DE SESION SE REALIZAN LAS PETICIONES
         */
        FuncionesGlobales.guardarVariableSesion(this,"Int","CREDITO_ID",idCredito.toString())
        //FuncionesGlobales.guardarVariableSesion(this,"String","ID_PRESIDENTA","0")
    }


    private fun mostrarHistorial() {
        //SE OBTIENE LA TABLA
        val tabla = findViewById<TableLayout>(R.id.tlbHistorial)
        //ENCABEZADO
        val fontTh = 18F
        val fontTr = 16F
        val trEn = TableRow(this)

        val ciclo = TextView(this)
        ciclo.text = "Ciclo"
        ciclo.setPadding(0, 20, 20, 20)
        ciclo.gravity = Gravity.CENTER
        ciclo.setTextColor(Color.WHITE)
        ciclo.setTypeface(null, Typeface.BOLD_ITALIC)
        ciclo.textSize = fontTh
        ciclo.maxWidth = 200

        trEn.addView(ciclo)

        val gpo = TextView(this)
        gpo.text = "Grupo"
        gpo.setPadding(0, 20, 20, 20)
        gpo.gravity = Gravity.CENTER
        gpo.setTextColor(Color.WHITE)
        gpo.setTypeface(null, Typeface.BOLD_ITALIC)
        gpo.textSize = fontTh
        trEn.addView(gpo)

        trEn.gravity = Gravity.CENTER
        trEn.setBackgroundResource(R.drawable.redondo_verde)

        tabla.addView(
            trEn,
            TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
            )
        )
        val numGpos = 5
        //FOR DEL NUMERO DE CLIENTES
        for (i in 1..numGpos) {
            val tr = TableRow(this)

            if (i != numGpos) {
                tr.setBackgroundResource(R.drawable.borde)
            } else {
                tr.setBackgroundResource(R.drawable.borde_redondeado_verde)
            }

            val linea = LinearLayout(this)
            linea.setPadding(0, 0, 0, 0)

            val txtCiclo = TextView(this)
            txtCiclo.text = "1$i/0$i/21 al 10/0$i/21"
            txtCiclo.setPadding(10, 20, 5, 20)
            //txtCiclo.gravity = Gravity.LEFT
            txtCiclo.setTextColor(ContextCompat.getColor(this, R.color.Azul1))
            txtCiclo.textSize = fontTr
            //linea.setBackgroundResource(borde_redondeado_verde)
            linea.addView(txtCiclo)

            val txtGpo = TextView(this)
            txtGpo.text = "Arbol de vida"
            txtGpo.setPadding(5, 20, 5, 20)
            txtGpo.gravity = Gravity.CENTER
            txtGpo.setTextColor(ContextCompat.getColor(this, R.color.Azul1))
            txtGpo.setTypeface(null, Typeface.BOLD_ITALIC)
            txtGpo.textSize = fontTr

            var grupo: String
            when (i) {
                2 -> {
                    grupo = "Arbol de vida $i"
                }
                3 -> {
                    grupo = "Las flores"
                }
                else -> {
                    grupo = "Las flores $i"
                }
            }

            txtGpo.text = grupo
            //tr.addView(txtCiclo)
            tr.addView(linea)
            tr.addView(txtGpo)

            tr.gravity = Gravity.CENTER
            tabla.addView(
                tr,
                TableLayout.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
            )
            tabla.isShrinkAllColumns = false

        }
    }
}