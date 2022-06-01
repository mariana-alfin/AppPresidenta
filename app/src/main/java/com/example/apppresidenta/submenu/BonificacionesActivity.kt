package com.example.apppresidenta.submenu

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import com.example.apppresidenta.R
import com.example.apppresidenta.generales.FuncionesGlobales

class BonificacionesActivity : AppCompatActivity() {
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bonificaciones_activity)
        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "true")
        /*SE AGREGA LOGO Y TITULO DEL LA ACTIVIDAD*/
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = HtmlCompat.fromHtml("<font color='#FFFFFF' face='montserrat_extra_bold_italic'>Mis Bonificaciones</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        //supportActionBar?.title = HtmlCompat.fromHtml("<font color='#FFFFFF'>Bienvenida</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_v6))
        mostrarDatos()
    }

    @SuppressLint("ResourceAsColor")
    private fun mostrarDatos() {
        //SE OBTIENE LA TABLA
        val tabla = findViewById<TableLayout>(R.id.tblBonificaciones)
        tabla.removeAllViews()
        //ENCABEZADO
        val fontTh = 18F
        val fontTr = 17F

        val trEn = TableRow(this)
        trEn.setPadding(0, 20, 0, 20)

        val e0 = TextView(this)
        e0.textSize = fontTr
        e0.text = "_"
        e0.alpha = 0.0F
        trEn.addView(e0)
        //val tipoLetra = Typeface.MONOSPACE
        val tipoLetra = ResourcesCompat.getFont(this, R.font.montserrat_medium_italic)
        val tipoLetraTr = ResourcesCompat.getFont(this, R.font.montserrat_semi_bold_italic)
        val fecha = TextView(this)
        fecha.text = "FECHA"
        fecha.gravity = Gravity.CENTER
        fecha.setTextColor(Color.WHITE)
        fecha.setTypeface(null, Typeface.BOLD_ITALIC)
        fecha.textSize = fontTh
        fecha.typeface = tipoLetra
        trEn.addView(fecha)

        val e = TextView(this)
        e.textSize = fontTr
        e.text = "___"
        e.alpha = 0.0F
        trEn.addView(e)

        val estatus = TextView(this)
        estatus.text = "   ESTATUS \t\t\t"
        estatus.gravity = Gravity.CENTER
        estatus.setTextColor(Color.WHITE)
        estatus.setTypeface(null, Typeface.BOLD_ITALIC)
        estatus.textSize = fontTh
        estatus.typeface = tipoLetra
        trEn.addView(estatus)

        trEn.gravity = Gravity.CENTER
        trEn.setBackgroundResource(R.drawable.borde_verde5)
        tabla.addView(
            trEn, TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT)
        )
        val numPagos = 9
        for (i in 0 until numPagos) {

            val tr = TableRow(this)
            tr.setPadding(10, 20, 0, 20)

            val colorAzul = ContextCompat.getColor(this, R.color.Azul3)
            val e0 = TextView(this)
            e0.setTextColor(colorAzul)
            e0.textSize = fontTr
            e0.text = "___"
            e0.alpha = 0.0F
            tr.addView(e0)

            if (((i+1)/2.0).toString().contains(".0")){
                tr.setBackgroundResource(R.drawable.color_tr)
            }

            val fec = TextView(this)
            fec.setTextColor(colorAzul)
            fec.textSize = fontTr
            fec.text = "1$i/10/2021"
            fec.gravity = Gravity.CENTER
            fec.typeface = tipoLetraTr
            tr.addView(fec)

            val e = TextView(this)
            e.setTextColor(colorAzul)
            e.textSize = fontTr
            e.text = "___"
            e.alpha = 0.0F
            tr.addView(e)

            val stats = TextView(this)
            stats.setTextColor(colorAzul)
            stats.textSize = fontTr
            stats.text = "PAGADA"
            if(i == 2 ||i == 4 ||i == 5 ||i == 8){
                stats.text = "POR PAGAR"
            }

            stats.gravity = Gravity.CENTER
            stats.typeface = tipoLetraTr
            tr.addView(stats)

            //tr.setBackgroundResource(R.drawable.borde_azul_b)
            tr.gravity = Gravity.CENTER
            tabla.addView(tr)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        finish()
        return false
    }
}

