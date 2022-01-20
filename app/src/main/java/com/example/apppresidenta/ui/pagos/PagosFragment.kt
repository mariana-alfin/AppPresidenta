package com.example.apppresidenta.ui.pagos

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.text.Html
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.FuncionesGlobales
import com.example.apppresidenta.JuntaActivity
import com.example.apppresidenta.R
import com.example.apppresidenta.ValGlobales
import com.example.apppresidenta.databinding.PagosNotificationsBinding
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.text.NumberFormat
import java.util.*

class PagosFragment : Fragment() {

    private lateinit var pagosViewModel: PagosViewModel
    private var _binding: PagosNotificationsBinding? = null

    //FORMATO EN PESOS MXM
    private val mx = Locale("es", "MX")
    private val formatPesos: NumberFormat = NumberFormat.getCurrencyInstance(mx)
    lateinit var progressBar: CircularProgressIndicator

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        pagosViewModel =
            ViewModelProvider(this).get(PagosViewModel::class.java)

        _binding = PagosNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        /* val textView: TextView = binding.textNotifications
         notificationsViewModel.text.observe(viewLifecycleOwner, Observer {
             textView.text = it
         })*/
        //INDICA QUE SE HABILITARA EL MENU DE OPCIONES
        setHasOptionsMenu(true)
        //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(activity as AppCompatActivity,"true")
        //llenarTbPagos()
        mostrarFormato(false)
        if (ValGlobales.validarConexion(activity as AppCompatActivity)) {
            datosPagos()
        } else {
            binding.txtCargando.text = getString(R.string.noConexion)
            binding.txtCargando.gravity = Gravity.CENTER
            binding.txtCargando.visibility = View.VISIBLE
            progressBar = binding.cargando
            progressBar.visibility = View.INVISIBLE
        }
        return root
    }

    private fun mostrarFormato(esMostrar: Boolean) {
        var valor = View.VISIBLE
        var valorLoadi = View.INVISIBLE
        progressBar = binding.cargando

        if (!esMostrar) {
            valor = View.INVISIBLE
            valorLoadi = View.VISIBLE
        }
        progressBar.visibility = valorLoadi
        binding.txtCargando.visibility = valorLoadi
        binding.txtCalendario.visibility = valor
        binding.txtPagoSemanal.visibility = valor
    }

    private fun datosPagos() {
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val prestamo = prefs.getInt("CREDITO_ID", 0)
        //val prestamo = 119483 //para pruebas
        val fecha = "2020-01-17" //para pruebas
        val dialogNo =
            AlertDialog.Builder(requireActivity(), R.style.ThemeOverlay_AppCompat_Dialog_Alert)
                .setTitle(Html.fromHtml("<font color='#3C8943'>Ingresar</font>"))
                .setMessage("OCURRIO UN ERROR, FAVOR DE INTENTARLO MAS TARDE.")
                .setPositiveButton("Aceptar") { dialog, which ->
                    dialog.cancel()
                }
        val jsonParametros = JSONObject()
        jsonParametros.put("credit_id", prestamo)
        jsonParametros.put("pay_date", fecha)

        val request =
            @SuppressLint("SetTextI18n") //<-- se agrega para admitir una variedad de configuraciones regionales sin tener que modificar código en la concatenacion de cadenas
            object : JsonObjectRequest(
                Method.POST,
                getString(R.string.urlPagosGrupo),
                //getString(R.string.urlDatosCredito),
                jsonParametros,
                Response.Listener { response ->
                    try {
                        //Obtiene su respuesta json
                        val jsonData =
                            JSONTokener(response.getString("data")).nextValue() as JSONObject
                        if (jsonData.getInt("code") == 200) {
                            //Toast.makeText(activity, "PETICION EXITOSA", Toast.LENGTH_SHORT).show()
                            val jsonResults = jsonData.getJSONArray("results")
                            llenarTbPagos(jsonResults)
                            /*
                            for (i in 0 until jsonResults.length()) {
                                val CL: JSONObject = jsonResults.getJSONObject(i)
                                binding.txt9.text = CL.toString()
                                binding.txt9.text = CL.getString("credit_id") + " NO CLIENTAS = "+jsonResults.length()
                            }
                            */
                        } else {
                            dialogNo.show()
                        }
                    } catch (e: Exception) {
                        //dialogNo.setMessage("Ocurrio un error catch $e") //PRUEBAS
                        if (e.message != null){
                            dialogNo.show()
                        }
                        //dialogNo.setMessage(getString(R.string.error))
                        //dialogNo.show()
                    }
                },
                Response.ErrorListener { error ->
                    //val errorD = VolleyError(String(error.networkResponse.data))
                    val responseError = String(error.networkResponse.data)
                    val dataError = JSONObject(responseError)
                    var mensaje = getString(R.string.error)
                    try {
                        val jsonData =
                            JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                        val code = jsonData.getInt("code")
                        val message = jsonData.getString("message")
                        val jResul =
                            JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                        if (code == 422 && jsonData.getString("results").contains("credit_id")) {
                            mensaje = jResul.getString("credit_id")
                        } else {
                            mensaje = message
                        }

                    } catch (e: Exception) {
                        mensaje = getString(R.string.error)
                    }
                    progressBar = binding.cargando
                    progressBar.visibility = View.INVISIBLE
                    binding.txtCargando.text = mensaje
                    dialogNo.setMessage(mensaje)
                    dialogNo.show()
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
        //primero borramos el cache y enviamos despues la peticion
        queue.cache.clear()
        queue.add(request)
        /*******  FIN ENVIO   *******/
    }

    @SuppressLint("SetTextI18n")
    private fun llenarTbPagos(jsonPagos: JSONArray) {
        val txt = binding.txtPagoSemanal
        txt.text = "Pago semanal del grupo: ${formatPesos.format(11984)}"
        txt.gravity = Gravity.CENTER
        //SE OBTIENE LA TABLA
        val tabla = binding.tblPagos
        val trEn = TableRow(activity)
        val fontTh = 16F
        val fontTr = 15F
        trEn.setBackgroundResource(R.drawable.redondo_verde)
        trEn.gravity = Gravity.CENTER
        val tS = TextView(activity)
        tS.text = "Fecha"
        tS.setPadding(0, 0, 0, 0)
        tS.gravity = Gravity.LEFT
        tS.setTextColor(Color.WHITE)
        tS.setTypeface(null, Typeface.BOLD_ITALIC)
        tS.textSize = fontTh
        trEn.addView(tS)

        val tF = TextView(activity)
        tF.text = "Estado"
        tF.setPadding(100, 0, 0, 0)
        tF.gravity = Gravity.RIGHT
        tF.setTextColor(Color.WHITE)
        tF.setTypeface(null, Typeface.BOLD_ITALIC)
        tF.textSize = fontTh
        trEn.addView(tF)

        val tEs = TextView(activity)
        tEs.text = ""
        tEs.setPadding(5, 0, 55, 0)
        tEs.gravity = Gravity.CENTER
        tEs.setTextColor(Color.WHITE)
        tEs.setTypeface(null, Typeface.BOLD_ITALIC)
        tEs.textSize = fontTh
        trEn.addView(tEs)

        val tRP = TextView(activity)
        tRP.text = "Registro de pagos"
        tRP.setPadding(0, 0, 0, 0)
        tRP.maxWidth = 200
        tRP.setTextColor(Color.WHITE)
        tRP.setTypeface(null, Typeface.BOLD_ITALIC)
        tRP.textSize = fontTh
        trEn.addView(tRP)

        tabla.addView(
            trEn,
            TableLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val numPagos = jsonPagos.length()
        //for del numero de pagos
        for (i in 0 until numPagos) {
            //SE GENERA EL OBJETO CLIENTE DEL ARREGLO
            val pago: JSONObject = jsonPagos.getJSONObject(i)
            val tr = TableRow(activity)
            if (i != numPagos) {
                tr.setBackgroundResource(R.drawable.borde)
            } else {
                tr.setBackgroundResource(R.drawable.borde_redondeado_verde)
            }

            val txtS = TextView(activity)
            txtS.setTextColor(resources.getColor(R.color.Azul1))
            txtS.setPadding(5, 10, 5, 10)
            txtS.textSize = fontTr
            txtS.gravity = Gravity.CENTER

            val txtF = TextView(activity)
            //txtF.setBackgroundResource(R.drawable.borde)
            txtF.setTextColor(resources.getColor(R.color.Azul1))
            txtF.setTextColor(resources.getColor(R.color.Azul1))
            txtF.setPadding(15, 10, 0, 10)
            txtF.textSize = fontTr
            txtF.gravity = Gravity.LEFT
            txtF.maxWidth = 380

            val txtEs = TextView(activity)
            txtEs.setTextColor(resources.getColor(R.color.Verde1))
            txtEs.setPadding(20, 10, 5, 10)
            txtEs.textSize = fontTr
            txtEs.gravity = Gravity.CENTER
            txtEs.maxWidth = 380

            val edit = ImageView(activity)
            edit.setImageResource(R.drawable.ic_editar_junta)
            edit.setColorFilter(Color.GREEN)
            edit.setPadding(1, 10, 0, 10)

            val ver = ImageView(activity)
            ver.setImageResource(R.drawable.ic_ver_junta)
            ver.setColorFilter(resources.getColor(R.color.Azul1))
            ver.setPadding(0, 10, 0, 10)

            txtS.text = pago.getString("pay_no")
            var fechaPago = pago.getString("pay_date") //"09/12/2021"
            txtF.text = fechaPago
            var estatus = pago.getString("pay_status").uppercase(Locale.getDefault())
            txtEs.text = estatus
            edit.setOnClickListener { generarJunta(true, pago.getInt("pay_id"),pago.getInt("pay_no"), fechaPago) }
            ver.setOnClickListener { generarJunta(false, pago.getInt("pay_id"),pago.getInt("pay_no"), fechaPago) }

            tr.addView(txtS)
            tr.addView(txtF)
            tr.addView(txtEs)
            tr.addView(edit)
            tr.addView(ver)

            tabla.addView(
                tr,
                TableLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        mostrarFormato(true)
    }

    private fun generarJunta(esEditar: Boolean, idPago: Int, numPago: Int, fechaPago: String) {
        val junta = Intent(activity, JuntaActivity::class.java)
        //enviamos datos
        junta.putExtra("esEdicion", esEditar)
        junta.putExtra("idPago", idPago)
        junta.putExtra("numPago", numPago)
        junta.putExtra("fechaPago", fechaPago)
        startActivity(junta)
    }

    //AGREGA EL MENU DE OPCIONES A LA VISTA
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
/*

    private fun llenarTbPagosSinJson() {
        val txt = binding.txtPagoSemanal
        txt.text = "Pago semanal del grupo: ${formatPesos.format(11984)}"
        txt.gravity = Gravity.CENTER
        //SE OBTIENE LA TABLA
        val tabla = binding.tblPagos
        val trEn = TableRow(activity)
        val fontTh = 16F
        val fontTr = 15F
        trEn.setBackgroundResource(R.drawable.redondo_verde)
        trEn.gravity = Gravity.CENTER
        val tS = TextView(activity)
        tS.text = "Fecha"
        tS.setPadding(0, 0, 0, 0)
        tS.gravity = Gravity.LEFT
        tS.setTextColor(Color.WHITE)
        tS.setTypeface(null, Typeface.BOLD_ITALIC)
        tS.textSize = fontTh
        trEn.addView(tS)

        val tF = TextView(activity)
        tF.text = "Estado"
        tF.setPadding(100, 0, 0, 0)
        tF.gravity = Gravity.RIGHT
        tF.setTextColor(Color.WHITE)
        tF.setTypeface(null, Typeface.BOLD_ITALIC)
        tF.textSize = fontTh
        trEn.addView(tF)

        val tEs = TextView(activity)
        tEs.text = ""
        tEs.setPadding(5, 0, 55, 0)
        tEs.gravity = Gravity.CENTER
        tEs.setTextColor(Color.WHITE)
        tEs.setTypeface(null, Typeface.BOLD_ITALIC)
        tEs.textSize = fontTh
        trEn.addView(tEs)

        val tRP = TextView(activity)
        tRP.text = "Registro de pagos"
        tRP.setPadding(0, 0, 0, 0)
        tRP.maxWidth = 200
        tRP.setTextColor(Color.WHITE)
        tRP.setTypeface(null, Typeface.BOLD_ITALIC)
        tRP.textSize = fontTh
        trEn.addView(tRP)

        tabla.addView(
            trEn,
            TableLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val numPagos = 16
        //for del numero de pagos
        for (i in 1..numPagos) {
            val tr = TableRow(activity)
            if (i != numPagos) {
                tr.setBackgroundResource(R.drawable.borde)
            } else {
                tr.setBackgroundResource(R.drawable.borde_redondeado_verde)
            }

            val txtS = TextView(activity)
            txtS.text = "$i"
            txtS.setTextColor(resources.getColor(R.color.Azul1))
            txtS.setPadding(5, 10, 5, 10)
            txtS.textSize = fontTr
            txtS.gravity = Gravity.CENTER
            tr.addView(txtS)

            val txtF = TextView(activity)
            txtF.setTextColor(resources.getColor(R.color.Azul1))
            var fechaPago = "09/12/2021"

            val txtEs = TextView(activity)
            txtEs.setTextColor(resources.getColor(R.color.Verde1))
            var estatus = "PAGADO"
            when (i) {
                1 -> {
                    fechaPago = "28/10/2021"
                }
                2 -> {
                    fechaPago = "04/11/2021"
                }
                3 -> {
                    fechaPago = "11/11/2021"
                    estatus = "ATRASADO"
                    txtEs.setTextColor(Color.RED)
                }
                4 -> {
                    fechaPago = "18/11/2021"
                    estatus = "VIGENTE"
                }
                5 -> {
                    fechaPago = "25/11/2021"
                    estatus = "VIGENTE"
                }
                6 -> {
                    fechaPago = "02/12/2021"
                    estatus = "VIGENTE"
                }
                else -> {
                    fechaPago = "09/12/2021"
                    estatus = "VIGENTE"
                }
            }
            txtF.text = fechaPago
            txtF.setTextColor(resources.getColor(R.color.Azul1))
            txtF.setPadding(15, 10, 0, 10)
            txtF.textSize = fontTr
            txtF.gravity = Gravity.LEFT
            txtF.maxWidth = 380
            tr.addView(txtF)

            //val txtEs = TextView(activity)
            //txtEs.text = formatPesos.format(1500 * i + 1)
            txtEs.text = estatus
            //txtEs.setTextColor(resources.getColor(R.color.Azul1))
            txtEs.setPadding(5, 10, 5, 10)
            txtEs.textSize = fontTr
            txtEs.gravity = Gravity.CENTER
            txtEs.maxWidth = 380
            tr.addView(txtEs)

            val edit = ImageView(activity)
            //edit.setImageResource(R.drawable.ic_edita_junta)
            edit.setImageResource(R.drawable.ic_editar_junta)
            edit.setColorFilter(Color.GREEN)
            edit.setPadding(1, 10, 0, 10)
            edit.setOnClickListener { generarJunta(true, i, fechaPago) }
            tr.addView(edit)

            val ver = ImageView(activity)
            ver.setImageResource(R.drawable.ic_ver_junta)
            ver.setColorFilter(resources.getColor(R.color.Azul1))
            ver.setPadding(0, 10, 0, 10)
            ver.setOnClickListener { generarJunta(false, i, fechaPago) }
            tr.addView(ver)
            tabla.addView(
                tr,
                TableLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
    }
 */
