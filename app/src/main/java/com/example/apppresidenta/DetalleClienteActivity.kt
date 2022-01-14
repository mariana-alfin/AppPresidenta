package com.example.apppresidenta

import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import android.widget.TableLayout.LayoutParams.*
import java.text.NumberFormat
import java.util.*

class DetalleClienteActivity : AppCompatActivity() {

    //FORMATO EN PESOS MXM
    private val mx = Locale("es", "MX")
    private val formatPesos: NumberFormat = NumberFormat.getCurrencyInstance(mx)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_cliente)
        //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this,"true")
        findViewById<ImageView>(R.id.imaCel).setColorFilter(Color.GREEN)
        mostrarDatos()
        pintarTablaPrestamo()
    }

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

    private fun pintarTablaPrestamo() {
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


}