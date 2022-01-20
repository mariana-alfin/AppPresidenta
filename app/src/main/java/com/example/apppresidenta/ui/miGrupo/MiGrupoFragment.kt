package com.example.apppresidenta.ui.miGrupo

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
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
import com.example.apppresidenta.DetalleClienteActivity
import com.example.apppresidenta.FuncionesGlobales
import com.example.apppresidenta.R
import com.example.apppresidenta.ValGlobales
import com.example.apppresidenta.databinding.MiGrupoActivityBinding
import com.example.apppresidenta.utils.GeneralUtils.Companion.enviarMensajeWhatsApp
import com.example.apppresidenta.utils.GeneralUtils.Companion.llamarContacto
import com.example.apppresidenta.utils.GeneralUtils.Companion.mostrarAlertInstalarApp
import com.example.apppresidenta.utils.GeneralUtils.Companion.validarAplicacionInstalada
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONObject
import org.json.JSONTokener
import java.text.NumberFormat
import java.util.*
import org.json.JSONArray





class MiGrupoFragment : Fragment() {

    private lateinit var miGrupoViewModel: MiGrupoViewModel
    private var _binding: MiGrupoActivityBinding? = null

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
        miGrupoViewModel =
            ViewModelProvider(this).get(MiGrupoViewModel::class.java)

        _binding = MiGrupoActivityBinding.inflate(inflater, container, false)
        val root: View = binding.root

        /* val textView: TextView = binding.textDashboard
         dashboardViewModel.text.observe(viewLifecycleOwner, Observer {
             textView.text = it
         })*/
        //INDICA QUE SE HABILITARA EL MENU DE OPCIONES
        setHasOptionsMenu(true)
        //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(activity as AppCompatActivity,"true")

        //llenarTabla()
        mostrarFormato(false)
        if (ValGlobales.validarConexion(activity as AppCompatActivity)) {
            datosDelGrupo()
        }else{
            binding.txtCargando.text = getString(R.string.noConexion)
            binding.txtCargando.gravity = Gravity.CENTER
            binding.txtCargando.visibility = View.VISIBLE
            progressBar = binding.cargando
            progressBar.visibility = View.INVISIBLE
        }
        return root
    }

    //AGREGA EL MENU DE OPCIONES A LA VISTA
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun detalleCliente(idCliente: Int, nombre: String) {
        val detalle = Intent(activity, DetalleClienteActivity::class.java)
        //enviamos datos
        detalle.putExtra("idCliente", idCliente)
        startActivity(detalle)
    }
    private fun datosDelGrupo() {
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val prestamo = prefs.getInt("CREDITO_ID", 0)
        //val prestamo = 119483 //para pruebas
        val fecha = "" //para pruebas
        val dialogNo = AlertDialog.Builder(requireActivity(), R.style.ThemeOverlay_AppCompat_Dialog_Alert)
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
                getString(R.string.urlMiembrosGrupo),
                //getString(R.string.urlDatosCredito),
                jsonParametros,
                Response.Listener { response ->
                    try {
                        //Obtiene su respuesta json
                        val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject
                        if(jsonData.getInt("code") == 200){
                            //Toast.makeText(activity, "PETICION EXITOSA", Toast.LENGTH_SHORT).show()
                            val jsonResults = jsonData.getJSONArray("results")
                            llenarTablaClientes(jsonResults)
                            /*
                            for (i in 0 until jsonResults.length()) {
                                val CL: JSONObject = jsonResults.getJSONObject(i)
                                binding.txt9.text = CL.toString()
                                binding.txt9.text = CL.getString("credit_id") + " NO CLIENTAS = "+jsonResults.length()
                            }
                            */
                        }
                    } catch (e: Exception) {
                        //dialogNo.setMessage("Ocurrio un error catch $e") //PRUEBAS
                            if (e.message != null){
                                dialogNo.show()
                            }
                        dialogNo.show()
                    }
                },/*
                Response.ErrorListener {
                    val codigoError = it.networkResponse.statusCode
                    /*findViewById<TextView>(R.id.txtPruebas).text = "$codigoError"
                    Toast.makeText(this, "$it", Toast.LENGTH_SHORT).show()*/
                    if (codigoError == 422) {
                        dialogNo.setMessage(Html.fromHtml("El ID de Crédito no se encontro."))
                    } else {
                        dialogNo.setMessage(getString(R.string.error))
                    }
                    dialogNo.show()

                }*/Response.ErrorListener { error ->
                    //val errorD = VolleyError(String(error.networkResponse.data))
                    val responseError = String(error.networkResponse.data)
                    val dataError = JSONObject(responseError)
                    var mensaje = getString(R.string.error)
                    try {
                        val jsonData = JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                        val code = jsonData.getInt("code")
                        val message = jsonData.getString("message")
                        val jResul = JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                        if(code == 422 && jsonData.getString("results").contains("credit_id")){
                            mensaje = jResul.getString("credit_id")
                        }else{
                            mensaje = message
                        }

                    }catch (e: Exception){
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

    private fun llenarTablaClientes(jsonClientes: JSONArray) {
        //se obtiene la tabla
        val tabla = binding.tblMiGpo
        //encabezado de la tabla
        val trEn = TableRow(activity)
        val fontTh = 16F
        val fontTr = 15F
        trEn.setBackgroundResource(R.drawable.redondo_verde)
        val linea = LinearLayout(activity)
        val txtN = TextView(activity)
        txtN.text = "Integrantes"
        txtN.setPadding(30, 0, 5, 0)
        txtN.gravity = Gravity.CENTER
        txtN.maxWidth = 500
        txtN.setTextColor(Color.WHITE)
        txtN.setTypeface(null, Typeface.BOLD_ITALIC)
        txtN.textSize = fontTh
        linea.addView(txtN)
        trEn.addView(linea)

        val txtP = TextView(activity)
        txtP.text = "Pago"
        txtP.setPadding(10, 0, 5, 0)
        txtP.setTextColor(Color.WHITE)
        txtP.setTypeface(null, Typeface.BOLD_ITALIC)
        txtP.textSize = fontTh
        trEn.addView(txtP)

        val txtL = TextView(activity)
        txtL.text = "Llamar"
        txtL.setPadding(0, 0, 5, 0)
        txtL.setTextColor(Color.WHITE)
        txtL.setTypeface(null, Typeface.BOLD_ITALIC)
        txtL.textSize = fontTh
        trEn.addView(txtL)

        val txtM = TextView(activity)
        txtM.text = "Mensaje"
        txtM.setPadding(5, 0, 0, 0)
        txtM.setTextColor(Color.WHITE)
        txtM.setTypeface(null, Typeface.BOLD_ITALIC)
        txtM.textSize = fontTh
        trEn.addView(txtM)

        tabla.addView(
            trEn,
            TableLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val numClientes = jsonClientes.length()
        //for de los clientes
        for (i in 0 until numClientes) {
            //SE GENERA EL OBJETO CLIENTE DEL ARREGLO
            val cte: JSONObject = jsonClientes.getJSONObject(i)

            //SOLO SI ES LA PRESIDENTA SE GUARDA EN SESION EL NOMBRE
            if(cte.getString("group_rol") == "P") {
                val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                val editor = prefs.edit()
                editor.putString("PRESIDENTA", cte.getString("customer_name"))
                editor.apply()
            }

            val tr1 = TableRow(activity)
            if (i != numClientes) {
                tr1.setBackgroundResource(R.drawable.borde)
            }else{
                tr1.setBackgroundResource(R.drawable.borde_redondeado_verde)
            }

            val linea = LinearLayout(activity)
            val view = ImageView(activity)
            view.setImageResource(R.drawable.ic_pago_v)
            view.setPadding(10,10,5,10)
            linea.addView(view)

            val txtN = TextView(activity)
            txtN.setTextColor(resources.getColor(R.color.Azul1))
            txtN.textSize = fontTr
            txtN.setTextColor(resources.getColor(R.color.Verde1))
            txtN.paintFlags = Paint.UNDERLINE_TEXT_FLAG
            txtN.setPadding(5, 10, 5, 10)
            txtN.gravity = Gravity.LEFT
            txtN.maxWidth = 380


            val txtP = TextView(activity)
            txtP.setTextColor(resources.getColor(R.color.Azul1))
            txtP.textSize = fontTr
            txtP.setPadding(5, 10, 5, 10)
            txtP.gravity = Gravity.CENTER

            val call = ImageView(activity)
            call.setImageResource(R.drawable.call_24)
            call.setColorFilter(Color.GREEN)
            call.setPadding(5, 10, 5, 10)

            val mensaje = ImageView(activity)
            mensaje.setImageResource(R.drawable.ic_whats)
            mensaje.setColorFilter(Color.GREEN)
            mensaje.setPadding(5, 10, 5, 10)

            txtN.text =  cte.getString("customer_name")
            txtP.text = formatPesos.format(cte.getDouble("pay"))
            //call.setOnClickListener{ llamarCliente(cte.getString("cell_phone"))}
            call.setOnClickListener{ llamarContacto(context,cte.getString("cell_phone"))}
            mensaje.setOnClickListener{
                validacionesEnvioWhats(cte.getString("cell_phone")
                    ,cte.getString("customer_name")) }

            linea.addView(txtN)
            linea.setOnClickListener { detalleCliente(cte.getInt("credit_id"),  cte.getString("customer_name") ) }
            tr1.addView(linea)
            tr1.addView(txtP)
            tr1.addView(call)
            tr1.addView(mensaje)

            tabla.addView(
                tr1,
                TableLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        mostrarFormato(true)
    }

    /*private fun llamarCliente(string: String) {
        Toast.makeText(activity, "Llamar al $string", Toast.LENGTH_SHORT).show()
    }*/

    private fun validacionesEnvioWhats(telefono : String, nombreclienta : String) {

        if (!validarAplicacionInstalada(getString(R.string.packagename_whats),context)) {
            mostrarAlertInstalarApp(context,getString(R.string.packagename_whats))
            return
        }

        enviarMensajeWhatsApp(context, getString(R.string.mensaje_whats) +" "+ nombreclienta,telefono)
    }

    private fun mostrarFormato(esMostrar: Boolean) {
        var valor = View.VISIBLE
        var valorLoadi = View.INVISIBLE
        progressBar = binding.cargando

        if(!esMostrar){
            valor = View.INVISIBLE
            valorLoadi = View.VISIBLE
        }
        progressBar.visibility = valorLoadi
        binding.txtCargando.visibility = valorLoadi
        binding.txtMiGrupo.visibility = valor
        binding.tblMiGpo.visibility = valor
    }

    fun llenarTabla() {
        //se obtiene la tabla
        val tabla = binding.tblMiGpo
        //encabezado de la tabla
        val trEn = TableRow(activity)
        val fontTh = 16F
        val fontTr = 15F
        trEn.setBackgroundResource(R.drawable.redondo_verde)
        val linea = LinearLayout(activity)
        val txtN = TextView(activity)
        txtN.text = "Integrantes"
        txtN.setPadding(30, 0, 5, 0)
        txtN.gravity = Gravity.CENTER
        txtN.maxWidth = 500
        txtN.setTextColor(Color.WHITE)
        txtN.setTypeface(null, Typeface.BOLD_ITALIC)
        txtN.textSize = fontTh
        linea.addView(txtN)
        trEn.addView(linea)

        val txtP = TextView(activity)
        txtP.text = "Pago"
        txtP.setPadding(10, 0, 5, 0)
        txtP.setTextColor(Color.WHITE)
        txtP.setTypeface(null, Typeface.BOLD_ITALIC)
        txtP.textSize = fontTh
        trEn.addView(txtP)

        val txtL = TextView(activity)
        txtL.text = "Llamar"
        txtL.setPadding(0, 0, 5, 0)
        txtL.setTextColor(Color.WHITE)
        txtL.setTypeface(null, Typeface.BOLD_ITALIC)
        txtL.textSize = fontTh
        trEn.addView(txtL)

        tabla.addView(
            trEn,
            TableLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val numClientes = 8
        //for de los clientes
        for (i in 0..numClientes) {
            val tr1 = TableRow(activity)
            if (i != numClientes) {
                tr1.setBackgroundResource(R.drawable.borde)
            }else{
                tr1.setBackgroundResource(R.drawable.borde_redondeado_verde)
            }

            val linea = LinearLayout(activity)
            val view = ImageView(activity)
            view.setImageResource(R.drawable.ic_pago_v)
            view.setPadding(10,10,5,10)

            val txtN = TextView(activity)
            txtN.setTextColor(resources.getColor(R.color.Azul1))
            txtN.textSize = fontTr
            var nombre = "Trejo Contreras Maria"
            when (i) {
                0 -> {
                    nombre = "Martinez Garduño Lidia"
                }
                1 -> {
                    nombre = "Arellano lara lopez lola Fonseca Ana Bertha"
                }
                2 -> {
                    nombre = "Delgadillo Lara Martha"
                    view.setImageResource(R.drawable.ic_no_pago_r)
                }
                3 -> {
                    nombre = "Valenzuela Sanches Martha"
                }
                4 -> {
                    nombre = "Quiroz Garcia Maria Fernanda"
                }
                5 -> {
                    nombre = "Campos Maysen Sonia"
                    view.setImageResource(R.drawable.ic_no_pago_r)
                }
                6 -> {
                    nombre = "Rosas Carmona Connie"
                }
                7 -> {
                    nombre = "Pintle Salinas Cerda Lourdes Maria Leticia"
                }
                else -> {
                    nombre = "Trejo Contreras Maria"
                }
            }
            //SE AGREGAN AS CARITAS
            linea.addView(view)

            txtN.text = nombre
            txtN.setTextColor(resources.getColor(R.color.Verde1))
            txtN.paintFlags = Paint.UNDERLINE_TEXT_FLAG
            txtN.setPadding(5, 10, 5, 10)
            txtN.gravity = Gravity.LEFT
            txtN.maxWidth = 380
            //txtN.gravity = Gravity.CENTER
            linea.addView(txtN)
            linea.setOnClickListener { detalleCliente(i, nombre) }
            tr1.addView(linea)

            val txtP = TextView(activity)
            txtP.text = formatPesos.format(1500 * i + 1)
            txtP.setTextColor(resources.getColor(R.color.Azul1))
            txtP.textSize = fontTr
            txtP.setPadding(5, 10, 5, 10)
            txtP.gravity = Gravity.CENTER
            tr1.addView(txtP)

            val call = ImageView(activity)
            call.setImageResource(R.drawable.call_24)
            call.setColorFilter(Color.GREEN)
            call.setPadding(5, 10, 5, 10)
            tr1.addView(call)

            tabla.addView(
                tr1,
                TableLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

    }

}