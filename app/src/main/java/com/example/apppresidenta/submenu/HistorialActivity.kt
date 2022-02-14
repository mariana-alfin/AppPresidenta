package com.example.apppresidenta.submenu

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.R

class HistorialActivity : AppCompatActivity() {
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
        FuncionesGlobales.guardarPestanaSesion(this,"true")
        mostrarHistorial()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val presidenta = prefs.getString("PRESIDENTA","SIN NOMBRE")
         findViewById<TextView>(R.id.txtPresidenta).text = presidenta

    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return false
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
            txtCiclo.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
            txtCiclo.textSize = fontTr
            //linea.setBackgroundResource(borde_redondeado_verde)
            linea.addView(txtCiclo)

            val txtGpo = TextView(this)
            txtGpo.text = "Arbol de vida"
            txtGpo.setPadding(5, 20, 5, 20)
            txtGpo.gravity = Gravity.CENTER
            txtGpo.setTextColor(ContextCompat.getColor(this,R.color.Azul1))
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
                TableLayout.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            )
            tabla.isShrinkAllColumns = false

        }
    }
}