package com.example.apppresidenta.submenu

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.example.apppresidenta.R
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.FuncionesGlobales.Companion.setMaxLength
import java.util.*


class CalculadoraActivity : AppCompatActivity(), OnItemSelectedListener {
    //MD FORMATO EN PESOS MXM

    var numPagos = 16
    val listPagos: MutableList<String> = ArrayList()
    var listOpciones = arrayOf("Sí", "No")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calculadora_activity)

        for (i in 1..numPagos) {
            listPagos.add("$i")
        }
        /*MD SE AGREGA LOGO Y TITULO DEL LA ACTIVIDAD*/
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = HtmlCompat.fromHtml("<font color='#FFFFFF'>Calculadora</font>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_v6))

        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "true")
        pintarTablaBonificacion()
        pintarCalculadoraBonificacion()
        findViewById<TextView>(R.id.txtNota).text =
            HtmlCompat.fromHtml("<font color='#3C8943'>Nota:</font><font color='#000000'> Recuerda que si el prestamo no se liquida se perdera la bolsa acumulada.</font>", HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return false
    }
    private fun pintarTablaBonificacion() {
        //MD SE OBTIENE LA TABLA
        val tabla = findViewById<TableLayout>(R.id.tblBonificacion)
        val fTr = 16F
        val color = Color.BLACK
        val gravityTxt = Gravity.LEFT
        val topBottom = 10
        val left = 100
        //Bonificacion Semanal
        val trP = TableRow(this)
        trP.setPadding(25, topBottom, 20, topBottom)
        //trP.setBackgroundResource(R.drawable.borde)

        val bs = TextView(this)
        bs.text = "Bonificacion Semanal"
        bs.setTextColor(color)
        bs.textSize = fTr

        val txtBonS = TextView(this)
        txtBonS.text = FuncionesGlobales.convertPesos(750.0,2)
        txtBonS.setTextColor(color)
        txtBonS.setPadding(left, 0, 5, 0)
        txtBonS.gravity = gravityTxt
        txtBonS.setTypeface(null, Typeface.BOLD)
        txtBonS.textSize = fTr

        trP.addView(bs)
        trP.addView(txtBonS)
        tabla.addView(
            trP, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
            )
        )

        //Bonificación, pago semanal
        val trB = TableRow(this)
        trB.setPadding(25, topBottom, 20, topBottom)

        val Mp = TextView(this)
        Mp.text = "Bonificación, pago semanal"
        Mp.setTextColor(ContextCompat.getColor(this,R.color.Verde1))
        Mp.textSize = fTr

        val txtMp = TextView(this)
        txtMp.text = FuncionesGlobales.convertPesos(24256.0,0)
        txtMp.setPadding(left, 0, 5, 0)
        txtBonS.gravity = gravityTxt
        txtMp.setTextColor(ContextCompat.getColor(this,R.color.Verde1))
        txtMp.textSize = fTr

        trB.addView(Mp)
        trB.addView(txtMp)
        tabla.addView(
            trB, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
            )
        )

        //Bolsa acumulada
        val trB2 = TableRow(this)
        trB2.setPadding(25, topBottom, 20, topBottom)

        val Ba = TextView(this)
        Ba.text = "*Bolsa acumulada"
        Ba.setTextColor(color)
        Ba.textSize = fTr

        val txtBa = TextView(this)
        txtBa.text = FuncionesGlobales.convertPesos(19768.0,0)
        txtBa.setPadding(left, 0, 5, 0)
        txtBa.gravity = gravityTxt
        txtBa.setTextColor(color)
        txtBa.textSize = fTr

        trB2.addView(Ba)
        trB2.addView(txtBa)
        tabla.addView(
            trB2, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.MATCH_PARENT
            )
        )

        //Bolsa acumulada nota
        val row = TableRow(this)
        row.setPadding(25, topBottom, 20, topBottom)
        val layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
        layoutParams.span = 4 // MD DEFINE CUANTAS COLUMNAS DE LA TABLA USARA EL ROW
        row.layoutParams = layoutParams

        val txt = TextView(this)
        txt.text = "*Bolsa acumulada - se entregará al final del préstamo."
        txt.gravity = Gravity.LEFT
        txt.textSize = fTr
        txt.setTextColor(color)
        txt.layoutParams = layoutParams

        row.addView(txt)
        tabla.addView(row)
    }

    private fun pintarCalculadoraBonificacion() {
        //MD SE OBTIENE LA TABLA
        val tabla = findViewById<TableLayout>(R.id.tblCalculadora)
        tabla.setPadding(60, 0, 50, 0)
        tabla.gravity = Gravity.CENTER_VERTICAL
        val fTr = 16F
        val color = Color.BLACK
        val gravityTxt = Gravity.LEFT
        val gravityTxtC = Gravity.CENTER
        val topBottom = 10
        val leftRigth = 10
        //ENCABEZADO
        val layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
        layoutParams.span = 4

        val txt = TextView(this)
        txt.text = "Calcula tu bonificación"
        txt.setPadding(0, 15, 0, 15)
        txt.gravity = Gravity.CENTER
        txt.textSize = 20F
        txt.setBackgroundResource(R.drawable.redondo_verde)
        txt.setTextColor(Color.WHITE)
        txt.layoutParams = layoutParams

        val row = TableRow(this)
        row.setPadding(0, 0, 0, 0)
        row.layoutParams = layoutParams
        row.addView(txt)
        tabla.addView(row)

        val l = LinearLayout(this)
        l.setBackgroundResource(R.drawable.borde_celda)
        l.setPadding(leftRigth, 20, leftRigth, 20)
        val ps = TextView(this)
        ps.text = "Pago Semanal"
        ps.setTextColor(color)
        ps.textSize = fTr
        l.addView(ps)

        val l2 = LinearLayout(this)
        l2.setBackgroundResource(R.drawable.borde_celda)
        l2.gravity = gravityTxtC
        val txtPsP = EditText(this)
        txtPsP.hint = "$25,000"
        txtPsP.setPadding(5, 20, 10, 20)
        txtPsP.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        txtPsP.keyListener = DigitsKeyListener.getInstance(".0123456789")
        txtPsP.setTextColor(color)
        txtPsP.gravity = gravityTxtC
        txtPsP.textSize = fTr
        txtPsP.setMaxLength(10)
        l2.addView(txtPsP)

        val trP = TableRow(this)
        trP.addView(l)
        trP.addView(l2)
        tabla.addView(
            trP, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
            )
        )

        val l3 = LinearLayout(this)
        l3.setBackgroundResource(R.drawable.borde_celda)
        l3.setPadding(leftRigth, topBottom, leftRigth, topBottom)
        val pt = TextView(this)
        pt.text = "Pago a tiempo"
        pt.setTextColor(color)
        pt.textSize = fTr
        l3.addView(pt)

        val l4 = LinearLayout(this)
        l4.setBackgroundResource(R.drawable.borde_celda)
        l4.setPadding(leftRigth, topBottom, leftRigth, 4)
        l4.gravity = gravityTxtC
        val spinnerPagos = Spinner(this)
        // MD CREA UN ARREGLO CON LAS OPCIONES PARA ENVIARLO AL COMBO DEL DIALOG
        val opciones = ArrayAdapter(this, android.R.layout.simple_spinner_item, listPagos)
        opciones.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // MD SE ASIGNA EL ARREGLO AL SPINER
        spinnerPagos.adapter = opciones
        spinnerPagos.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                arg0: AdapterView<*>?,
                arg1: View?,
                position: Int,
                id: Long
            ) {
                //Toast.makeText(baseContext,"se selecciono "+ listPagos[position], Toast.LENGTH_SHORT).show()
                findViewById<TextView>(R.id.txtPgos).text = listPagos[position]
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {
                // TODO Auto-generated method stub
            }
        }
        l4.addView(spinnerPagos)

        val trB = TableRow(this)
        trB.addView(l3)
        trB.addView(l4)
        tabla.addView(
            trB, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
            )
        )

        val l5 = LinearLayout(this)
        l5.setBackgroundResource(R.drawable.borde_celda)
        l5.setPadding(leftRigth, topBottom, leftRigth, topBottom + 1)
        l5.gravity = gravityTxt
        val pl = TextView(this)
        pl.text = "Prestamo Liquidado"
        pl.setTextColor(color)
        pl.textSize = fTr
        l5.addView(pl)

        val l6 = LinearLayout(this)
        l6.setBackgroundResource(R.drawable.borde_celda)
        l6.setPadding(0, topBottom, 0, 4)
        l6.gravity = gravityTxtC
        val spinnerPL = Spinner(this)
        val opciones2 = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOpciones)
        opciones2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPL.adapter = opciones2
        spinnerPL.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                arg0: AdapterView<*>?,
                arg1: View?,
                position: Int,
                id: Long
            ) {
                //Toast.makeText(baseContext,"se "+ listOpciones[position], Toast.LENGTH_SHORT).show()
                findViewById<TextView>(R.id.txtPL).text = listOpciones[position]
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {
                // TODO Auto-generated method stub
            }
        }
        l6.addView(spinnerPL)

        val trB2 = TableRow(this)
        trB2.addView(l5)
        trB2.addView(l6)
        tabla.addView(
            trB2, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
            )
        )

        val trB3 = TableRow(this)
        val l7 = LinearLayout(this)
        l7.setPadding(0, topBottom, 0, topBottom)
        l7.layoutParams = layoutParams

        trB3.addView(l7)
        tabla.addView(
            trB3, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
            )
        )

        val l8 = LinearLayout(this)
        l8.setBackgroundResource(R.drawable.borde_celda)
        l8.setPadding(leftRigth, topBottom, leftRigth, topBottom)
        l8.gravity = gravityTxt
        val bsa = TextView(this)
        bsa.text = "Bolsa semanal acumulada"
        bsa.setTextColor(ContextCompat.getColor(this,R.color.Verde1))
        bsa.textSize = fTr
        l8.addView(bsa)

        val l9 = LinearLayout(this)
        l9.setBackgroundResource(R.drawable.borde_celda)
        l9.setPadding(0, topBottom, 0, topBottom)
        l9.gravity = gravityTxtC
        val txtBsa = TextView(this)
        txtBsa.text =FuncionesGlobales.convertPesos(12000.0,0)
        txtBsa.setTextColor(ContextCompat.getColor(this,R.color.Verde1))
        txtBsa.textSize = fTr
        l9.addView(txtBsa)

        val trB4 = TableRow(this)
        trB4.addView(l8)
        trB4.addView(l9)
        tabla.addView(
            trB4, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
            )
        )

        val l10 = LinearLayout(this)
        l10.setBackgroundResource(R.drawable.borde_celda)
        l10.setPadding(leftRigth, topBottom, leftRigth, topBottom)
        l10.gravity = gravityTxt
        val ba = TextView(this)
        ba.text = "Bolsa Acumulada"
        ba.setTextColor(color)
        ba.textSize = fTr
        l10.addView(ba)

        val l11 = LinearLayout(this)
        l11.setBackgroundResource(R.drawable.borde_celda)
        l11.setPadding(0, topBottom, 0, topBottom)
        l11.gravity = gravityTxtC
        val txtBa = TextView(this)
        txtBa.text = FuncionesGlobales.convertPesos(4000.0,0)
        txtBa.setTextColor(color)
        txtBa.textSize = fTr
        l11.addView(txtBa)

        val trB5 = TableRow(this)
        trB5.addView(l10)
        trB5.addView(l11)
        tabla.addView(
            trB5, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
            )
        )

        val l12 = LinearLayout(this)
        l12.setPadding(leftRigth, topBottom, leftRigth, topBottom)
        l12.gravity = gravityTxt
        val b = TextView(this)
        b.text = "Bonificación Total"
        b.setTypeface(null, Typeface.BOLD_ITALIC)
        b.setTextColor(Color.WHITE)
        b.textSize = fTr + 2
        l12.addView(b)

        val l13 = LinearLayout(this)
        l13.setPadding(0, topBottom, 0, topBottom)
        l13.gravity = gravityTxtC
        val txtBonificacion = TextView(this)
        txtBonificacion.text = FuncionesGlobales.convertPesos(16000.0,0)
        txtBonificacion.setTypeface(null, Typeface.BOLD_ITALIC)
        txtBonificacion.setTextColor(Color.WHITE)
        txtBonificacion.textSize = fTr + 2
        l13.addView(txtBonificacion)

        val trB6 = TableRow(this)
        l12.setBackgroundResource(R.drawable.redondo_verde_1)
        l13.setBackgroundResource(R.drawable.redondo_verde_2)
        trB6.addView(l12)
        trB6.addView(l13)
        tabla.addView(
            trB6, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
            )
        )
    }
    //MD FUNCION DE ITEM SELECCIONADO
    override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, id: Long) {
        Toast.makeText(this, "Selected : $id ", Toast.LENGTH_SHORT).show()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }
}
/*
fun pagosAtiempo(idTxt: Int){
    //se genera la cadena
    var numPagos = 16
    val txtPagos = findViewById<TextView>(idTxt)
    val list: MutableList<String> = ArrayList()
    for (i in 1..numPagos){
        list.add("$i")
    }
    // add a list
    //val animals = arrayOf("horse", "cow", "camel", "sheep", "goat")
    // setup the alert builder
    val builder = AlertDialog.Builder(this)
    builder.setIcon(R.drawable.ic_alert)
    builder.setTitle("Selecciona el número de pagos realizados a tiempo")
    builder.setItems(list.toTypedArray()) { _, which ->
        //Toast.makeText(this,"Valor = $which", Toast.LENGTH_SHORT).show()
        txtPagos.text = "${which+1}"
        /*
        when (which) {
            0 -> { /* horse */ }
            1 -> { /* cow   */ }
            2 -> { /* camel */ }
            3 -> { /* sheep */ }
            4 -> { /* goat  */ }
        */
    }
// create and show the alert dialog
    val dialog = builder.create()
    dialog.show()
}
fun opcionesSiNo(seccion: String,idTxt: Int){
    val txt = findViewById<TextView>(idTxt)
    val opciones = arrayOf("Sí", "No")
    var title = "¿Prestamo liquidado?"
    if(seccion == "BA"){
        title = "¿Bolsa Acumulada?"
    }
    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
    builder.setItems(opciones) { _, which ->
        //Toast.makeText(this,"Valor = $which", Toast.LENGTH_SHORT).show()
        txt.text = "${which + 1}"

        when (which) {
            0 -> { txt.text = "Sí" }
            1 -> { txt.text = "No" }
        }
    }
    val dialog = builder.create()
    dialog.show()
}
*/
