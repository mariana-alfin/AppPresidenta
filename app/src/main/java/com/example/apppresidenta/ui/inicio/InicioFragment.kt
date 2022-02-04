package com.example.apppresidenta.ui.inicio

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.FuncionesGlobales
import com.example.apppresidenta.FuncionesGlobales.Companion.setMaxLength
import com.example.apppresidenta.LoadingScreen
import com.example.apppresidenta.R
import com.example.apppresidenta.ValGlobales
import com.example.apppresidenta.databinding.InicioFragmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject
import org.json.JSONTokener
import java.text.NumberFormat
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
    ): View {
        try {

            homeViewModel =
                ViewModelProvider(this)[InicioViewModel::class.java]
            _binding = InicioFragmentBinding.inflate(inflater, container, false)
            val root: View = binding.root

            //INDICA QUE SE HABILITARA EL MENU DE OPCIONES
            setHasOptionsMenu(true)
            /*
        binding.btnAyuda.setColorFilter(resources.getColor(R.color.Azul1))
        binding.btnAyuda.setOnClickListener{ solicitarSoporte()
            //Toast.makeText(activity,"Boton de ayuda ", Toast.LENGTH_SHORT).show();
        }
        */
            //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
            FuncionesGlobales.guardarPestanaSesion(activity as AppCompatActivity, "true")
            mostrarFormato(false)
            if (ValGlobales.validarConexion(activity as AppCompatActivity)) {
                datosDelCredito()
            } else {
                binding.txtCargando.text = getString(R.string.noConexion)
                binding.txtCargando.gravity = Gravity.CENTER
                binding.txtCargando.visibility = View.VISIBLE
                progressBar = binding.cargando
                progressBar.visibility = View.INVISIBLE
            }

            return root
        }
        catch (e: Exception){
            val root: View = binding.root
            Toast.makeText(activity,"${e.message}", Toast.LENGTH_SHORT).show();
            return root
        }
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
    }

    //AGREGA EL MENU DE OPCIONES A LA VISTA
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_inicio, menu)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun datosDelCredito() {
        //FORMATO EN PESOS MXM
        val mx = Locale("es", "MX")
        val formatPesos: NumberFormat = NumberFormat.getCurrencyInstance(mx)
        formatPesos.maximumFractionDigits = 0

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
        val alertError = FuncionesGlobales.mostrarAlert(requireActivity(),"error",true,"Inicio",getString(R.string.error),false)
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
                        txtBonificacionAcumulada.text = "  ${formatPesos.format(bonificacion)}  "
                        //txtBonificacion.text = "  ${formatPesos.format(bonificacion)}  "
                        txtBonificacion.text = "  ${formatPesos.format(bonificacion)}  "
                        txtPago.text = formatPesos.format(jsonResults.getDouble("pay"))
                        /*MD SE GUARDA EN SESSION EL MONTO PAGO DEL CREDITO PARA LA VISTA DE PAGOS*/
                        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                        val editor = prefs.edit()
                        editor.putFloat("MONTO_SEMANAL", jsonResults.getDouble("pay").toFloat())
                        editor.apply()
                        /*****************/
                        //txtFPago.text = FuncionesGlobales.convertFecha(jsonResults.getString("next_pay_date"),"dd/MM/yyyy")
                        txtFPago.text = FuncionesGlobales.convertFecha(jsonResults.getString("next_pay_date"),"dd-MMM-yy").replace(".-","-").uppercase()


                        txtNoPago.text = noPago
                        txtMtoPag.text = formatPesos.format(jsonResults.getDouble("payments"))
                        txtPagos.text = jsonResults.getString("due_pay")
                        txtSaldoVencido.text = formatPesos.format(jsonResults.getDouble("min_pay"))
                        txtAsesor.text = jsonResults.getString("zone_name")
                        txtAsesor.textSize = 15F
                        mostrarFormato(true)
                    }else{
                        alertError.show()

                    }

                } catch (e: Exception) {
                    if (e.message != null){
                        alertError.show()
                    }
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
        try {
            val queue = Volley.newRequestQueue(activity)
            //primero borramos el cache y enviamos despues la peticion
            queue.cache.clear()
            queue.add(request)
        }catch(e: Exception){
            alertError.show()
        }
        /*******  FIN ENVIO   *******/
    }
    //FUNCIONES DE CADA OPTION
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.iAyuda -> {
                solicitarSoporte()
                true
            }
            R.id.iCalculadora -> {
                //showOption(item.title)
                //redireccionarOpcion("CALCULADORA")
                startActivity(FuncionesGlobales.redireccionarOpcion(activity as AppCompatActivity,"CALCULADORA"))
                true
            }
            R.id.iHistorial -> {
                startActivity(FuncionesGlobales.redireccionarOpcion(activity as AppCompatActivity,"MI_HISTORIAL"))
                true
            }
            R.id.iSesion-> {
                startActivity(FuncionesGlobales.cerrarSesion(activity as AppCompatActivity))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun solicitarSoporte() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(Html.fromHtml("<font color='#1F2C49' size='20'>DESCRIBE TU PROBLEMA</font>"))
        builder.setIcon(R.drawable.ic_soporte)
        val constraintLayout = getEditTextLayout(requireActivity())
        builder.setView(constraintLayout)
        val textInputLayout = constraintLayout.
        findViewWithTag<TextInputLayout>("textInputLayoutTag")
        val textInputEditText = constraintLayout.
        findViewWithTag<TextInputEditText>("textInputEditTextTag")
        builder.setPositiveButton("Aceptar"){dialog,which->
            val mensaje = textInputEditText.text
            //Toast.makeText(activity, "$mensaje", Toast.LENGTH_SHORT).show()
            enviarEmail(mensaje.toString())
        }
        builder.setNegativeButton("Cancelar",null)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int,
                                           p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int,
                                       p2: Int, p3: Int) {
                if (p0.isNullOrBlank()){
                    textInputLayout.error = "El comentario es requerido."
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .isEnabled = false
                }else{
                    textInputLayout.error = ""
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .isEnabled = true
                }
            }
        })
    }

    private fun enviarEmail(mensaje: String) {
        LoadingScreen.displayLoadingWithText(activity,"Solicitando apoyo", false)
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alertError = FuncionesGlobales.mostrarAlert(requireActivity(),"error",true,"Solicitar Soporte",getString(R.string.error),false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val prestamo = prefs.getInt("CREDITO_ID", 0)
        val jsonParametros = JSONObject()
        jsonParametros.put("credit_id", prestamo)
        jsonParametros.put("message", mensaje)

        val request = object : JsonObjectRequest(
            Method.POST,
            getString(R.string.urlEnviarEmail),
            jsonParametros,
            Response.Listener { response ->
                try {
                    val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject
                    if (jsonData.getInt("code") == 200)//si la peticion fue correcta se continua con el login
                    {
                        val jsonResults = JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                        val mensaje = jsonResults.getString("message")
                        //Toast.makeText(requireActivity(),mensaje, Toast.LENGTH_SHORT).show()
                        val alertCorrecto = FuncionesGlobales.mostrarAlert(requireActivity(),"correcto",true,"Solicitar Soporte",
                            mensaje,false)
                        alertCorrecto.show()
                    } else {
                        alertError.show()
                    }
                    LoadingScreen.hideLoading()
                } catch (e: Exception) {
                    alertError.show()
                    LoadingScreen.hideLoading()
                }
            },
            Response.ErrorListener { error ->
                //val errorD = VolleyError(String(error.networkResponse.data))
                alertError.show()
                LoadingScreen.hideLoading()
            })
        {
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
            alertError.show()
        }

        /*******  FIN ENVIO   *******/
    }

    // get edit text layout
    fun getEditTextLayout(context:Context): ConstraintLayout {
        val constraintLayout = ConstraintLayout(context)
        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        constraintLayout.layoutParams = layoutParams
        constraintLayout.id = View.generateViewId()

        val textInputLayout = TextInputLayout(context)
        textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        layoutParams.setMargins(
            18.toDp(context),
            8.toDp(context),
            18.toDp(context),
            8.toDp(context)
        )
        textInputLayout.layoutParams = layoutParams
        textInputLayout.hint = "Un asesor se pondrá en contacto con usted."
        textInputLayout.id = View.generateViewId()
        textInputLayout.tag = "textInputLayoutTag"


        val textInputEditText = TextInputEditText(context)
        textInputEditText.id = View.generateViewId()
        textInputEditText.tag = "textInputEditTextTag"
        textInputEditText.setMaxLength(150)//maximo largo del mensaje

        textInputLayout.addView(textInputEditText)

        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        constraintLayout.addView(textInputLayout)
        return constraintLayout
    }


    // extension method to convert pixels to dp
    fun Int.toDp(context: Context):Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,this.toFloat(),context.resources.displayMetrics
    ).toInt()

}