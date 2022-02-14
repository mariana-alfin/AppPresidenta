package com.example.apppresidenta.submenu

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableLayout.LayoutParams.MATCH_PARENT
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.R
import com.example.apppresidenta.generales.ValGlobales
import com.example.apppresidenta.utils.GeneralUtils
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONObject
import org.json.JSONTokener
import java.text.NumberFormat
import java.util.*

class DetalleClienteActivity : AppCompatActivity() {

    //MD FORMATO EN PESOS MXM
    private val mx = Locale("es", "MX")
    private val formatPesos: NumberFormat = NumberFormat.getCurrencyInstance(mx)
    lateinit var progressBar: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detalle_cliente_activity)
        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this,"true")

        mostrarFormato(false)

        if (ValGlobales.validarConexion(this)) {
            val parametros = this.intent.extras
            val id = parametros!!.getInt("idCliente", 25)
            detalleCliente(id)
        } else {
            findViewById<TextView>(R.id.txtCargando).text = getString(R.string.noConexion)
            findViewById<TextView>(R.id.txtCargando).gravity = Gravity.CENTER
            findViewById<TextView>(R.id.txtCargando).visibility = View.VISIBLE
            progressBar = findViewById(R.id.cargando)
            progressBar.visibility = View.INVISIBLE
        }
        /*MD SE AGREGA LOGO Y TITULO DEL LA ACTIVIDAD*/
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detalle Cliente"
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_v6))

        //pintarTablaDetalle()
    }
    override fun onSupportNavigateUp(): Boolean {
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
        findViewById<TextView>(R.id.txtNombreCliente).visibility = valor
        findViewById<TextView>(R.id.txtDireccionCliente).visibility = valor
        findViewById<ImageView>(R.id.imaCel).visibility = valor
        findViewById<ImageView>(R.id.imaWhats).visibility = valor
    }
    private fun detalleCliente(credit_id: Int) {
        /**************   MD  ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/

        val alertError = FuncionesGlobales.mostrarAlert(this,"error",true,"Detalle Cliente",getString(R.string.error),false)
        val jsonParametros = JSONObject()
        jsonParametros.put("credit_id", credit_id)

        val request =
            @SuppressLint("SetTextI18n") //<-- se agrega para admitir una variedad de configuraciones regionales sin tener que modificar código en la concatenacion de cadenas
            object : JsonObjectRequest(
                Method.POST,
                getString(R.string.urlDetalleCliente),
                jsonParametros,
                Response.Listener { response ->
                    try {
                        //MD OBTIENE SU RESPUESTA JSON
                        val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject
                        if(jsonData.getInt("code") == 200)//si la peticion fue correcta se continua con el login
                        {
                            val jsonResults = JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                            pintarTablaDetalle(jsonResults)
                        }else{
                            alertError.show()
                        }
                    } catch (e: Exception) {
                        alertError.show()
                    }

                },
                Response.ErrorListener { error ->
                    //val errorD = VolleyError(String(error.networkResponse.data))
                    val responseError = String(error.networkResponse.data)
                    val dataError = JSONObject(responseError)
                    try {
                        val jsonData = JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                        val code = jsonData.getInt("code")
                        val jResul = JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                        if(code == 422 && jsonData.getString("results").contains("credit_id")){
                            alertError.setMessage(jResul.getString("credit_id"))
                        }
                    }catch (e: Exception){
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
        try {
            val queue = Volley.newRequestQueue(this)
            //MD PRIMERO BORRAMOS EL CACHE Y ENVIAMOS DESPUES LA PETICION
            queue.cache.clear()
            queue.add(request)
        }catch(e: Exception){
            alertError.show()
        }
        /*******  FIN ENVIO   *******/
    }
    private fun pintarTablaDetalle(jsonResults: JSONObject) {

        val nombre = jsonResults.getString("customer_name")
        val direccion = jsonResults.getString("address")
        val telefono = jsonResults.getString("cell_phone")
        findViewById<TextView>(R.id.txtNombreCliente).text = nombre
        findViewById<TextView>(R.id.txtDireccionCliente).text = direccion

        findViewById<ImageView>(R.id.imaCel).setOnClickListener{ GeneralUtils.llamarContacto(this, telefono) }
        findViewById<ImageView>(R.id.imaWhats).setOnClickListener{ validacionesEnvioWhats(telefono,getString(R.string.mensaje_whats) +" "+ nombre) }

        //SE OBTIENE LA TABLA
        val tabla = findViewById<TableLayout>(R.id.tblDetalle)
        val fTr = 18F
        //ENCABEZADO
        val trEn = TableRow(this)
        val txtRo = TextView(this)
        txtRo.text = "Datos del Crédito"
        txtRo.setPadding(30, 20, 30, 20)
        txtRo.gravity = Gravity.CENTER
        //txtRo.maxWidth = 50
        txtRo.setTextColor(Color.WHITE)
        txtRo.setTypeface(null, Typeface.BOLD_ITALIC)
        txtRo.textSize = 20F
        trEn.gravity = Gravity.CENTER
        trEn.setBackgroundResource(R.drawable.redondo_verde)
        trEn.addView(txtRo)

        tabla.addView(trEn, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        //PRESTAMO
        val trP = TableRow(this)
        trP.setBackgroundResource(R.drawable.borde)
        val prest = TextView(this)
        prest.text = "Prestamo"
        prest.setPadding(25, 20, 25, 20)
        prest.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
        prest.setTypeface(null, Typeface.BOLD)
        prest.textSize = fTr
        trP.addView(prest)

        val txtPrestamo = TextView(this)
        txtPrestamo.text = formatPesos.format(jsonResults.getDouble("credit_amount"))
        txtPrestamo.setPadding(25, 20, 25, 20)
        txtPrestamo.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
        txtPrestamo.setTypeface(null, Typeface.BOLD)
        txtPrestamo.textSize = fTr
        trP.addView(txtPrestamo)
        trP.gravity = Gravity.CENTER
        tabla.addView(trP, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        //MONTO A PAGAR
        val trMp = TableRow(this)
        trMp.setBackgroundResource(R.drawable.borde)
        val Mp = TextView(this)
        Mp.text = "Monto a Pagar"
        Mp.setPadding(25, 20, 25, 20)
        Mp.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
        Mp.setTypeface(null, Typeface.BOLD)
        Mp.textSize = fTr
        trMp.addView(Mp)

        val txtMp = TextView(this)
        txtMp.text = formatPesos.format(jsonResults.getDouble("pays_total"))
        txtMp.setPadding(25, 20, 25, 20)
        txtMp.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
        txtMp.setTypeface(null, Typeface.BOLD)
        txtMp.textSize = fTr
        trMp.addView(txtMp)
        trMp.gravity = Gravity.CENTER
        tabla.addView(trMp, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        //MONTO PAGADO
        val trMtop = TableRow(this)
        trMtop.setBackgroundResource(R.drawable.borde)
        val mtop = TextView(this)
        mtop.text = "Monto Pagado"
        mtop.setPadding(25, 20, 25, 20)
        mtop.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
        mtop.setTypeface(null, Typeface.BOLD)
        mtop.textSize = fTr
        trMtop.addView(mtop)

        val txtMtoP = TextView(this)
        txtMtoP.text = formatPesos.format(jsonResults.getDouble("payments"))
        txtMtoP.setPadding(25, 20, 25, 20)
        txtMtoP.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
        txtMtoP.setTypeface(null, Typeface.BOLD)
        txtMtoP.textSize = fTr
        trMtop.addView(txtMtoP)
        trMtop.gravity = Gravity.CENTER
        tabla.addView(trMtop, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        //SALDO VENCIDO
        val trSV = TableRow(this)
        trSV.setBackgroundResource(R.drawable.borde)
        val sv = TextView(this)
        sv.text = "Saldo vencido"
        sv.setPadding(25, 20, 25, 20)
        sv.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
        sv.setTypeface(null, Typeface.BOLD)
        sv.textSize = fTr
        trSV.addView(sv)

        val txtSV = TextView(this)
        val saldoVencido = jsonResults.getDouble("min_pay")
        txtSV.text = formatPesos.format(saldoVencido)
        txtSV.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
        txtSV.setPadding(25, 20, 25, 20)
        txtSV.setTypeface(null, Typeface.BOLD)
        txtSV.textSize = fTr
        txtSV.gravity = Gravity.CENTER
        trSV.addView(txtSV)
        trSV.gravity = Gravity.CENTER
        tabla.addView(trSV, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        //DIAS ATRASO
        val trDA = TableRow(this)
        val txtda = TextView(this)
        txtda.text = "Días de atraso"
        txtda.setPadding(25, 20, 25, 20)
        txtda.gravity = Gravity.LEFT
        txtda.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
        txtda.setTypeface(null, Typeface.BOLD)
        txtda.textSize = fTr
        trDA.addView(txtda)

        val txtDa = TextView(this)
        var dias_atraso = jsonResults.getInt("due")
        txtDa.text = "$dias_atraso"
        txtDa.setPadding(25, 20, 25, 20)
        txtDa.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
        txtDa.setTypeface(null, Typeface.BOLD)
        txtDa.textSize = fTr
        txtDa.gravity = Gravity.CENTER

        if (dias_atraso != 0){
            txtDa.setTextColor(Color.RED)
        }
        if (saldoVencido != 0.0){
            txtSV.setTextColor(Color.RED)
        }

        trDA.addView(txtDa)
        trDA.gravity = Gravity.CENTER
        trDA.setBackgroundResource(R.drawable.borde_redondeado_verde)
        tabla.addView(trDA, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        mostrarFormato(true)
    }
    private fun validacionesEnvioWhats(telefono : String, nombreclienta : String) {
        if (!GeneralUtils.validarAplicacionInstalada(getString(R.string.packagename_whats), this))
        {
            GeneralUtils.mostrarAlertInstalarApp(this, getString(R.string.packagename_whats))
            return
        }
        GeneralUtils.enviarMensajeWhatsApp(this, getString(R.string.mensaje_whats) + " " + nombreclienta, telefono)
    }
    /*
    private fun mostrarDatos() {
        val parametros = this.intent.extras
        val nombreCliente = parametros!!.getString("nombreCliente")
        val id = parametros!!.getInt("idCliente", 25)
        val direccionCliente = "Direccion: Priv Carabia Mz. ${(id * 0.25).toInt()} Urbina Villa del Rey C.P 54693"

        val fT = 24F
        findViewById<TextView>(R.id.txtNombreCliente).text = nombreCliente
        findViewById<TextView>(R.id.txtDireccionCliente).text = direccionCliente
        findViewById<TextView>(R.id.txtTelefonoCliente).text = "Télefono: 5547999878"

        findViewById<TextView>(R.id.txtNombreCliente).textSize = fT
        findViewById<TextView>(R.id.txtDireccionCliente).textSize = fT
        findViewById<TextView>(R.id.txtTelefonoCliente).textSize = fT

    }
    private fun pintarTablaDetalleSinJson() {
        val parametros = this.intent.extras
        val nombreCliente = parametros!!.getString("nombreCliente")
        //se obtiene la tabla
        val tabla = findViewById<TableLayout>(R.id.tblDetalle)
        val fTr = 18F
        //ENCABEZADO
        val trEn = TableRow(this)
        val txtRo = TextView(this)
        txtRo.text = "Datos del Crédito"
        txtRo.setPadding(30, 20, 30, 20)
        txtRo.gravity = Gravity.CENTER
        //txtRo.maxWidth = 50
        txtRo.setTextColor(Color.WHITE)
        txtRo.setTypeface(null, Typeface.BOLD_ITALIC)
        txtRo.textSize = 20F
        trEn.gravity = Gravity.CENTER
        trEn.setBackgroundResource(R.drawable.redondo_verde)
        trEn.addView(txtRo)

        tabla.addView(trEn, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        //PRESTAMO
        val trP = TableRow(this)
        trP.setBackgroundResource(R.drawable.borde)
        val prest = TextView(this)
        prest.text = "Prestamo"
        prest.setPadding(25, 20, 25, 20)
        prest.setTextColor(resources.getColor(R.color.Azul1))
        prest.setTypeface(null, Typeface.BOLD)
        prest.textSize = fTr
        trP.addView(prest)

        val txtPrestamo = TextView(this)
        txtPrestamo.text = formatPesos.format(20000)
        txtPrestamo.setPadding(25, 20, 25, 20)
        txtPrestamo.setTextColor(resources.getColor(R.color.Azul1))
        txtPrestamo.setTypeface(null, Typeface.BOLD)
        txtPrestamo.textSize = fTr
        trP.addView(txtPrestamo)
        trP.gravity = Gravity.CENTER
        tabla.addView(trP, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        //MONTO A PAGAR
        val trMp = TableRow(this)
        trMp.setBackgroundResource(R.drawable.borde)
        val Mp = TextView(this)
        Mp.text = "Monto a Pagar"
        Mp.setPadding(25, 20, 25, 20)
        Mp.setTextColor(resources.getColor(R.color.Azul1))
        Mp.setTypeface(null, Typeface.BOLD)
        Mp.textSize = fTr
        trMp.addView(Mp)

        val txtMp = TextView(this)
        txtMp.text = formatPesos.format(24256)
        txtMp.setPadding(25, 20, 25, 20)
        txtMp.setTextColor(resources.getColor(R.color.Azul1))
        txtMp.setTypeface(null, Typeface.BOLD)
        txtMp.textSize = fTr
        trMp.addView(txtMp)
        trMp.gravity = Gravity.CENTER
        tabla.addView(trMp, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        //MONTO PAGADO
        val trMtop = TableRow(this)
        trMtop.setBackgroundResource(R.drawable.borde)
        val mtop = TextView(this)
        mtop.text = "Monto Pagado"
        mtop.setPadding(25, 20, 25, 20)
        mtop.setTextColor(resources.getColor(R.color.Azul1))
        mtop.setTypeface(null, Typeface.BOLD)
        mtop.textSize = fTr
        trMtop.addView(mtop)

        val txtMtoP = TextView(this)
        txtMtoP.text = formatPesos.format(19768)
        txtMtoP.setPadding(25, 20, 25, 20)
        txtMtoP.setTextColor(resources.getColor(R.color.Azul1))
        txtMtoP.setTypeface(null, Typeface.BOLD)
        txtMtoP.textSize = fTr
        trMtop.addView(txtMtoP)
        trMtop.gravity = Gravity.CENTER
        tabla.addView(trMtop, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        //SALDO VENCIDO
        val trSV = TableRow(this)
        trSV.setBackgroundResource(R.drawable.borde)
        val sv = TextView(this)
        sv.text = "Saldo vencido"
        sv.setPadding(25, 20, 25, 20)
        sv.setTextColor(resources.getColor(R.color.Azul1))
        sv.setTypeface(null, Typeface.BOLD)
        sv.textSize = fTr
        trSV.addView(sv)

        val txtSV = TextView(this)
        txtSV.text = formatPesos.format(0)
        txtSV.setTextColor(resources.getColor(R.color.Azul1))
        txtSV.setPadding(25, 20, 25, 20)
        txtSV.setTypeface(null, Typeface.BOLD)
        txtSV.textSize = fTr
        txtSV.gravity = Gravity.CENTER
        var dias_atraso = 0
        //solo de pruebas
        if (nombreCliente == "Delgadillo Lara Martha" || nombreCliente == "Campos Maysen Sonia" ){
            txtSV.text = formatPesos.format(4488)
            txtSV.setTextColor(Color.RED)
            dias_atraso = 2
        }
        trSV.addView(txtSV)
        trSV.gravity = Gravity.CENTER
        tabla.addView(trSV, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        //DIAS ATRASO
        val trDA = TableRow(this)
        val txtda = TextView(this)
        txtda.text = "Días de atraso"
        txtda.setPadding(25, 20, 25, 20)
        txtda.gravity = Gravity.LEFT
        txtda.setTextColor(resources.getColor(R.color.Azul1))
        txtda.setTypeface(null, Typeface.BOLD)
        txtda.textSize = fTr
        trDA.addView(txtda)


        val txtDa = TextView(this)
        txtDa.text = "$dias_atraso"
        txtDa.setPadding(25, 20, 25, 20)
        txtDa.setTextColor(resources.getColor(R.color.Azul1))
        txtDa.setTypeface(null, Typeface.BOLD)
        txtDa.textSize = fTr
        txtDa.gravity = Gravity.CENTER

        if (dias_atraso != 0){
            txtDa.setTextColor(Color.RED)
        }

        trDA.addView(txtDa)
        trDA.gravity = Gravity.CENTER
        trDA.setBackgroundResource(R.drawable.borde_redondeado_verde)
        tabla.addView(trDA, TableLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }
*/

}