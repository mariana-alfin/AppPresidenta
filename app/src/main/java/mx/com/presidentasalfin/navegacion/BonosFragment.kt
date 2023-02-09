package mx.com.presidentasalfin.navegacion

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import mx.com.presidentasalfin.R
import mx.com.presidentasalfin.databinding.BonosFragmentBinding
import mx.com.presidentasalfin.generales.FuncionesGlobales
import mx.com.presidentasalfin.generales.FuncionesGlobales.Companion.convertPesos
import mx.com.presidentasalfin.generales.ValGlobales
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*


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
        binding.lbl.visibility = View.GONE
        if (ValGlobales.validarConexion(activity as AppCompatActivity)) {
            try {
                val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                val prestamo = prefs.getInt("CREDITO_ID", 0)
                val numPagoActual = prefs.getInt("NO_PAGO_ACTUAL", 0)
                datosBonificaciones(prestamo, numPagoActual)

            } catch (e: Exception) {
                progressBar = binding.cargando
                progressBar.visibility = View.INVISIBLE
                binding.lbl.visibility = View.VISIBLE
            }

        } else {
            binding.txtCargando.text = getString(R.string.noConexion)
            binding.txtCargando.gravity = Gravity.CENTER
            binding.txtCargando.visibility = View.VISIBLE
            progressBar = binding.cargando
            progressBar.visibility = View.INVISIBLE
        }
        return root
    }


    private fun datosBonificaciones(prestamo: Int, numPagoActual: Int) {
        /**************   MD  ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alertError = FuncionesGlobales.mostrarAlert(
            requireActivity(),
            "error",
            true,
            "Obtener Pagos",
            getString(R.string.error),
            false
        )
        try {
            val jsonParametros = JSONObject()
            jsonParametros.put("credit_id", prestamo)

            val request =
                @SuppressLint("SetTextI18n") //<-- se agrega para admitir una variedad de configuraciones regionales sin tener que modificar código en la concatenacion de cadenas
                object : JsonObjectRequest(
                    Method.POST,
                    //getString(R.string.urlBonificaciones),
                    getString(R.string.url)+getString(R.string.metBonificaciones),
                    jsonParametros,
                    Response.Listener { response ->
                        try {
                            //Obtiene su respuesta json
                            val jsonData =
                                JSONTokener(response.getString("data")).nextValue() as JSONObject
                            if (jsonData.getInt("code") == 200) {
                                val jsonResults = jsonData.getJSONArray("results")
                                mostrarDatosBonificacion(numPagoActual, jsonResults)
                            } else {
                                alertError.show()
                            }
                        } catch (e: Exception) {
                            if (e.message != null) {
                                alertError.show()
                            }
                        }
                    },
                    Response.ErrorListener { error ->
                        //MD MANEJO DE ERROES EN LA RESPUESTA
                        var mensaje = getString(R.string.error)
                        try {
                            mensaje =
                                "Por el momento no es posible mostrar su tabla de bonificaciones, favor de revisarlo con su asesor."

                        } catch (e: Exception) {
                            val codigo = error.networkResponse.statusCode
                            mensaje = "Error: $codigo \n${getString(R.string.errorServidor)}"

                        }
                        progressBar = binding.cargando
                        progressBar.visibility = View.INVISIBLE
                        binding.txtCargando.text = mensaje
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
            val queue = Volley.newRequestQueue(activity)
            //MD PRIMERO BORRAMOS EL CACHE Y ENVIAMOS DESPUES LA PETICION
            queue.cache.clear()
            queue.add(request)
        } catch (e: java.lang.Exception) {
            alertError.show()
        }
        /*******  FIN ENVIO   *******/
    }

    @SuppressLint("SetTextI18n")
    private fun mostrarDatosBonificacion(numPagoActual: Int, jsonResults: JSONArray) {
        val alertError = FuncionesGlobales.mostrarAlert(
            requireActivity(),
            "error",
            true,
            "Mis Bonificaciones",
            getString(R.string.error),
            false
        )
        try {
            val WidthDp = this.resources.configuration.screenWidthDp
            var fontTh = 18F
            var fontTr = 17F
            var witCte = 200
            var nTabs = "\t\t"

            /**     VARIABLES PARA TAMAÑOS      **/
            //MD SE OBTIENE LA DENSIDAD DEL DISPOSITIVO PARA CAMBIAR LOS TAMAÑOS
            val densidad = resources.displayMetrics.densityDpi
            val width = resources.displayMetrics.widthPixels
            if (densidad < DisplayMetrics.DENSITY_HIGH) {
                fontTh = 15F
                fontTr = 14F
                nTabs = "\t"
            } else if (densidad in DisplayMetrics.DENSITY_HIGH until DisplayMetrics.DENSITY_XHIGH && width > 400) {
                witCte = (width / 4)
                fontTh = 15F
                fontTr = 14F
                nTabs = "\t"
            } else if (densidad in DisplayMetrics.DENSITY_HIGH until DisplayMetrics.DENSITY_XHIGH && width < 400) {
                witCte = 300
                fontTh = 16F
                fontTr = 17F
                nTabs = "\t"
            } else if (densidad == DisplayMetrics.DENSITY_XHIGH) {
                witCte = 250
                fontTh = 16F
                fontTr = 17F
            } else if (densidad in DisplayMetrics.DENSITY_XHIGH until DisplayMetrics.DENSITY_XXHIGH) {
                witCte = 380
                fontTh = 16F
                fontTr = 17F
            } else if ((densidad in DisplayMetrics.DENSITY_XXHIGH until DisplayMetrics.DENSITY_XXXHIGH) && width < 1200) {
                witCte = 415
                fontTh = 17F
                fontTr = 18F
            } else if ((densidad >= DisplayMetrics.DENSITY_XXXHIGH) || width >= 1200) {
                witCte = 480
            } else {//MD SI LA PANTALLA ES MAYOR A 1200PX EL VALOR DEL NOMBRE SERA DE UNA TERCERA PARTE
                witCte = (width / 3)
            }
            /******      DISEÑO DE LA TABLA       **********/
            //SE OBTIENE LA TABLA
            val tabla = binding.tblBonificaciones
            tabla.removeAllViews()
            //ENCABEZADO
            val tipoLetraTr =
                ResourcesCompat.getFont(requireContext(), R.font.montserrat_semi_bold_italic)
            val tipoLetra =
                ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium_italic)

            val colorTh = ContextCompat.getColor(requireContext(), R.color.Verde5)
            val colorAzul = ContextCompat.getColor(requireContext(), R.color.Azul3)
            val trEn = TableRow(context)
            trEn.setPadding(10, 10, 10, 10)

            val ll = LinearLayout(context)
            ll.setPadding(20, 20, 20, 20)
            /*************/
            val e0 = TextView(context)
            e0.textSize = fontTh
            e0.text = "_"
            e0.alpha = 0.0F
            trEn.addView(e0)

            val fecha = TextView(context)
            fecha.text = "Ficha"
            fecha.gravity = Gravity.CENTER
            fecha.setTextColor(colorTh)
            fecha.textSize = fontTh
            fecha.typeface = tipoLetraTr
            ll.addView(fecha)

            val m = TextView(context)
            m.textSize = fontTh
            m.text = "___"
            m.alpha = 0.0F
            ll.addView(m)

            val montoBno = TextView(context)
            montoBno.text = "Bono"
            montoBno.gravity = Gravity.RIGHT
            montoBno.setTextColor(colorTh)
            montoBno.textSize = fontTh
            montoBno.typeface = tipoLetraTr
            ll.addView(montoBno)

            val e = TextView(context)
            e.textSize = fontTh
            e.text = "___"
            e.alpha = 0.0F
            ll.addView(e)

            val estatus = TextView(context)
            estatus.text = "Estatus $nTabs"
            estatus.gravity = Gravity.RIGHT
            estatus.setTextColor(colorTh)
            estatus.textSize = fontTh
            estatus.typeface = tipoLetraTr
            ll.addView(estatus)

            val info = TextView(context)
            info.textSize = fontTh
            info.text = "_____$nTabs"
            info.alpha = 0.0F
            ll.addView(info)
            /*************/
            trEn.addView(ll)
            tabla.addView(
                trEn,
                TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.MATCH_PARENT)
            )
            /***********************************************/

            val numPagos = jsonResults.length()
            var semPagadas = 0
            var semPorPagar = 0
            var pagoSemanalPagado = 0.0
            var pagoSemanalPorPagar = 0.0
            var reservaPagada = 0.0
            var reservaPorPagar = 0.0

            //MD FOR DEL NUMERO DE PAGOS
            for (i in 0 until numPagos) {
                //SE GENERA EL OBJETO BONO DEL ARREGLO
                val bono: JSONObject = jsonResults.getJSONObject(i)
                //SI EL NUMERO DE PAGO DEL BONO ES EL NUMERO DE PAGO ACTUAL SE MUESTRA EN LOS DATOS DE BONIFICACION
                if (bono.getInt("pay_number") == numPagoActual) {
                    val bonoTotal = bono.getDouble("bonus_amount")
                    val bonoReserva = bono.getDouble("stock_amount")
                    val pagoSemanal = bonoTotal - bonoReserva
                    binding.txtBonificacion.setText(convertPesos(bonoTotal, 2))
                    binding.txtPagoSemanal.setText(convertPesos(pagoSemanal, 2))
                    binding.txtReserva.setText(convertPesos(bonoReserva, 2))
                }
//SE HACE EL CALCULO DE LAS SEMANAS
                if (bono.getInt("bonus_status_id") == 3) {//Dispersado
                    semPagadas++
                    pagoSemanalPagado += (bono.getDouble("bonus_amount") - bono.getDouble("stock_amount"))
                    reservaPagada += bono.getDouble("stock_amount")
                } else if (bono.getInt("bonus_status_id") != 4 && bono.getInt("bonus_status_id") != 5) {//DIFERENTE DE En revision Y No autorizado
                    semPorPagar++
                    pagoSemanalPorPagar += (bono.getDouble("bonus_amount") - bono.getDouble("stock_amount"))
                    reservaPorPagar += bono.getDouble("stock_amount")
                }


                //SE AGREGAN A LA TABLA
                val trEn = TableRow(context)
                trEn.setPadding(10, 5, 10, 5)

                val ll = LinearLayout(context)
                ll.setPadding(20, 20, 20, 20)
                ll.elevation = 15F

                val e0 = TextView(context)
                e0.textSize = fontTr
                e0.text = "_"
                e0.alpha = 0.0F
                trEn.addView(e0)

                val fecha = TextView(context)
                fecha.text = "$nTabs" + bono.getInt("pay_number")
                fecha.gravity = Gravity.CENTER
                fecha.setTextColor(colorAzul)
                fecha.textSize = fontTh
                fecha.typeface = tipoLetra
                ll.addView(fecha)

                val m = TextView(context)
                m.textSize = fontTr
                m.text = "___"
                m.alpha = 0.0F
                ll.addView(m)

                val montoBno = TextView(context)
                val bonoPagado = bono.getDouble("bonus_amount") - bono.getDouble("stock_amount")
                if (bono.getInt("pay_number") == 1) {
                    montoBno.text = "\t\t " + convertPesos(bonoPagado, 2)
                } else if (bono.getInt("pay_number") == 10) {
                    montoBno.text = "\t" + convertPesos(bonoPagado, 2)
                } else {
                    montoBno.text = "\t\t" + convertPesos(bonoPagado, 2)
                }

                montoBno.gravity = Gravity.RIGHT
                montoBno.setTextColor(colorAzul)
                montoBno.textSize = fontTh
                montoBno.typeface = tipoLetraTr
                ll.addView(montoBno)

                val e = TextView(context)
                e.textSize = fontTr
                e.text = "___"
                e.alpha = 0.0F
                ll.addView(e)

                val estatus = TextView(context)
                estatus.maxWidth = witCte
                //estatus.text = if(bono.getInt("bonus_status_id") == 1) "POR PAGAR" else "PAGADO"
                val estatusBono = when (bono.getInt("bonus_status_id")) {
                    1 -> {
                        "Generado"
                    }
                    2 -> {
                        "Por dispersar"
                    }
                    3 -> {
                        "Dispersado"
                    }
                    4 -> {
                        "En revision"
                    }
                    5 -> {
                        "No autorizado"
                    }
                    else -> {
                        "No autorizado"
                    }
                }
                estatus.text = estatusBono + "$nTabs"
                estatus.gravity = Gravity.RIGHT
                estatus.setTextColor(colorAzul)
                estatus.textSize = fontTh - 1
                estatus.typeface = tipoLetra
                ll.addView(estatus)

                val info = ImageView(activity)
                info.setImageResource(R.drawable.ic_info)
                info.setOnClickListener {
                    verInformacion(bono.getInt("bonus_status_id"),
                        bono.getString("payment_date"),
                        bono.getDouble("bonus_amount"),
                        bono.getDouble("stock_amount"))
                }
                info.foregroundGravity = Gravity.CENTER
                ll.addView(info)
                ll.setBackgroundResource(R.drawable.fondo_row)
                trEn.addView(ll)
                tabla.addView(
                    trEn,
                    TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                        TableLayout.LayoutParams.MATCH_PARENT)
                )
                /*****************/

            }

            var textSize = 18.0F
            if (width != 1080) {
                textSize = (((20 * width) / 1080 + 2).toFloat())
            }
            binding.txtSemanasPagadas.textSize = textSize
            binding.txtSemanasPorPagar.textSize = textSize
            binding.txtPagoSemanalPagado.textSize = textSize
            binding.txtReservaPagada.textSize = textSize
            binding.txtPagoSemanalPorPagar.textSize = textSize
            binding.txtReservaPorPagar.textSize = textSize
            binding.txtTotalPagoSemanal.textSize = textSize
            binding.txtTotalReserva.textSize = textSize
            binding.ttal.textSize = textSize

            binding.txtSemanasPagadas.text =
                "$semPagadas  ${if (semPagadas == 1) "pagada" else "pagadas"}"
            binding.txtSemanasPorPagar.text = "$semPorPagar x obtener"
            binding.txtPagoSemanalPagado.text = convertPesos(pagoSemanalPagado, 2)
            binding.txtReservaPagada.text = convertPesos(reservaPagada, 2)
            binding.txtPagoSemanalPorPagar.text = convertPesos(pagoSemanalPorPagar, 2)
            binding.txtReservaPorPagar.text = convertPesos(reservaPorPagar, 2)
            binding.txtTotalPagoSemanal.text =
                convertPesos((pagoSemanalPagado + pagoSemanalPorPagar), 2)
            binding.txtTotalReserva.text =
                convertPesos((reservaPagada + reservaPorPagar), 2)

            progressBar = binding.cargando
            progressBar.visibility = View.INVISIBLE
            binding.lbl.visibility = View.VISIBLE
        } catch (e: Exception) {
            alertError.show()
        }

    }

    private fun verInformacion(
        estatus: Int,
        fechaPago: String,
        montoTotal: Double,
        reserva: Double,
    ) {
        var detalle = "Bono pagado: ${
            convertPesos(montoTotal - reserva,
                2)
        } \nqueda en Reserva:  ${convertPesos(reserva, 2)} "
        if (estatus == 3) {
            // detalle = "Bonificación pagada el: 24/12/2022 \nMonto del Pago semanal: ${convertPesos(montoTotal-reserva,2)} \nMonto en Reserva: ${convertPesos(reserva,2)} "
            detalle = "Bonificación pagada el: $fechaPago \nBono pagado: ${
                convertPesos(montoTotal - reserva,
                    2)
            } \nqueda en Reserva: ${convertPesos(reserva, 2)} "
        }
        FuncionesGlobales.mostrarAlert(requireActivity(), "info", false, "Información del bono",
            detalle, false).show()
    }


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
        binding.lbl.visibility = View.VISIBLE
    }

    //MD AGREGA EL MENU DE OPCIONES A LA VISTA
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        (activity as AppCompatActivity).supportActionBar?.title =
            HtmlCompat.fromHtml("<font face='montserrat_extra_bold_italic'>Mis Bonificaciones</font>",
                HtmlCompat.FROM_HTML_MODE_LEGACY)
        //(activity as AppCompatActivity).supportActionBar?.setBackgroundDrawable(getDrawable((activity as AppCompatActivity),R.drawable.barra_v6))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
