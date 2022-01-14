package com.example.apppresidenta.ui.inicio

import android.annotation.SuppressLint
import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.*
import com.example.apppresidenta.databinding.InicioFragmentBinding
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONObject
import org.json.JSONTokener
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class InicioFragment : Fragment() {

    private lateinit var homeViewModel: InicioViewModel
    private var _binding: InicioFragmentBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    lateinit var progressBar: CircularProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this)[InicioViewModel::class.java]
        _binding = InicioFragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        /*val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })*/
        //INDICA QUE SE HABILITARA EL MENU DE OPCIONES
        setHasOptionsMenu(true)
        binding.btnAyuda.setColorFilter(resources.getColor(R.color.Azul1))
        binding.btnAyuda.setOnClickListener{
            Toast.makeText(activity,"Boton de ayuda ", Toast.LENGTH_SHORT).show();
        }
        //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(activity as AppCompatActivity,"true")
        mostrarFormato(false)
        if (ValGlobales.validarConexion(activity as AppCompatActivity)) {
            datosDelCredito()
        }else{
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

        if(!esMostrar){
            valor = View.INVISIBLE
            valorLoadi = View.VISIBLE
        }
        progressBar.visibility = valorLoadi
        binding.txtCargando.visibility = valorLoadi
        binding.txtB.visibility = valor
        binding.txtBonificacionAcumulada.visibility = valor
        binding.textView12.visibility = valor
        binding.txtBonificacion.visibility = valor
        binding.tblInicio.visibility = valor
        binding.btnAyuda.visibility = valor
    }

    //AGREGA EL MENU DE OPCIONES A LA VISTA
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun datosDelCredito() {
        //FORMATO EN PESOS MXM
        val mx = Locale("es", "MX")
        val formatPesos: NumberFormat = NumberFormat.getCurrencyInstance(mx)
        val txtDiaPago: TextView = binding.txtDiaPago
        val txtBonificacionAcumulada: TextView = binding.txtBonificacionAcumulada
        val txtBonificacion: TextView = binding.txtBonificacion
        val txtPago: TextView = binding.txtPago
        val txtFPago: TextView = binding.txtFPago
        val txtNoPago: TextView = binding.txtNoPago
        val txtMtoPag: TextView = binding.txtMtoPag
        val txtPagos: TextView = binding.txtPagos
        val txtSaldoVencido: TextView = binding.txtSaldoVencido
        val txtAsesor: TextView = binding.txtAsesor
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val prestamo = prefs.getInt("CREDITO_ID", 0)
        //val prestamo = 119483 //para pruebas
        val dialogNo = AlertDialog.Builder(requireActivity(), R.style.ThemeOverlay_AppCompat_Dialog_Alert)
            .setTitle(Html.fromHtml("<font color='#3C8943'>Inicio</font>"))
            .setMessage("OCURRIO UN ERROR, FAVOR DE INTENTARLO MAS TARDE.")
            .setPositiveButton("Aceptar") { dialog, which ->
                dialog.cancel()
            }
        val jsonParametros = JSONObject()
        jsonParametros.put("credit_id", prestamo)

        val request =
        @SuppressLint("SetTextI18n") //<-- se agrega para admitir una variedad de configuraciones regionales sin tener que modificar código en la concatenacion de cadenas
        object : JsonObjectRequest(
            Method.POST,
            getString(R.string.urlDatosCredito),
            jsonParametros,
            Response.Listener { response ->
                try {
                    //Obtiene su respuesta json
                    val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject
                    if(jsonData.getInt("code") == 200)//si la peticion fue correcta se continua con el login
                    {
                        val jsonResults = JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                        val noPago =  jsonResults.getString("period") + " de " +jsonResults.getString("pays")
                        txtDiaPago.text = "Tu día de pago son los ${jsonResults.getString("pay_day")}"
                        val bonificacion = 858
                        txtBonificacionAcumulada.text = "  ${formatPesos.format(bonificacion)} mxn "
                        txtBonificacion.text = "  ${formatPesos.format(bonificacion)} mxn "
                        txtPago.text = formatPesos.format(jsonResults.getDouble("pay"))
                        txtFPago.text = jsonResults.getString("next_pay_date")
                        txtNoPago.text = noPago
                        txtMtoPag.text = formatPesos.format(jsonResults.getDouble("payments"))
                        txtPagos.text = jsonResults.getString("due_pay")
                        txtSaldoVencido.text = formatPesos.format(jsonResults.getDouble("min_pay"))
                        txtAsesor.text = jsonResults.getString("zone_name")
                        mostrarFormato(true)
                    }else{
                        dialogNo.show()

                    }

                } catch (e: Exception) {
                    dialogNo.show()
                }

            },
            Response.ErrorListener { error ->
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
        try {
            val queue = Volley.newRequestQueue(activity)
            //primero borramos el cache y enviamos despues la peticion
            queue.cache.clear()
            queue.add(request)
        }catch(e: Exception){
            dialogNo.setMessage("Ocurrio un error")
            dialogNo.show()
        }
        /*******  FIN ENVIO   *******/
    }

}