package com.example.apppresidenta.navegacion

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.R
import com.example.apppresidenta.databinding.MiGrupoFragmentBinding
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.ValGlobales
import com.example.apppresidenta.submenu.DetalleClienteActivity
import com.example.apppresidenta.utils.GeneralUtils.Companion.enviarMensajeWhatsApp
import com.example.apppresidenta.utils.GeneralUtils.Companion.llamarContacto
import com.example.apppresidenta.utils.GeneralUtils.Companion.mostrarAlertInstalarApp
import com.example.apppresidenta.utils.GeneralUtils.Companion.validarAplicacionInstalada
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*


class MiGrupoFragment : Fragment() {

    private var _binding: MiGrupoFragmentBinding? = null

    lateinit var progressBar: CircularProgressIndicator

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = MiGrupoFragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //MD INDICA QUE SE HABILITARA EL MENU DE OPCIONES
        setHasOptionsMenu(true)
        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(activity as AppCompatActivity, "true")

        mostrarFormato(false)
        if (ValGlobales.validarConexion(activity as AppCompatActivity)) {
            datosDelGrupo()
        } else {
            binding.txtCargando.text = getString(R.string.noConexion)
            binding.txtCargando.gravity = Gravity.CENTER
            binding.txtCargando.visibility = View.VISIBLE
            progressBar = binding.cargando
            progressBar.visibility = View.INVISIBLE
        }
        return root
    }

    //MD AGREGA EL MENU DE OPCIONES A LA VISTA
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val grupo = prefs.getString("NOMBRE_GPO", "--")
        /*Se agrega logo y titulo del la actividad*/
        (activity as AppCompatActivity).supportActionBar?.title = "$grupo"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun detalleCliente(idCliente: Int) {
        val detalle = Intent(activity, DetalleClienteActivity::class.java)
        //MD ENVIAMOS DATOS AL ACTIVITY
        detalle.putExtra("idCliente", idCliente)
        startActivity(detalle)
    }

    private fun datosDelGrupo() {
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alertError = FuncionesGlobales.mostrarAlert(requireActivity(),"error",true,"Mi Grupo",getString(R.string.error),false)
        try {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val prestamo = prefs.getInt("CREDITO_ID", 0)
        //val prestamo = 119483 //para pruebas
        val fecha = "" //para pruebas
        val jsonParametros = JSONObject()
        jsonParametros.put("credit_id", prestamo)
        jsonParametros.put("pay_date", fecha)

        val request =
            @SuppressLint("SetTextI18n") //<-- MD SE AGREGA PARA ADMITIR UNA VARIEDAD DE CONFIGURACIONES REGIONALES SIN TENER QUE MODIFICAR CÓDIGO EN LA CONCATENACION DE CADENAS
            object : JsonObjectRequest(
                Method.POST,
                getString(R.string.urlMiembrosGrupo),
                //getString(R.string.urlDatosCredito),
                jsonParametros,
                Response.Listener { response ->
                    try {
                        //MD Obtiene su respuesta json
                        val jsonData =
                            JSONTokener(response.getString("data")).nextValue() as JSONObject
                        if (jsonData.getInt("code") == 200) {
                            //Toast.makeText(activity, "PETICION EXITOSA", Toast.LENGTH_SHORT).show()
                            val jsonResults = jsonData.getJSONArray("results")
                            llenarTablaClientes(jsonResults)
                        }
                    } catch (e: Exception) {
                        if (e.message != null) {
                            alertError.show()
                        }
                    }
                }, Response.ErrorListener { error ->
                    // MD MANEJO DE ERRORES EN LA RESPUESTA DE LA PETICION AL WS
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
        //MD PRIMERO BORRAMOS EL CACHE Y ENVIAMOS DESPUES LA PETICION
        queue.cache.clear()
        queue.add(request)
            }catch (e: Exception) {
            // Couldn't properly decode data to string
            alertError.show()
        }
        /*******  FIN ENVIO   *******/
    }

    @SuppressLint("WrongConstant")
    private fun llenarTablaClientes(jsonClientes: JSONArray) {
        //MD SE OBTIENE LA TABLA
        val tabla = binding.tblMiGpo
        //MD ENCABEZADO DE LA TABLA
        val trEn = TableRow(activity)
        var fontTh = 18F
        val fontTr = 15F

        /**     VARIABLES PARA TAMAÑOS      **/
        //MD SE OBTIENE LA DENSIDAD DEL DISPOSITIVO PARA CAMBIAR LOS TAMAÑOS
        val densidad = resources.displayMetrics.densityDpi
        val width =  resources.displayMetrics.widthPixels
        val witCte: Int = if(densidad < DisplayMetrics.DENSITY_HIGH){
            200
        }else if(densidad in DisplayMetrics.DENSITY_HIGH until DisplayMetrics.DENSITY_XHIGH && width > 400){
            (width/4)
        }else if(densidad in DisplayMetrics.DENSITY_HIGH until DisplayMetrics.DENSITY_XHIGH && width < 400){
            300
        }else if(densidad == DisplayMetrics.DENSITY_XHIGH){
            250
        }else if(densidad in DisplayMetrics.DENSITY_XHIGH until DisplayMetrics.DENSITY_XXHIGH){
            380
        }else if((densidad in DisplayMetrics.DENSITY_XXHIGH until DisplayMetrics.DENSITY_XXXHIGH) && width < 1200){
            415
        }else if((densidad >= DisplayMetrics.DENSITY_XXXHIGH) || width >= 1200){
            480
        }else {//MD SI LA PANTALLA ES MAYOR A 1200PX EL VALOR DEL NOMBRE SERA DE UNA TERCERA PARTE
            (width/3)
        }
        /***       FIN VARIABLES       **/

        trEn.setBackgroundResource(R.drawable.redondo_verde)
        trEn.setPadding(20, 20, 10, 20)

        val colorTH = Color.WHITE
        val tipoLetra = Typeface.MONOSPACE

        val linea = LinearLayout(activity)
        val txtN = TextView(activity)
        txtN.text = "Integrantes".uppercase()
        txtN.gravity = Gravity.CENTER
        txtN.maxWidth = 500
        txtN.setTextColor(colorTH)
        txtN.setTypeface(null, Typeface.BOLD_ITALIC)
        txtN.textSize = fontTh

        linea.addView(txtN)
        trEn.addView(linea)

        val txtP = TextView(activity)
        txtP.text = "Pago".uppercase()
        txtP.setTextColor(colorTH)
        txtP.setTypeface(null, Typeface.BOLD_ITALIC)
        txtP.textSize = fontTh
        trEn.addView(txtP)

        val txt = TextView(activity)
        txt.text = "___"
        txt.alpha = 0.0F
        txt.setTextColor(colorTH)
        txt.setTypeface(null, Typeface.BOLD_ITALIC)
        txt.textSize = fontTh
        trEn.addView(txt)

        val txtL = TextView(activity)
        txtL.text = "Contacto".uppercase()
        txtL.gravity = Gravity.CENTER
        txtL.setTextColor(colorTH)
        txtL.setTypeface(null, Typeface.BOLD_ITALIC)
        txtL.textSize = fontTh
        trEn.addView(txtL)

        val txtM = TextView(activity)
        txtM.text = "   "
        txtM.setTextColor(colorTH)
        txtM.setTypeface(null, Typeface.BOLD_ITALIC)
        txtM.textSize = fontTh
        trEn.addView(txtM)
        //propuesta titulos tabla
        txtN.typeface = tipoLetra
        txtP.typeface = tipoLetra
        txtL.typeface = tipoLetra
        trEn.setBackgroundResource(R.drawable.borde_relleno)

        tabla.addView(
            trEn,
            TableLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val numClientes = jsonClientes.length()
        //MD FOR DE LOS CLIENTES
        for (i in 0 until numClientes) {
            //SE GENERA EL OBJETO CLIENTE DEL ARREGLO
            val cte: JSONObject = jsonClientes.getJSONObject(i)

            //SOLO SI ES LA PRESIDENTA SE GUARDA EN SESION EL NOMBRE
            /*
            if (cte.getString("group_rol") == "P") {
                //FuncionesGlobales.guardarVariableSesion(requireActivity(),"String","PRESIDENTA",cte.getString("customer_name"))
                FuncionesGlobales.guardarVariableSesion(requireActivity(),"String","ID_PRESIDENTA",cte.getString("credit_id"))
            }*/

            val tr1 = TableRow(activity)
            tr1.setPadding(10,10,10,10)
            if (i != numClientes) {
                tr1.setBackgroundResource(R.drawable.borde)
            } else {
                tr1.setBackgroundResource(R.drawable.borde_redondeado_verde)
            }

            val linea = LinearLayout(activity)
            val view = ImageView(activity)
            view.setImageResource(R.drawable.ic_si_pago)
            if (cte.getInt("due") != 0) {
                view.setImageResource(R.drawable.ic_no_pago)
                view.setColorFilter(Color.RED)
            }

            view.setPadding(0, 10, 0, 10)
            linea.addView(view)
            val colorTr =ContextCompat.getColor(requireActivity(),R.color.Azul1)
            val colorTr2 =ContextCompat.getColor(requireActivity(),R.color.Verde3)
            val txtN = TextView(activity)
            txtN.setPadding(10,0,0,0)
            txtN.textSize = fontTr
            txtN.setTextColor(colorTr2)
            txtN.paintFlags = Paint.UNDERLINE_TEXT_FLAG
            txtN.gravity = Gravity.LEFT
            txtN.maxWidth = witCte

            val txtP = TextView(activity)
            txtP.setTextColor(colorTr)
            txtP.textSize = fontTr
            txtP.gravity = Gravity.CENTER

            val linea2 = LinearLayout(activity)
            val call = ImageView(activity)
            call.setPadding(0,0,30,0)
            call.setImageResource(R.drawable.call_24)

            val mensaje = ImageView(activity)
            mensaje.setImageResource(R.drawable.ic_whats)
            mensaje.setColorFilter(Color.GREEN)

            val txt = TextView(activity)
            txt.setTextColor(colorTr)
            txt.textSize = fontTr
            txt.text ="__"
            txt.alpha = 0.0F
            txt.gravity = Gravity.CENTER

            txtN.text = cte.getString("customer_name") //+" $densidad $width m$witCte"
            txtP.text = FuncionesGlobales.convertPesos(cte.getDouble("pay"),0)

            call.setOnClickListener { llamarContacto(context, cte.getString("cell_phone")) }
            mensaje.setOnClickListener {
                validacionesEnvioWhats(
                    cte.getString("cell_phone"), cte.getString("customer_name")
                )
            }

            linea.addView(txtN)
            linea.setOnClickListener { detalleCliente(cte.getInt("credit_id")) }
            tr1.addView(linea)
            tr1.addView(txtP)
            tr1.addView(txt)
            linea2.addView(call)
            linea2.addView(mensaje)
            linea2.gravity =  Gravity.CENTER
            tr1.addView(linea2)

            tabla.addView(
                tr1,
                TableLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        /*val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val grupo = prefs.getString("NOMBRE_GPO", "--")
        binding.txtMiGpo.text = grupo*/
        mostrarFormato(true)
    }
    private fun validacionesEnvioWhats(telefono: String, nombreclienta: String) {

        if (!validarAplicacionInstalada(getString(R.string.packagename_whats), context)) {
            mostrarAlertInstalarApp(context, getString(R.string.packagename_whats))
            return
        }

        enviarMensajeWhatsApp(
            context,
            getString(R.string.mensaje_whats) + " " + nombreclienta,
            telefono
        )
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
        binding.tblMiGpo.visibility = valor
    }
}
/* fun llenarTabla() {
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
                    //view.setImageResource(R.drawable.ic_no_pago_r)
                }
                3 -> {
                    nombre = "Valenzuela Sanches Martha"
                }
                4 -> {
                    nombre = "Quiroz Garcia Maria Fernanda"
                }
                5 -> {
                    nombre = "Campos Maysen Sonia"
                   // view.setImageResource(R.drawable.ic_no_pago_r)
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
*/