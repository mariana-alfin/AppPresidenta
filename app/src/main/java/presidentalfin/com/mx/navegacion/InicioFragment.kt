package presidentalfin.com.mx.navegacion

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import presidentalfin.com.mx.R
import presidentalfin.com.mx.databinding.Inicio2FragmentBinding
import presidentalfin.com.mx.generales.FuncionesGlobales
import presidentalfin.com.mx.generales.FuncionesGlobales.Companion.setMaxLength
import presidentalfin.com.mx.generales.LoadingScreen
import presidentalfin.com.mx.generales.ValGlobales
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject
import org.json.JSONTokener


class InicioFragment : Fragment() {

    private var _binding: Inicio2FragmentBinding? = null
    private val binding get() = _binding!!
    lateinit var progressBar: CircularProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {

            _binding = Inicio2FragmentBinding.inflate(inflater, container, false)
            val root: View = binding.root

            //MD INDICA QUE SE HABILITARA EL MENU DE OPCIONES
            setHasOptionsMenu(true)
            //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
            FuncionesGlobales.guardarPestanaSesion(activity as AppCompatActivity, "true")
            mostrarFormato(false)
            if (ValGlobales.validarConexion(activity as AppCompatActivity)) {
                try {
                    datosDelCredito()

                }catch (e: java.lang.Exception){
                    Toast.makeText(activity,"sf", Toast.LENGTH_SHORT).show()
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
        catch (e: Exception){
            val root: View = binding.root
            Toast.makeText(activity,"${e.message}", Toast.LENGTH_SHORT).show();
            return root
        }
    }

    // MD FUNCION PARA OCULTAR LOS DATOS Y MOSTRAR CARGANDO
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
        binding.txtBonificacion.visibility = valor
        //binding.tblInicio.visibility = valor
        binding.lblInicio.visibility = valor
    }

    //MD AGREGA EL MENU DE OPCIONES A LA VISTA
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_inicio, menu)
        (activity as AppCompatActivity).supportActionBar?.title = HtmlCompat.fromHtml("<font face='montserrat_extra_bold_italic'>Inicio</font>",
        HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    //MD HACE LA CONEXION CON EL WS PARA OBTENER LOS DATOS
    private fun datosDelCredito() {
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
        val alertError = FuncionesGlobales.mostrarAlert(
            requireActivity(),
            "error",
            true,
            "Inicio",
            getString(R.string.error),
            false
        )
        try {
            val jsonParametros = JSONObject()
            jsonParametros.put("credit_id", prestamo)

            val request =
                @SuppressLint("SetTextI18n") //<-- MD SE AGREGA PARA ADMITIR UNA VARIEDAD DE CONFIGURACIONES REGIONALES SIN TENER QUE MODIFICAR CÓDIGO EN LA CONCATENACION DE CADENAS
                object : JsonObjectRequest(
                    Method.POST,
                    getString(R.string.urlDatosCredito),
                    jsonParametros,
                    Response.Listener { response ->
                        try {
                            // MD OBTIENE SU RESPUESTA JSON
                            val jsonData =
                                JSONTokener(response.getString("data")).nextValue() as JSONObject
                            if (jsonData.getInt("code") == 200)//si la peticion fue correcta se continua con el login
                            {
                                val jsonResults =
                                    JSONTokener(jsonData.getString("results")).nextValue() as JSONObject
                                val noPago =
                                    jsonResults.getString("period") + " de " + jsonResults.getString(
                                        "pays"
                                    )
                                //txtDiaPago.text = "Tu día de pago son los ${jsonResults.getString("pay_day")}"
                                txtDiaPago.text = jsonResults.getString("pay_day")
                                val bonificacion = 858
                                txtBonificacionAcumulada.text = "  ${FuncionesGlobales.convertPesos(1235.0,0)}  "
                                txtBonificacion.text = "  ${FuncionesGlobales.convertPesos(289.0,0)}  "
                                txtPago.text = FuncionesGlobales.convertPesos(jsonResults.getDouble("pay"),0)
                                /*MD SE GUARDA EN SESSION EL MONTO PAGO DEL CREDITO PARA LA VISTA DE PAGOS*/
                                FuncionesGlobales.guardarVariableSesion(requireActivity(),"Float","MONTO_SEMANAL",jsonResults.getString("pay"))
                                FuncionesGlobales.guardarVariableSesion(requireActivity(),"String","FECHA_PAGO_CONCILIACION",jsonResults.getString("next_pay_date"))
                                /*MD SE AGREGA QUE GUARDE EL NUMERO DE PAGO ACTUAL PARA LA TABLA DE BONIFICACIONES*/
                                FuncionesGlobales.guardarVariableSesion(requireActivity(),"Int","NO_PAGO_ACTUAL",jsonResults.getString("period"))

                                /*****************/
                                //txtFPago.text = FuncionesGlobales.convertFecha(jsonResults.getString("next_pay_date"),"dd/MM/yyyy")
                                txtFPago.text = FuncionesGlobales.convertFecha(
                                    jsonResults.getString("next_pay_date"), "dd-MMM-yy"
                                ).replace(".-", "-").uppercase()


                                txtNoPago.text = noPago
                                txtMtoPag.text =
                                    FuncionesGlobales.convertPesos(jsonResults.getDouble("payments"),0)
                                txtPagos.text = jsonResults.getString("due_pay")
                                txtSaldoVencido.text =
                                    FuncionesGlobales.convertPesos(jsonResults.getDouble("min_pay"),0)
                                txtAsesor.text = jsonResults.getString("zone_name")
                                mostrarFormato(true)
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
                        //val errorD = VolleyError(String(error.networkResponse.data))
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

                            if (code == 422 && jsonData.getString("results").contains("credit_id")){
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
    //MD FUNCIONES DE CADA OPTION DEL MENU
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.iAyuda -> {
                solicitarSoporte()
                true
            }
            /*R.id.iCalculadora -> {
                //showOption(item.title)
                //redireccionarOpcion("CALCULADORA")
                startActivity(
                    FuncionesGlobales.redireccionarOpcion(
                        activity as AppCompatActivity,
                        "CALCULADORA"
                    )
                )
                true
            }*/
            R.id.iHistorial -> {
                startActivity(
                    FuncionesGlobales.redireccionarOpcion(
                        activity as AppCompatActivity,
                        "MI_HISTORIAL"
                    )
                )
                true
            }
            R.id.iSesion -> {
                startActivity(FuncionesGlobales.cerrarSesion(activity as AppCompatActivity))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun solicitarSoporte() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(HtmlCompat.fromHtml("<font color='#1F2C49' size='20' face='montserrat_medium_italic'>DESCRIBE TU PROBLEMA</font>", HtmlCompat.FROM_HTML_MODE_LEGACY))
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
        LoadingScreen.displayLoadingWithText(activity, "Solicitando apoyo", false)
        /**************   MD  ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alertError = FuncionesGlobales.mostrarAlert(
            requireActivity(),
            "error",
            true,
            "Solicitar Soporte",
            getString(R.string.error),
            false
        )
        try {
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
                        val alertCorrecto = FuncionesGlobales.mostrarAlert(
                            requireActivity(), "correcto", true, "Solicitar Soporte",
                            mensaje, false
                        )
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
            Response.ErrorListener {
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
            val queue = Volley.newRequestQueue(activity)
            //MD PRIMERO BORRAMOS EL CACHE Y ENVIAMOS DESPUES LA PETICION
            queue.cache.clear()
            queue.add(request)
        }catch(e: Exception){
            alertError.show()
        }

        /*******  FIN ENVIO   *******/
    }
    // MD MUESTRA EL INPUT PARA AGREGAR EL MENSAJE EN EL DIALOG
    fun getEditTextLayout(context:Context): ConstraintLayout {
        val tipoLetra = ResourcesCompat.getFont(requireActivity(), R.font.montserrat_medium_italic)
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
        textInputLayout.hint = "Un gerente se pondrá en contacto con usted."
        textInputLayout.counterMaxLength = 150
        textInputLayout.id = View.generateViewId()
        textInputLayout.tag = "textInputLayoutTag"
        textInputLayout.typeface = tipoLetra

        val textInputEditText = TextInputEditText(context)
        textInputEditText.id = View.generateViewId()
        textInputEditText.tag = "textInputEditTextTag"
        textInputEditText.setMaxLength(150)//maximo largo del mensaje
        textInputEditText.typeface = tipoLetra
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
