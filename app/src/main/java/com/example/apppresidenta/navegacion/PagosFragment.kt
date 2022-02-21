package com.example.apppresidenta.navegacion

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.DetalleJuntaActivity
import com.example.apppresidenta.JuntaActivity
import com.example.apppresidenta.JuntaConciliacionActivity
import com.example.apppresidenta.R
import com.example.apppresidenta.databinding.PagosFragmentBinding
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.LoadingScreen
import com.example.apppresidenta.generales.ValGlobales
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*


class PagosFragment : Fragment() {

    private var _binding: PagosFragmentBinding? = null
    lateinit var progressBar: CircularProgressIndicator

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        _binding = PagosFragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //MD INDICA QUE SE HABILITARA EL MENU DE OPCIONES
        setHasOptionsMenu(true)
        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(activity as AppCompatActivity, "true")
        //llenarTbPagos()
        mostrarFormato(false)
        if (ValGlobales.validarConexion(activity as AppCompatActivity)) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            val prestamo = prefs.getInt("CREDITO_ID", 0)
            val pagoSemanal = prefs.getFloat("MONTO_SEMANAL", 0.0F).toDouble()
            obtenerSaldoConciliar(prestamo) //MD SE OBTIENE EL SALDO POR CONCILIAR
            datosPagos(prestamo, pagoSemanal)

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
        binding.txtPagoSemanal.visibility = valor
        binding.txtP.visibility = valor
        binding.txtSaldoConciliar.visibility = valor
        binding.iConciliar.visibility = View.INVISIBLE
        val textConciliar = binding.txtSaldoConciliar.text
        if (textConciliar != "") {
            binding.iConciliar.visibility = View.VISIBLE
        }
    }

    private fun datosPagos(prestamo: Int, pagoSemanal: Double) {
        /**************   MD  ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        /*val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val prestamo = prefs.getInt("CREDITO_ID", 0)
        val pagoSemanal = prefs.getFloat("MONTO_SEMANAL", 0.0F)
        */
        val alertError = FuncionesGlobales.mostrarAlert(
            requireActivity(),
            "error",
            true,
            "Obtener Pagos",
            getString(R.string.error),
            false
        )
        try {
            val fecha = ""//"2020-01-17" //para pruebas
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
                                llenarTbPagos(jsonResults, pagoSemanal, prestamo)
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
                            val responseError = String(error.networkResponse.data)
                            val dataError = JSONObject(responseError)
                            val jsonData =
                                JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                            val code = jsonData.getInt("code")
                            val message = jsonData.getString("message")
                            val jResul =
                                JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                            mensaje = if (code == 422 && jsonData.getString("results")
                                    .contains("credit_id")
                            ) {
                                jResul.getString("credit_id")
                            } else {
                                message
                            }

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

    @SuppressLint("SetTextI18n", "Range")
    private fun llenarTbPagos(jsonPagos: JSONArray, pagoSemanal: Double, prestamo: Int) {
        val txt = binding.txtPagoSemanal
        txt.text = "  ${FuncionesGlobales.convertPesos(pagoSemanal,2)}  "
        txt.gravity = Gravity.CENTER
        //MD SE OBTIENE LA TABLA
        val tabla = binding.tblPagos
        val trEn = TableRow(activity)
        val fontTh = 18F
        val fontTr = 16F
        trEn.setBackgroundResource(R.drawable.redondo_verde)
        trEn.setPadding(0, 20, 0, 20)

        val txtM = TextView(activity)
        txtM.text = "______"
        txtM.alpha = 0.0F //MD HACE COMPLETAMENTE OPACO UN ELEMENTO
        txtM.setTextColor(resources.getColor(R.color.Verde2))
        txtM.setTypeface(null, Typeface.BOLD_ITALIC)
        txtM.textSize = fontTh
        trEn.addView(txtM)

        val tS = TextView(activity)
        tS.text = "Fecha"
        tS.gravity = Gravity.CENTER
        tS.setTextColor(Color.WHITE)
        tS.setTypeface(null, Typeface.BOLD_ITALIC)
        tS.textSize = fontTh
        trEn.addView(tS)


        val tF = TextView(activity)
        tF.text = "   Estado   "
        tF.gravity = Gravity.CENTER
        tF.setTextColor(Color.WHITE)
        tF.setTypeface(null, Typeface.BOLD_ITALIC)
        tF.textSize = fontTh
        trEn.addView(tF)

        val tRP = TextView(activity)
        tRP.text = "Seguimiento"
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
        //MD FOR DEL NUMERO DE PAGOS
        for (i in 0 until numPagos) {
            //SE GENERA EL OBJETO CLIENTE DEL ARREGLO
            val pago: JSONObject = jsonPagos.getJSONObject(i)
            val tr = TableRow(activity)
            tr.setPadding(0, 10, 0, 10)
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
            txtF.setTextColor(resources.getColor(R.color.Azul1))
            txtF.textSize = fontTr
            txtF.gravity = Gravity.LEFT
            txtF.maxWidth = 380

            val txtEs = TextView(activity)
            txtEs.setTextColor(resources.getColor(R.color.Verde1))
            txtEs.textSize = fontTr
            txtEs.gravity = Gravity.CENTER
            txtEs.maxWidth = 380

            val lC = LinearLayout(activity)
            lC.gravity = Gravity.CENTER

            val edit = ImageView(activity)
            edit.setImageResource(R.drawable.ic_editar_junta)
            edit.setColorFilter(resources.getColor(R.color.Verde3))

            val ver = ImageView(activity)
            ver.setImageResource(R.drawable.ic_ver_junta)
            ver.setColorFilter(resources.getColor(R.color.Azul1))
            ver.setPadding(30, 0, 0, 0)

            txtS.text = pago.getString("pay_no")
            val fechaPago = pago.getString("pay_date")
            //txtF.text = FuncionesGlobales.convertFecha(fechaPago,"dd/MM/yyyy")
            txtF.text = FuncionesGlobales.convertFecha(fechaPago, "dd-MMM-yy").replace(".-", "-")
                .uppercase()

            val estatus = pago.getString("pay_status").uppercase(Locale.getDefault())
            txtEs.text = estatus
            edit.setOnClickListener {
                generarJunta(
                    true,
                    pago.getInt("pay_id"),
                    pago.getInt("pay_no"),
                    fechaPago
                )
            }
            ver.setOnClickListener { obtenerJuntas(fechaPago, prestamo) }

            tr.addView(txtS)
            tr.addView(txtF)
            tr.addView(txtEs)
            lC.addView(edit)
            lC.addView(ver)
            tr.addView(lC)

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

    private fun obtenerJuntas(fechaPago: String, prestamo: Int) {
        LoadingScreen.displayLoadingWithText(activity, "Espere un momento...", false)
        /**************   MD  ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alertError = FuncionesGlobales.mostrarAlert(
            requireActivity(),
            "error",
            true,
            "Obtener Juntas",
            getString(R.string.error),
            false
        )
        try {
            val jsonParametros = JSONObject()
            jsonParametros.put("credit_id", prestamo)
            jsonParametros.put("pay_date", fechaPago)

            val request =
                @SuppressLint("SetTextI18n") //<--MD SE AGREGA PARA ADMITIR UNA VARIEDAD DE CONFIGURACIONES REGIONALES SIN TENER QUE MODIFICAR CÓDIGO EN LA CONCATENACION DE CADENAS
                object : JsonObjectRequest(
                    Method.POST,
                    getString(R.string.urlJuntasPgo),
                    jsonParametros,
                    Response.Listener { response ->
                        try {
                            //MD OBTIENE SU RESPUESTA JSON
                            val jsonData =
                                JSONTokener(response.getString("data")).nextValue() as JSONObject
                            if (jsonData.getInt("code") == 200) {
                                val jsonResults = JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                                verJuntas(fechaPago,jsonResults.getJSONArray("tasks"))

                            } else {
                                alertError.show()
                                LoadingScreen.hideLoading()
                            }
                        } catch (e: Exception) {
                            if (e.message != null) {
                                alertError.show()
                                LoadingScreen.hideLoading()
                            }
                        }
                    },
                    Response.ErrorListener { error ->
                        //MD MANEJO DE ERROES EN LA RESPUESTA
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
                            mensaje = if (code == 422 && jsonData.getString("results")
                                    .contains("credit_id")
                            ) {
                                jResul.getString("credit_id")
                            } else {
                                message
                            }
                        } catch (e: Exception) {
                            val codigo = error.networkResponse.statusCode
                            mensaje = "Error: $codigo \n${getString(R.string.errorServidor)}"

                        }
                        progressBar = binding.cargando
                        progressBar.visibility = View.INVISIBLE
                        binding.txtCargando.text = mensaje
                        alertError.setMessage(mensaje)
                        alertError.show()
                        LoadingScreen.hideLoading()
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
            LoadingScreen.hideLoading()
        }
        /*******  FIN ENVIO   *******/
    }
    //MD SE GENERA UNA CLASE PARA GUARDAR LOS DATOS
    class juntaPgo {
        var num_junta: Int
        var amount: Double
        var pay_id: Int

        constructor(num_junta: Int,amount: Double, pay_id: Int) {
            this.num_junta = num_junta
            this.amount = amount
            this.pay_id = pay_id
        }
    }
    private fun verJuntas(fechaPago: String, juntas: JSONArray) {
        val alertJuntas = FuncionesGlobales.mostrarAlert(
            requireActivity(),
            "cuestion",
            true,
            "Selecciona una Junta",
            "fechaPago ",
            false
        )
        val listaJuntas: MutableList<String> = ArrayList()
        val listaNJuntas: MutableMap<Int,juntaPgo> = mutableMapOf(0 to juntaPgo(0,0.0, 1))
        listaNJuntas.clear()
        //MD SE GENERA UNA LISTA CON LOS DATOS DE LAS JUNTAS Y OTRA CON LOS NUMEROS DE ELLAS PARA MOSTRARLOS Y RECUPERARLOS
        try{
            for (i in 0 until juntas.length()) {
                val junta: JSONObject = juntas.getJSONObject(i)
                listaJuntas.add("Junta #${junta.getString("number")} Monto Capturado ${FuncionesGlobales.convertPesos(junta.getDouble("amount"),2)}")
                listaNJuntas.put(i,juntaPgo(junta.getInt("number"),junta.getDouble("amount"),junta.getInt("pay_id")))
            }
        }catch (e: java.lang.Exception){
            listaJuntas.add(e.message.toString())
        }
        LoadingScreen.hideLoading()
        var idPago = 0
        var task_number = 0
        var monto = 0.0
        val builder = AlertDialog.Builder(requireActivity(),R.style.MyAlertDialogTheme)
        //builder.setTitle(HtmlCompat.fromHtml("<font color='#246035'>Selecciona una junta </font>", HtmlCompat.FROM_HTML_MODE_LEGACY))
        builder.setTitle("Selecciona una junta")
        builder.setIcon(R.drawable.ic_juntas)
        if (juntas.length() != 0){
            builder.setSingleChoiceItems(listaJuntas.toTypedArray(),-1) { dialog, which ->
                // user checked an item
                idPago = listaNJuntas[which]?.pay_id!!
                task_number = listaNJuntas[which]?.num_junta!!
                monto = listaNJuntas[which]?.amount!!
            }
            builder.setPositiveButton("Aceptar") { dialog, which ->
                //MD SE VALIDA QUE SE HALLA SELECIONADO UNA JUNTA
                if(task_number != 0 && idPago != 0){
                    //alertJuntas.setMessage("Datos Junta: # $task_number PagoId: $idPago")
                    val detalleJunta = Intent(activity, DetalleJuntaActivity::class.java)
                    //MD ENVIAMOS DATOS
                    detalleJunta.putExtra("pay_id",idPago)
                    detalleJunta.putExtra("task_number", task_number)
                    detalleJunta.putExtra("monto", monto)
                    detalleJunta.putExtra("fechaPago", fechaPago)
                    startActivity(detalleJunta)
                }else{
                    alertJuntas.setIcon(R.drawable.ic_warning)
                    alertJuntas.setMessage("Debe de seleccionar una junta para visualizar su información")
                    alertJuntas.show()
                }
            }
            builder.setNegativeButton("Cancelar", null)
        }else{
            builder.setMessage("No hay juntas registradas")
            builder.setPositiveButton("Aceptar", null)
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun obtenerSaldoConciliar(prestamo: Int) {
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alertError = FuncionesGlobales.mostrarAlert(
            requireActivity(),
            "error",
            true,
            "Saldo Conciliar",
            getString(R.string.error),
            false
        )
        try {
            val jsonParametros = JSONObject()
            jsonParametros.put("credit_id", prestamo)
            jsonParametros.put("payment_date", null)
            jsonParametros.put("payment_status", 0)//1 PARA PRUEBAS

            val request = object : JsonObjectRequest(
                Method.POST,
                getString(R.string.urlPagosConciliar),
                jsonParametros,
                Response.Listener { response ->
                    try {
                        //OBTIENE SU RESPUESTA JSON
                        val jsonData =
                            JSONTokener(response.getString("data")).nextValue() as JSONObject
                        if (jsonData.getInt("code") == 200) {
                            val jsonResults =
                                JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                            val payments = jsonResults.getJSONArray("payments")
                            var saldoConciliar = 0.0
                            var pagosConciliar = ""
                            for (i in 0 until payments.length()) {
                                val pago: JSONObject = payments.getJSONObject(i)
                                saldoConciliar += pago.getDouble("amount")
                                pagosConciliar += "${pago.getString("payment_id")},"
                            }
                            if (saldoConciliar != 0.0) {//MD SOLO SI EL SALDO A CONCILIAR ES MAYOR A 0 SE MUESTRA
                                binding.txtSaldoConciliar.text =
                                    "Saldo por Conciliar ${FuncionesGlobales.convertPesos(saldoConciliar,2)}"
                                //MD SE AGREGA LA ACCION AL BOTON
                                binding.iConciliar.setOnClickListener {
                                    juntaConciliacion(
                                        saldoConciliar,
                                        pagosConciliar
                                    )
                                }
                            } else {
                                binding.txtSaldoConciliar.text = ""

                            }
                        } else {
                            alertError.show()
                        }
                    } catch (e: Exception) {
                    }
                },
                Response.ErrorListener { error ->
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
                        if (code == 422 && jsonData.getString("results").contains("credit_id")) {
                            mensaje = jResul.getString("credit_id")
                        } else {
                            mensaje = message
                        }
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
            //PRIMERO BORRAMOS EL CACHE Y ENVIAMOS DESPUES LA PETICION
            queue.cache.clear()
            queue.add(request)
            /*******  FIN ENVIO   *******/
        } catch (e: java.lang.Exception) {
            alertError.show()
        }
    }

    private fun juntaConciliacion(saldoConciliar: Double, idPagos: String) {
        val junta = Intent(activity, JuntaConciliacionActivity::class.java)
        //MD ENVIAMOS DATOS
        junta.putExtra("saldoConciliar", saldoConciliar)
        junta.putExtra("idPagos", idPagos)
        startActivity(junta)
    }

    private fun generarJunta(esEditar: Boolean, idPago: Int, numPago: Int, fechaPago: String) {
        val junta = Intent(activity, JuntaActivity::class.java)
        //MD ENVIAMOS DATOS
        junta.putExtra("esEdicion", esEditar)
        junta.putExtra("idPago", idPago)
        junta.putExtra("numPago", numPago)
        junta.putExtra("fechaPago", fechaPago)
        startActivity(junta)
    }

    //MD AGREGA EL MENU DE OPCIONES A LA VISTA
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
