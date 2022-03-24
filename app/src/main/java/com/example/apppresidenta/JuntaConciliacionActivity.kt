package com.example.apppresidenta

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.TableLayout.generateViewId
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.FuncionesGlobales.Companion.eliminaVariableSesion
import com.example.apppresidenta.generales.FuncionesGlobales.Companion.setMaxLength
import com.example.apppresidenta.generales.LoadingScreen
import com.example.apppresidenta.generales.ValGlobales
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*

/*MD ESTE ACTIVITY ES UN REPLICA DE JUNTA LA UNICA DIFERENCIA ES QUE NO SE TOMA FOTOGRAFIA Y SE ENVIAN LOS ID_PAGOS RECIVIDOS*/
class JuntaConciliacionActivity : CameraBaseActivity() {
    lateinit var progressBar: CircularProgressIndicator

    class CteIds {
        var idTxtPago: Int
        var idSolidario: Int
        var idChk: Int

        constructor(idTxtPago: Int, idSolidario: Int, idChk: Int) {
            this.idTxtPago = idTxtPago
            this.idSolidario = idSolidario
            this.idChk = idChk
        }
    }

    var listClientes: MutableMap<Int, CteIds> = mutableMapOf(0 to CteIds(1, 1, 1))
    var listPagos: MutableMap<Int, Int> = mutableMapOf(0 to 0)
    var saldoPorConciliar: Double = 0.0
    var idPagosArray: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_junta_conciliacion)
        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "true")

        //Se elimina la ruta de la fotografia y ubicacion para generar una nueva junta en caso de que
        //el usuario no haya concluido la actividad
        eliminaVariableSesion(this, "LATITUD")
        eliminaVariableSesion(this, "LONGITUD")

        val parametros = this.intent.extras
        val saldoConciliar = parametros!!.getDouble("saldoConciliar", 0.0)
        val idPagos = parametros.getString("idPagos", "")
        idPagosArray = idPagos.substring(0, idPagos.length - 1)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val FECHA_PAGO_CONCILIACION = prefs.getString("FECHA_PAGO_CONCILIACION", "")
        val fecha = FuncionesGlobales.convertFecha(FECHA_PAGO_CONCILIACION!!, "dd-MMM-yy")
            .replace(".-", "-").uppercase()
        //findViewById<TextView>(R.id.txtDatosPago).text = "  Fecha de pago: ${if (fecha.contains(".")) fecha.replace('.','-') else fecha}   "
        findViewById<TextView>(R.id.txtDatosPago).text = "  Fecha de pago: $fecha"
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            guardarJunta(FECHA_PAGO_CONCILIACION)
        }

        /*MD SE AGREGA LOGO, TITULO Y SUBTITULO DEL LA ACTIVIDAD*/
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = FuncionesGlobales.convertPesos(saldoConciliar,2)
        supportActionBar?.subtitle = "Saldo por Conciliar"
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        //SE ASIGNA VARIABLE
        saldoPorConciliar = saldoConciliar
        mostrarFormato(false)
        if (ValGlobales.validarConexion(this)) {
            datosJunta("", false)
            //SE OBTIENE LA UBICACION
            solicitarUsoUbicacion(this)
        } else {
            findViewById<TextView>(R.id.txtCargando).text = getString(R.string.noConexion)
            findViewById<TextView>(R.id.txtCargando).gravity = Gravity.CENTER
            findViewById<TextView>(R.id.txtCargando).visibility = View.VISIBLE
            progressBar = findViewById(R.id.cargando)
            progressBar.visibility = View.INVISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return false
    }

    //MD AGREGA EL MENU DE OPCIONES
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu to use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_junta_c, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //MD FUNCIONES DE CADA OPTION
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.iCopy -> {
                copiarMontos()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun copiarMontos() {
        val alert = FuncionesGlobales.mostrarAlert(
            this,
            "cuestion",
            true,
            "Pago completo",
            "¿Está seguro que todas las clientas pagaron completo?",
            true
        )
        alert.setPositiveButton("Aceptar") { _, _ ->
            //solo pruebas val fechaPago = "2021-09-07"
            if (ValGlobales.validarConexion(this)) {
                LoadingScreen.displayLoadingWithText(this, "Cargando ...", false)
                datosJunta("", true)
            } else {
                findViewById<TextView>(R.id.txtCargando).text = getString(R.string.noConexion)
                findViewById<TextView>(R.id.txtCargando).gravity = Gravity.CENTER
                findViewById<TextView>(R.id.txtCargando).visibility = View.VISIBLE
                progressBar = findViewById(R.id.cargando)
                progressBar.visibility = View.INVISIBLE
            }
        }
        alert.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        alert.create()
        alert.show()
    }

    private fun mostrarFormato(esMostrar: Boolean) {
        var valor = View.VISIBLE
        var valorLoadi = View.INVISIBLE
        progressBar = findViewById(R.id.cargando)

        if (!esMostrar) {
            valor = View.INVISIBLE
            valorLoadi = View.VISIBLE
        }
        progressBar.visibility = valorLoadi
        findViewById<TextView>(R.id.txtCargando).visibility = valorLoadi
        findViewById<TextView>(R.id.txtDatosPago).visibility = valor
        findViewById<Button>(R.id.btnGuardar).visibility = valor
    }

    private fun showOpcionesSolidario(
        v: View,
        menuRes: Int,
        idTxt: Int,
        idSelect: Int,
        idtxtPago: Int,
        montoCuota: Double
    ) { //idtxtPago,mCuota
        //se obtiene el txt por el id
        val txt = findViewById<TextView>(idTxt)
        //se obtiene el icono del combo por el id
        val ddl = findViewById<ImageView>(idSelect)
        val popup = PopupMenu(this, v)
        popup.menuInflater.inflate(menuRes, popup.menu)
        popup.setOnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.option_1 -> {
                    //Toast.makeText(this, item.title, Toast.LENGTH_SHORT).show()
                    txt.text = "NA"
                    txt.error = null
                }
                R.id.option_2 -> {
                    //txt.text = item.title
                    txt.text = "DA"
                    txt.error = null
                    validaPagoSolidario("DA", idtxtPago, montoCuota)
                }
                R.id.option_3 -> {
                    txt.text = "RE"
                    txt.error = null
                    validaPagoSolidario("RE", idtxtPago, montoCuota)
                }
            }
            ddl.visibility = View.GONE
            true
        }
        popup.show()
    }

    private fun validaPagoSolidario(tipoSolidario: String, idtxtPago: Int, montoCuota: Double) {
        try {
            //SE OBTIENE EL MONTO CAPTURADO EN EL PAGO PARA COMPARARLO CON LA COUTA
            val montoPago = findViewById<EditText>(idtxtPago)
            if ((montoPago.text.toString()
                    .toDouble() == montoCuota) && (tipoSolidario == "DA" || tipoSolidario == "RE")
            ) {
                montoPago.setText("0")
            }
        } catch (e: Exception) {
            //Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    //SE REUTILIZA LA FUNCION DE DATOS DEL GRUPO DEL FRAGMENT MIGRUPO
    private fun datosJunta(fechaPago: String, esCopia: Boolean) {
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alertError = FuncionesGlobales.mostrarAlert(
            this,
            "error",
            true,
            "Guardar Junta",
            getString(R.string.error),
            false
        )
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val prestamo = prefs.getInt("CREDITO_ID", 0)
            val jsonParametros = JSONObject()
            jsonParametros.put("credit_id", prestamo)
            jsonParametros.put("pay_date", fechaPago)

            val request =
                @SuppressLint("SetTextI18n") //<-- se agrega para admitir una variedad de configuraciones regionales sin tener que modificar código en la concatenacion de cadenas
                object : JsonObjectRequest(
                    Method.POST,
                    getString(R.string.urlMiembrosGrupo),
                    jsonParametros,
                    Response.Listener { response ->
                        try {
                            //Obtiene su respuesta json
                            val jsonData =
                                JSONTokener(response.getString("data")).nextValue() as JSONObject
                            if (jsonData.getInt("code") == 200) {
                                val jsonResults = jsonData.getJSONArray("results")
                                pintarTablaJunta(jsonResults, esCopia)
                            }
                        } catch (e: Exception) {
                        }
                    },
                    Response.ErrorListener { error ->
                        val codigoError = error.networkResponse.statusCode
                        try {
                            if (codigoError == 422) {
                                alertError.setMessage("El ID de Crédito no se encontro.")
                            } else {
                                alertError.setMessage("Error: $codigoError \n${getString(R.string.errorServidor)}")
                            }
                        } catch (e: Exception) {
                            val codigo = error.networkResponse.statusCode
                            alertError.setMessage("Error: $codigo \n${getString(R.string.errorServidor)}")
                        }
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
            val queue = Volley.newRequestQueue(this)
            //primero borramos el cache y enviamos despues la peticion
            queue.cache.clear()
            queue.add(request)
        } catch (e: java.lang.Exception) {
            alertError.show()
        }

        /*******  FIN ENVIO   *******/
    }

    @SuppressLint("SetTextI18n")
    private fun pintarTablaJunta(jsonClientes: JSONArray, esCopia: Boolean) {
        //se obtiene la tabla
        val tabla = findViewById<TableLayout>(R.id.tblJunta)
        tabla.removeAllViews()
        //ENCABEZADO
        val fontTh = 16F
        val fontTr = 13.5F
        val trEn = TableRow(this)
        trEn.setPadding(0, 20, 0, 20)
        val cliente = TextView(this)
        cliente.text = "Cliente"
        cliente.gravity = Gravity.CENTER
        cliente.setTextColor(Color.WHITE)
        cliente.setTypeface(null, Typeface.BOLD_ITALIC)
        cliente.textSize = fontTh
        cliente.maxWidth = 130
        trEn.addView(cliente)

        val cuota = TextView(this)
        cuota.text = "Cuota"
        cuota.gravity = Gravity.CENTER
        cuota.setTextColor(Color.WHITE)
        cuota.setTypeface(null, Typeface.BOLD_ITALIC)
        cuota.textSize = fontTh
        trEn.addView(cuota)

        val pago = TextView(this)
        pago.text = "Pago"
        pago.gravity = Gravity.CENTER
        pago.setTextColor(Color.WHITE)
        pago.setTypeface(null, Typeface.BOLD_ITALIC)
        pago.textSize = fontTh
        trEn.addView(pago)

        val sol = TextView(this)
        sol.text = "Solidario"
        sol.gravity = Gravity.CENTER
        sol.setTextColor(Color.WHITE)
        sol.setTypeface(null, Typeface.BOLD_ITALIC)
        sol.textSize = fontTh
        trEn.addView(sol)

        val so = TextView(this)
        so.text = "______"
        so.gravity = Gravity.CENTER
        so.setTextColor(ContextCompat.getColor(this, R.color.Verde2))
        so.setTypeface(null, Typeface.BOLD_ITALIC)
        so.textSize = fontTh
        trEn.addView(so)

        trEn.gravity = Gravity.CENTER
        trEn.setBackgroundResource(R.drawable.redondo_verde)
        tabla.addView(
            trEn
        )
        val numClientes = jsonClientes.length()
        //for de los clientes
        //SE BORRA LA LISTA DE CTES
        listClientes.clear()

        /**     VARIABLES PARA TAMAÑOS      **/
        //SE OBTIENE LA DENSIDAD DEL DISPOSITIVO PARA CAMBIAR LOS TAMAÑOS
        val densidad = resources.displayMetrics.densityDpi
        val width = resources.displayMetrics.widthPixels
        var witCte: Int
        var witPgo = 140
        if (densidad < DisplayMetrics.DENSITY_HIGH) {
            witCte = 140
        } else if (densidad in DisplayMetrics.DENSITY_HIGH until DisplayMetrics.DENSITY_XHIGH) {
            witCte = 200
        } else if (densidad == DisplayMetrics.DENSITY_XHIGH) {
            witCte = 220
        } else if (densidad in DisplayMetrics.DENSITY_XHIGH until DisplayMetrics.DENSITY_XXHIGH) {
            witCte = 380
            witPgo = 200
        } else if (densidad == DisplayMetrics.DENSITY_XXHIGH && width == 1080) {
            witCte = 380
            witPgo = 175
        } else if ((densidad in DisplayMetrics.DENSITY_XXHIGH until DisplayMetrics.DENSITY_XXXHIGH) && width < 1200 && width != 1080) {
            witCte = 415
            witPgo = 205
        } else if ((densidad >= DisplayMetrics.DENSITY_XXXHIGH) || width <= 1200) {
            witCte = 430
            witPgo = 215
        } else {//SI LA PANTALLA ES MAYOR A 1200PX EL VALOR DEL NOMBRE SERA DE UNA TERCERA PARTE
            witCte = (width / 3)
            witPgo = 225
        }
        /***       FIN VARIABLES       **/
        for (i in 0 until numClientes) {
            //SE GENERA EL OBJETO CLIENTE DEL ARREGLO
            val cte: JSONObject = jsonClientes.getJSONObject(i)
            val tr = TableRow(this)
            tr.setPadding(10, 20, 0, 20)
            if (i != numClientes) {
                tr.setBackgroundResource(R.drawable.borde)
            } else {
                tr.setBackgroundResource(R.drawable.borde_redondeado_verde)
            }
            val l = LinearLayout(this)
            val chk = CheckBox(this)
            val idCk = generateViewId()
            chk.id = idCk
            chk.gravity = Gravity.RIGHT
            val colorAzul = ContextCompat.getColor(this, R.color.Azul1)
            val cliente = TextView(this)
            cliente.setTextColor(colorAzul)
            cliente.textSize = (fontTr - 1)
            //cliente.maxWidth = 230
            cliente.maxWidth = witCte

            val couta = TextView(this)
            val mCuota = cte.getDouble("pay")
            couta.text = FuncionesGlobales.convertPesos(mCuota,2)
            couta.setTextColor(colorAzul)
            couta.textSize = fontTr

            val pago = EditText(this)
            val idtxtPago = cte.getInt("credit_id")
            pago.id = idtxtPago
            pago.inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            pago.keyListener = DigitsKeyListener.getInstance(".0123456789")
            pago.setTextColor(colorAzul)
            pago.textSize = fontTr
            pago.gravity = Gravity.CENTER
            pago.width = witPgo
            pago.maxWidth = witPgo
            pago.setMaxLength(7)
            pago.background.setColorFilter(colorAzul, PorterDuff.Mode.SRC_ATOP)
            pago.doAfterTextChanged { sumaPagos() }

            val lS = LinearLayout(this)
            val sol = TextView(this)
            val idT = generateViewId()
            sol.id = idT
            sol.setTypeface(null, Typeface.BOLD)
            sol.textSize = (fontTr + 1)
            sol.setTextColor(ContextCompat.getColor(this, R.color.Verde5))
            sol.setPadding(15, 0, 0, 0)
            sol.gravity = Gravity.CENTER

            val checlSol = ImageView(this)
            val idS = generateViewId()
            checlSol.id = idS
            checlSol.setImageResource(R.drawable.drop_down_36)
            checlSol.setColorFilter(ContextCompat.getColor(this, R.color.Azul1))
            lS.setOnClickListener { v: View ->
                showOpcionesSolidario(v, R.menu.opciones_solidario, idT, idS, idtxtPago, mCuota)
            }

            //CUANDO ES COPIA DE MONTOS SE ASIGNA EL MONTO DE LA CUOTA AL PAGO Y SE PONE NO APLICA AL SOLIDARIO
            if (esCopia) {
                pago.setText(cte.getString("pay"))// cte.getString("pay")
                sol.text = "NA"
                checlSol.visibility = View.GONE
                LoadingScreen.hideLoading()
            }
            //SE AGREGAN LOS VALORES
            cliente.text = cte.getString("customer_name")//+ " $witCte w$width $witPgo"
            //SE AGREGA A LA LISTA DE IDs
            listClientes.put(cte.getInt("credit_id"), CteIds(idtxtPago, idT, idCk))
            //SE AGREGA A LA LISTA DE SUMA DE PAGO
            listPagos.put(cte.getInt("credit_id"), idtxtPago)

            //SE AGREGA COLUMNA CLIENTE
            l.addView(chk)
            l.addView(cliente)
            tr.addView(l)
            //SE AGREGA COLUMNA COUTA
            tr.addView(couta)
            //SE AGREGA COLUMNA PAGO
            tr.addView(pago)
            //SE COLUMNA AGREGA SOLIDARIO
            lS.addView(checlSol)
            lS.addView(sol)
            tr.addView(lS)

            tr.gravity = Gravity.CENTER
            tabla.addView(tr)
        }
        if (!esCopia) {
            mostrarFormato(true)
        } else {
            Toast.makeText(this, "Montos copiados correctamente", Toast.LENGTH_SHORT).show()
        }

    }

    private fun sumaPagos() {
        //MD SE REALIZA LA AUTOSUMA DE LOS MONTOS CAPTURADOS
        var sumaPagos = 0.0
        for (pgo in listPagos) {
            try {
                val txtPago = findViewById<EditText>(pgo.value)       //TEXT DE MONTO PAGO
                if (txtPago.text.isNotEmpty()) {                      //SOLO SI NO ESTA VACIO SE AGREGA ALA SUMA
                    sumaPagos += txtPago.text.toString().toDouble()
                    findViewById<TextView>(R.id.txtSuma).text = FuncionesGlobales.convertPesos(sumaPagos,2)
                    if (sumaPagos > saldoPorConciliar) {
                        Toast.makeText(
                            this,
                            "El monto capturado no debe ser mayor al Saldo por Conciliar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun guardarJunta(fechaPagoConciliacion: String) {
        //VARIABLE PARA SABER SI HAY VACIOS PARA CONTINUAR ESTA DEBE DE ESTAR EN CERO
        var hayVacios = 0
        var saldoConciliacion = 0.0
        //se genera el json y se envia el metodo para enviarlo al ws
        //var clientes = "\"clientes\" : [ "
        var pagos = "\"tasks\" : [ "
        //SE RECORRE EL LISTADO DE CLIENTES Y LOS IDS DE SUS INPUTS
        for (cte in listClientes) {
            try {
                val txtPago = findViewById<EditText>(cte.value.idTxtPago)       //TEXT DE MONTO PAGO
                val txtSolidario =
                    findViewById<TextView>(cte.value.idSolidario)//TEXT DE SOLIDARIO (NA,DA,RE)
                val chkAsistencia =
                    findViewById<CheckBox>(cte.value.idChk)     //TEXT DE SOLIDARIO (NA,DA,RE)
                //var respuesta = "idCliente ${cte.key}"
                if (txtPago.text.isNotEmpty()) {                                //SI NO ESTA VACIO SE AGREGA ALA RESPUESTA
                    saldoConciliacion += txtPago.text.toString().toDouble()
                    //se agrega al string del json
                    pagos += "{" +
                            " \"credit_id\" : ${cte.key}," +
                            " \"amount\" : \"${txtPago.text}\","
                } else { //CONTRARIO SE MANDA EL ERROR
                    txtPago.error = "Es requerido"
                    hayVacios++
                }
                pagos += " \"description\" : null,"                          //se agrega description siempre en blanco
                if (txtSolidario.text.isNotEmpty()) {
                    val solidario_v = when (txtSolidario.text) {
                        "DA" -> {
                            11
                        }
                        "RE" -> {
                            12
                        }
                        "NA" -> {
                            13
                        }
                        else -> {
                            13
                        }
                    }
                    pagos += " \"result_type_id\" : \"${solidario_v}\"," //MD SE QUITA Y SE USA RESULT-TYPE-ID
                } else {
                    txtSolidario.error = "Es requerido"
                    hayVacios++
                }
                pagos += " \"validate\" : \"${if (chkAsistencia.isChecked) "1" else "0"}\"}," //se agrega la asistencia de la clienta
                //Toast.makeText(this, "$respuesta , hayVacios $hayVacios ", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                //Toast.makeText(this, "ex ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        var pagosString = pagos.substring(0, pagos.length - 1)
        pagosString += " ]"

        //SE MUESTRA PARA PRUEBAS
        //findViewById<TextView>(R.id.txtJson).text = pagosString
        //SI EL MONTO NO ES IGUAL AL DE CONCILIAR NO DEJA CONTINUAR
        if (saldoConciliacion != saldoPorConciliar) {
            val alertCorrecto = FuncionesGlobales.mostrarAlert(
                this,
                "advertencia",
                true,
                "Guardar Junta",
                "El total debe ser igual al Saldo por Conciliar",
                false
            )
            alertCorrecto.show()
        }
        //SI HAY VACIOS ES 0 CONTINUA DE LO CONTRARIO NO
        else if (hayVacios == 0 && saldoConciliacion == saldoPorConciliar) {
            val alertCorrecto = FuncionesGlobales.mostrarAlert(
                this,
                "cuestion",
                true,
                "¿Está seguro de continuar?",
                "na",
                true
            )
            alertCorrecto.setPositiveButton("Aceptar") { _, _ ->
                //SE VALIDA QUE EXISTA CONEXION
                if (ValGlobales.validarConexion(this)) {
                    generarJson(pagos, fechaPagoConciliacion)
                }
            }
            alertCorrecto.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }
            alertCorrecto.create()
            alertCorrecto.show()
        } else {
            val alert = FuncionesGlobales.mostrarAlert(
                this,
                "error",
                true,
                "Guardar Junta",
                "Debe capturar todos los datos, para continuar",
                false
            )
            alert.create()
            alert.show()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun generarJson(pagosStrings: String, fechaPagoConciliacion: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val prestamo = prefs.getInt("CREDITO_ID", 0)
        //se genera el json
        var JSJuntaC = ""
        val valSolicitud = "{" +
                " \"credit_id\" :$prestamo," +
                " \"date_accrual\" : \"$fechaPagoConciliacion\"," + //fecha de pago
                " \"close_task\" : 1," +
                " \"payments\" : [$idPagosArray],"

        //Se obtienen las coordenadas de las ultima ubicacion registrada
        val latitude = prefs.getString("LATITUD", "")
        val longitude = prefs.getString("LONGITUD", "")

        val valGeoreferencia = "\"georeference_data\" : { " +
                " \"longitude\" :$longitude," +
                " \"latitude\" : $latitude," +
                " \"image\" : null}"

        //JSJuntaC = "$valSolicitud ${pagosStrings.substring(0, pagosStrings.length - 1)}]}"
        JSJuntaC = "$valSolicitud ${
            pagosStrings.substring(
                0,
                pagosStrings.length - 1
            )
        }], $valGeoreferencia}"
        try {
            val jsonJuntaC = JSONObject(JSJuntaC)
            Log.e("DatosJunta", jsonJuntaC.toString());
            enviarJuntaConciliacion(jsonJuntaC)
            //findViewById<TextView>(R.id.txtJson).text = jsonJuntaC.toString()
        } catch (e: Exception) {
        }
    }

    private fun enviarJuntaConciliacion(jsonJuntaC: JSONObject) {
        LoadingScreen.displayLoadingWithText(this, "Enviando Información...", false)
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/

        val alertError = FuncionesGlobales.mostrarAlert(
            this,
            "error",
            true,
            "Guardar Junta",
            getString(R.string.error),
            false
        )
        val alertCorrecto = FuncionesGlobales.mostrarAlert(
            this,
            "correcto",
            true,
            "Datos guardados correctamente",
            "na",
            true
        )

        val request = object : JsonObjectRequest(
            Method.POST,
            getString(R.string.urlGuardarJuntaConciliacion),
            jsonJuntaC,
            Response.Listener { response ->
                try {
                    //Obtiene su respuesta json
                    //Toast.makeText(this, "Respuesta: $response", Toast.LENGTH_SHORT).show()
                    val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject
                    if (jsonData.getInt("code") == 201)//si la peticion fue correcta se continua con el login
                    {
                        alertCorrecto.setPositiveButton("Aceptar") { _, _ ->
                            //Se elimina la ruta de la fotografia y ubicacion para generar una nueva junta
                            eliminaVariableSesion(this, "LATITUD")
                            eliminaVariableSesion(this, "LONGITUD")
                            //se finaliza la actividad
                            finish()
                        }
                        alertCorrecto.create()
                        alertCorrecto.show()

                    } else {
                        alertError.show()
                    }
                    LoadingScreen.hideLoading()
                } catch (e: Exception) {
                    LoadingScreen.hideLoading()
                    alertError.show()
                }
            },
            Response.ErrorListener { error ->
                var mensaje = getString(R.string.error)
                try {
                    val responseError = String(error.networkResponse.data)
                    val dataError = JSONObject(responseError)
                    JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                } catch (e: Exception) {
                    val codigo = error.networkResponse.statusCode
                    mensaje = "Error: $codigo \n${getString(R.string.errorServidor)}"
                }
                alertError.setMessage(mensaje)
                alertError.show()
                LoadingScreen.hideLoading()
            }) {
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
            val queue = Volley.newRequestQueue(this)
            //MD PRIMERO BORRAMOS EL CACHE Y ENVIAMOS DESPUES LA PETICION
            queue.cache.clear()
            queue.add(request)
        } catch (e: Exception) {
            alertError.show()
        }
        /*******  FIN ENVIO   *******/
    }

}