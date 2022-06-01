package com.example.apppresidenta.navegacion

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.example.apppresidenta.R
import com.example.apppresidenta.databinding.BonosFragmentBinding
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.ValGlobales
import com.google.android.material.progressindicator.CircularProgressIndicator


class BonosFragment : Fragment() {

    private var _binding: BonosFragmentBinding? = null
    lateinit var progressBar: CircularProgressIndicator

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        _binding = BonosFragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //MD INDICA QUE SE HABILITARA EL MENU DE OPCIONES
        setHasOptionsMenu(true)
        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(activity as AppCompatActivity, "true")
        //mostrarFormato(false)
        if (ValGlobales.validarConexion(activity as AppCompatActivity)) {
            mostrarDatos()

        }/* else {
            binding.txtCargando.text = getString(R.string.noConexion)
            binding.txtCargando.gravity = Gravity.CENTER
            binding.txtCargando.visibility = View.VISIBLE
            progressBar = binding.cargando
            progressBar.visibility = View.INVISIBLE
        }*/
        return root
    }

    @SuppressLint("ResourceAsColor")
    private fun mostrarDatos() {
        //SE OBTIENE LA TABLA
        val tabla = binding.tblBonificaciones
        tabla.removeAllViews()
        //ENCABEZADO
        val fontTh = 18F
        val fontTr = 17F
        val context = requireActivity()
        val trEn = TableRow(context)
        trEn.setPadding(0, 20, 0, 20)

        val e0 = TextView(context)
        e0.textSize = fontTr
        e0.text = "_"
        e0.alpha = 0.0F
        trEn.addView(e0)

        //val tipoLetra = Typeface.MONOSPACE
        val tipoLetra = ResourcesCompat.getFont(context, R.font.montserrat_medium_italic)
        val tipoLetraTr = ResourcesCompat.getFont(context, R.font.montserrat_semi_bold_italic)
        val fecha = TextView(context)
        fecha.text = "FECHA"
        fecha.gravity = Gravity.CENTER
        fecha.setTextColor(Color.WHITE)
        fecha.setTypeface(null, Typeface.BOLD_ITALIC)
        fecha.textSize = fontTh
        fecha.typeface = tipoLetra
        trEn.addView(fecha)

        val e = TextView(context)
        e.textSize = fontTr
        e.text = "___"
        e.alpha = 0.0F
        trEn.addView(e)

        val estatus = TextView(context)
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
            trEn,
            TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT)
        )
        val numPagos = 9
        for (i in 0 until numPagos) {

            val tr = TableRow(context)
            tr.setPadding(10, 20, 0, 20)

            val colorAzul = ContextCompat.getColor(context, R.color.Azul3)
            val e0 = TextView(context)
            e0.setTextColor(colorAzul)
            e0.textSize = fontTr
            e0.text = "___"
            e0.alpha = 0.0F
            tr.addView(e0)

            if (((i + 1) / 2.0).toString().contains(".0")) {
                tr.setBackgroundResource(R.drawable.color_tr)
            }

            val fec = TextView(context)
            fec.setTextColor(colorAzul)
            fec.textSize = fontTr
            fec.text = "1$i/10/2021"
            fec.gravity = Gravity.CENTER
            fec.typeface = tipoLetraTr
            tr.addView(fec)

            val e = TextView(context)
            e.setTextColor(colorAzul)
            e.textSize = fontTr
            e.text = "___"
            e.alpha = 0.0F
            tr.addView(e)

            val stats = TextView(context)
            stats.setTextColor(colorAzul)
            stats.textSize = fontTr
            stats.text = "PAGADA"
            if (i == 2 || i == 4 || i == 5 || i == 8) {
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
    //MD AGREGA EL MENU DE OPCIONES A LA VISTA
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        (activity as AppCompatActivity).supportActionBar?.title = HtmlCompat.fromHtml("<font face='montserrat_extra_bold_italic'>Mis Bonificaciones</font>",
            HtmlCompat.FROM_HTML_MODE_LEGACY)
        //(activity as AppCompatActivity).supportActionBar?.setBackgroundDrawable(getDrawable((activity as AppCompatActivity),R.drawable.barra_v6))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
