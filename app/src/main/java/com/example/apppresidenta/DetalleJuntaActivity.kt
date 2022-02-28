package com.example.apppresidenta

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.FuncionesGlobales.Companion.convertPesos
import com.example.apppresidenta.generales.ValGlobales
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*

class DetalleJuntaActivity : AppCompatActivity() {
    lateinit var progressBar: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detalle_junta_activity)

        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "true")

        val parametros = this.intent.extras
        val pay_id = parametros!!.getInt("pay_id", 0)
        val task_number = parametros.getInt("task_number", 1)
        val montoRecuperado = parametros.getDouble("monto", 0.0)
        val fechaPago = parametros.getString("fechaPago", "")

        /*MD SE AGREGA LOGO Y TITULO DEL LA ACTIVIDAD*/
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title =
            HtmlCompat.fromHtml("<font color='#FFFFFF'>Junta # $task_number</font> ",//$pay_id",
                HtmlCompat.FROM_HTML_MODE_LEGACY)
        supportActionBar?.subtitle = "Monto Capturado " + convertPesos(montoRecuperado, 2)
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_v6))

        val fecha =
            FuncionesGlobales.convertFecha(fechaPago, "dd-MMM-yy").replace(".-", "-").uppercase()
        findViewById<TextView>(R.id.txtDatosPago).text = "  Fecha de pago: $fecha"

        mostrarFormato(false)
        if (ValGlobales.validarConexion(this)) {
            datosJunta(pay_id, task_number)
        } else {
            findViewById<TextView>(R.id.txtCargando).text = getString(R.string.noConexion)
            findViewById<TextView>(R.id.txtCargando).gravity = Gravity.CENTER
            findViewById<TextView>(R.id.txtCargando).visibility = View.VISIBLE
            progressBar = findViewById(R.id.cargando)
            progressBar.visibility = View.INVISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        onBackPressed()
        return false
    }

    private fun mostrarFormato(esMostrar: Boolean) {
        var valor = View.VISIBLE
        var valorLoadi = View.INVISIBLE
        progressBar = findViewById(R.id.cargando)

        if (!esMostrar) {
            valor = View.INVISIBLE
            valorLoadi = View.VISIBLE
        }
        progressBar.visibility = valorLoadi
        findViewById<TextView>(R.id.txtCargando).visibility = valorLoadi
        findViewById<TextView>(R.id.txtDatosPago).visibility = valor
    }

    private fun datosJunta(payId: Int, taskNumber: Int) {
        /**************   MD  ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alertError = FuncionesGlobales.mostrarAlert(
            this,
            "error",
            true,
            "Datos Junta",
            getString(R.string.error),
            false
        )
        try {
            val jsonParametros = JSONObject()
            jsonParametros.put("pay_id", payId)
            jsonParametros.put("task_number", taskNumber)

            val request = object : JsonObjectRequest(
                Method.POST,
                getString(R.string.urlDatosJuntasPgo),
                jsonParametros,
                Response.Listener { response ->
                    try {
                        //Obtiene su respuesta json
                        val jsonData =
                            JSONTokener(response.getString("data")).nextValue() as JSONObject
                        if (jsonData.getInt("code") == 200) {
                            val jsonResults = jsonData.getJSONArray("results")
                            pintarTablaJunta(jsonResults)
                        }
                    } catch (e: Exception) {
                        alertError.show()
                    }
                },
                Response.ErrorListener { error ->
                    val codigoError = error.networkResponse.statusCode
                    if (codigoError == 422) {
                        alertError.setMessage("El ID de Crédito no se encontro.")
                    } else {
                        alertError.setMessage("Error: $codigoError \n${getString(R.string.errorServidor)}")
                    }
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

    private fun pintarTablaJunta(jsonClientes: JSONArray) {
        //SE OBTIENE LA TABLA
        val tabla = findViewById<TableLayout>(R.id.tblJunta)
        tabla.removeAllViews()
        //ENCABEZADO
        val fontTh = 17F
        val fontTr = 15F
        val trEn = TableRow(this)
        trEn.setPadding(0, 20, 0, 20)
        val cliente = TextView(this)
        cliente.text = "Cliente"
        //cliente.setPadding(0, 20, 20, 20)
        cliente.gravity = Gravity.CENTER
        cliente.setTextColor(Color.WHITE)
        cliente.setTypeface(null, Typeface.BOLD_ITALIC)
        cliente.textSize = fontTh
        cliente.maxWidth = 130
        trEn.addView(cliente)

        val cuota = TextView(this)
        cuota.text = "Pago"
        //cuota.setPadding(0, 20, 20, 20)
        cuota.gravity = Gravity.CENTER
        cuota.setTextColor(Color.WHITE)
        cuota.setTypeface(null, Typeface.BOLD_ITALIC)
        cuota.textSize = fontTh
        trEn.addView(cuota)

        val pago = TextView(this)
        pago.text = "Pago"
        pago.alpha = 0.0F
        pago.gravity = Gravity.CENTER
        pago.setTextColor(Color.WHITE)
        pago.setTypeface(null, Typeface.BOLD_ITALIC)
        pago.textSize = fontTh
        trEn.addView(pago)

        val sol = TextView(this)
        sol.text = "Solidario"
        //sol.setPadding(0, 20, 0, 20)
        sol.gravity = Gravity.CENTER
        sol.setTextColor(Color.WHITE)
        sol.setTypeface(null, Typeface.BOLD_ITALIC)
        sol.textSize = fontTh
        trEn.addView(sol)

        val so = TextView(this)
        so.text = "____"
        so.alpha = 0.0F //MD HACE COMPLETAMENTE OPACO UN ELEMENTO
        so.gravity = Gravity.CENTER
        so.setTextColor(ContextCompat.getColor(this, R.color.Verde2))
        so.setTypeface(null, Typeface.BOLD_ITALIC)
        so.textSize = fontTh
        trEn.addView(so)

        trEn.gravity = Gravity.CENTER
        trEn.setBackgroundResource(R.drawable.redondo_verde)
        tabla.addView(
            trEn//, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        val numClientes = jsonClientes.length()
        /**     VARIABLES PARA TAMAÑOS      **/
        //MD SE OBTIENE LA DENSIDAD DEL DISPOSITIVO PARA CAMBIAR LOS TAMAÑOS
        val densidad = resources.displayMetrics.densityDpi
        val width = resources.displayMetrics.widthPixels
        var witCte: Int
        var witPgo = 140
        if (densidad < DisplayMetrics.DENSITY_HIGH) {
            witCte = 140
        } else if (densidad in DisplayMetrics.DENSITY_HIGH until DisplayMetrics.DENSITY_XHIGH) {
            witCte = 200
        } else if (densidad == DisplayMetrics.DENSITY_XHIGH) {
            witCte = 220
        } else if (densidad in DisplayMetrics.DENSITY_XHIGH until DisplayMetrics.DENSITY_XXHIGH) {
            witCte = 380
            witPgo = 200
        } else if (densidad == DisplayMetrics.DENSITY_XXHIGH && width == 1080) {
            witCte = 380
            witPgo = 175
        } else if ((densidad in DisplayMetrics.DENSITY_XXHIGH until DisplayMetrics.DENSITY_XXXHIGH) && width < 1200 && width != 1080) {
            witCte = 415
            witPgo = 205
        } else if ((densidad >= DisplayMetrics.DENSITY_XXXHIGH) || width <= 1200) {
            witCte = 430
            witPgo = 215
        } else {//MD SI LA PANTALLA ES MAYOR A 1200PX EL VALOR DEL NOMBRE SERA DE UNA TERCERA PARTE
            witCte = (width / 3)
            witPgo = 225
        }
        /***       FIN VARIABLES       **/
        for (i in 0 until numClientes) {
            //MD SE GENERA EL OBJETO CLIENTE DEL ARREGLO PARA ACCEDER A SUS DATOS
            val cte: JSONObject = jsonClientes.getJSONObject(i)
            val tr = TableRow(this)
            tr.setPadding(10, 20, 0, 20)
            if (i != numClientes) {
                tr.setBackgroundResource(R.drawable.borde)
            } else {
                tr.setBackgroundResource(R.drawable.borde_redondeado_verde)
            }
            val l = LinearLayout(this)
            val chk = CheckBox(this)
            if (cte.getInt("validate") == 1) {
                chk.isChecked = true
            }
            chk.isEnabled = false
            chk.gravity = Gravity.RIGHT
            val colorAzul = ContextCompat.getColor(this, R.color.Azul1)
            val cliente = TextView(this)
            cliente.setTextColor(colorAzul)
            cliente.text = cte.getString("customer_name")
            cliente.textSize = fontTr
            cliente.maxWidth = witCte

            val couta = TextView(this)
            val mCuota = cte.getDouble("amount")
            couta.text = convertPesos(mCuota,0)
            couta.setTextColor(colorAzul)
            couta.textSize = fontTr
            couta.width = witPgo
            couta.gravity = Gravity.CENTER

            val pago = TextView(this)
            pago.setTextColor(colorAzul)
            pago.textSize = fontTr

            val lS = LinearLayout(this)
            val sol = TextView(this)
            sol.text = converSolidario(cte.getInt("result_type_id"))//+" --> "+cte.getString("result_type_id")
            sol.setTextColor(colorAzul)
            sol.textSize = fontTr
            sol.width = witPgo

            //SE AGREGA COLUMNA CLIENTE
            l.addView(chk)
            l.addView(cliente)
            tr.addView(l)
            //SE AGREGA COLUMNA COUTA
            tr.addView(couta)
            //SE AGREGA COLUMNA PAGO
            tr.addView(pago)
            //SE COLUMNA AGREGA SOLIDARIO
            lS.addView(sol)
            tr.addView(lS)

            tr.gravity = Gravity.CENTER
            tabla.addView(tr)
        }
        mostrarFormato(true)
    }

    fun converSolidario(valor: Int):String{
        val solidario = when (valor) {
            1, 11 -> {
                "DA"
            }
            12 -> {
                "RE"
            }
            13 -> {
                "NA"
            }
            else -> {
                "NA"
            }
        }
        return solidario
    }

}