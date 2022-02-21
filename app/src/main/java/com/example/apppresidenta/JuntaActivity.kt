package com.example.apppresidenta

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.TableLayout.generateViewId
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.FuncionesGlobales.Companion.setMaxLength
import com.example.apppresidenta.generales.LoadingScreen
import com.example.apppresidenta.generales.ValGlobales
import com.example.apppresidenta.utils.GeneralUtils.Companion.eliminaVariableSesion
import com.example.apppresidenta.utils.GeneralUtils.Companion.eliminarFotos
import com.example.apppresidenta.utils.GeneralUtils.Companion.obtenerCadenaB64DeImagen
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*


class JuntaActivity : CameraBaseActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.junta_activity)
        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "true")

        val parametros = this.intent.extras
        val esEditar = parametros!!.getBoolean("esEdicion", false)
        val semana = parametros.getInt("numPago", 1)
        val fechaPago = parametros.getString("fechaPago", "")

        //Se elimina la ruta de la fotografia y ubicacion para generar una nueva junta en caso de que
        //el usuario no haya concluido la actividad
        eliminaVariableSesion(this, "RUTA_FOTO")
        eliminaVariableSesion(this, "LATITUD")
        eliminaVariableSesion(this, "LONGITUD")

        if (esEditar) {
            solicitarUsoUbicacion(this)
        }
        //val fecha = FuncionesGlobales.convertFecha(fechaPago,"dd/MM/yyyy")
        val fecha = FuncionesGlobales.convertFecha(fechaPago, "dd-MMM-yy").replace(".-", "-").uppercase()
        //findViewById<TextView>(R.id.txtDatosPago).text = "  Fecha de pago: ${if (fecha.contains(".")) fecha.replace('.','-') else fecha}   "
        findViewById<TextView>(R.id.txtDatosPago).text = "  Fecha de pago: $fecha"
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardarJunta() }

        /*Se agrega logo y titulo del la actividad*/
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "SEMANA $semana"
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)

        //pintarTablaJunta()
        mostrarFormato(false)

        if (ValGlobales.validarConexion(this)) {
            datosJunta(fechaPago, false)
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
        inflater.inflate(R.menu.menu_junta, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //MD FUNCIONES DE CADA OPTION
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.iCamara -> {
                solicitarUsarCamara(this)
                true
            }
            R.id.iCopy -> {
                copiarMontos()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun copiarMontos() {
        /* MD SE GENERA FUNCION PARA COPIAR LOS MONTOS*/
        val alert = FuncionesGlobales.mostrarAlert(
            this,
            "cuestion",
            true,
            "Pago completo",
            "¿Está seguro que todas las clientas pagaron completo?",
            true
        )
        alert.setPositiveButton("Aceptar") { _, _ ->
            val parametros = this.intent.extras
            val fechaPago = parametros!!.getString("fechaPago", "")
            //solo pruebas
            //val fechaPago = "2021-09-07"
            if (ValGlobales.validarConexion(this)) {
                LoadingScreen.displayLoadingWithText(this, "Cargando ...", false)
                datosJunta(fechaPago, true)
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
        //findViewById<TextView>(R.id.txtTitle).visibility = valor
        findViewById<TextView>(R.id.txtDatosPago).visibility = valor
        findViewById<Button>(R.id.btnGuardar).visibility = valor
    }

    //MD FUNCION PARA CAMBIAR EL SELECT POR EL TEXTO DEL SOLIDARIO
    private fun showOpcionesSolidario(
        v: View,
        menuRes: Int,
        idTxt: Int,
        idSelect: Int,
        idtxtPago: Int,
        montoCuota: Double
    ) { //idtxtPago,mCuota
        //MD SE OBTIENE EL TXT POR EL ID
        val txt = findViewById<TextView>(idTxt)
        //MD SE OBTIENE EL ICONO DEL COMBO POR EL ID
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
            //MD SE OBTIENE EL MONTO CAPTURADO EN EL PAGO PARA COMPARARLO CON LA COUTA
            val montoPago = findViewById<EditText>(idtxtPago)
            if ((montoPago.text.toString()
                    .toDouble() == montoCuota) && (tipoSolidario == "DA" || tipoSolidario == "RE")
            ) {
                //   Toast.makeText(this, "monto pago debe de ser 0", Toast.LENGTH_SHORT).show()
                montoPago.setText("0")
            }
        } catch (e: Exception) {
            //Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

    }

    //MD SE REUTILIZA LA FUNCION DE DATOS DEL GRUPO DEL FRAGMENT MIGRUPO
    private fun datosJunta(fechaPago: String, esCopia: Boolean) {
        /**************   MD  ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
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
                                //Toast.makeText(activity, "PETICION EXITOSA", Toast.LENGTH_SHORT).show()
                                val jsonResults = jsonData.getJSONArray("results")
                                pintarTablaJunta(jsonResults, esCopia)
                            }
                        } catch (e: Exception) {
                            alertError.show()
                        }
                    },
                    Response.ErrorListener { error ->
                        val codigoError = error.networkResponse.statusCode
                        if (codigoError == 422) {
                            alertError.setMessage("El ID de Crédito no se encontro.")
                        } else {
                            alertError.setMessage("Error: $codigoError \n${getString(R.string.errorServidor)}")
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
            //MD PRIMERO BORRAMOS EL CACHE Y ENVIAMOS DESPUES LA PETICION
            queue.cache.clear()
            queue.add(request)
        } catch (e: Exception) {
            // Couldn't properly decode data to string
            alertError.show()
        }
        /*******  FIN ENVIO   *******/
    }

    @SuppressLint("SetTextI18n")
    private fun pintarTablaJunta(jsonClientes: JSONArray, esCopia: Boolean) {
        //SE OBTIENE LA TABLA
        val tabla = findViewById<TableLayout>(R.id.tblJunta)
        tabla.removeAllViews()
        //ENCABEZADO
        val fontTh = 17F
        val fontTr = 14F
        val trEn = TableRow(this)
        trEn.setPadding(0, 20, 0, 20)
        val cliente = TextView(this)
        cliente.text = "Cliente"
        //cliente.setPadding(0, 20, 20, 20)
        cliente.gravity = Gravity.CENTER
        cliente.setTextColor(Color.WHITE)
        cliente.setTypeface(null, Typeface.BOLD_ITALIC)
        cliente.textSize = fontTh
        cliente.maxWidth = 130
        trEn.addView(cliente)

        val cuota = TextView(this)
        cuota.text = "Cuota"
        //cuota.setPadding(0, 20, 20, 20)
        cuota.gravity = Gravity.CENTER
        cuota.setTextColor(Color.WHITE)
        cuota.setTypeface(null, Typeface.BOLD_ITALIC)
        cuota.textSize = fontTh
        trEn.addView(cuota)

        val pago = TextView(this)
        pago.text = "Pago"
        //pago.setPadding(0, 20, 0, 20)
        pago.gravity = Gravity.CENTER
        pago.setTextColor(Color.WHITE)
        pago.setTypeface(null, Typeface.BOLD_ITALIC)
        pago.textSize = fontTh
        trEn.addView(pago)

        val sol = TextView(this)
        sol.text = "Solidario"
        //sol.setPadding(0, 20, 0, 20)
        sol.gravity = Gravity.CENTER
        sol.setTextColor(Color.WHITE)
        sol.setTypeface(null, Typeface.BOLD_ITALIC)
        sol.textSize = fontTh
        trEn.addView(sol)

        val so = TextView(this)
        so.text = "______"
        so.alpha = 0.0F //MD HACE COMPLETAMENTE OPACO UN ELEMENTO
        so.gravity = Gravity.CENTER
        so.setTextColor(ContextCompat.getColor(this, R.color.Verde2))
        so.setTypeface(null, Typeface.BOLD_ITALIC)
        so.textSize = fontTh
        trEn.addView(so)

        trEn.gravity = Gravity.CENTER
        trEn.setBackgroundResource(R.drawable.redondo_verde)
        tabla.addView(
            trEn//, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        val numClientes = jsonClientes.length()
        //val ctsList: MutableMap<String, String> = mutableMapOf()
        //for de los clientes
        //MD SE BORRA LA LISTA DE CTES PARA EVITAR DUPLICADOS
        listClientes.clear()

        /**     VARIABLES PARA TAMAÑOS      **/
        //MD SE OBTIENE LA DENSIDAD DEL DISPOSITIVO PARA CAMBIAR LOS TAMAÑOS
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
        } else {//MD SI LA PANTALLA ES MAYOR A 1200PX EL VALOR DEL NOMBRE SERA DE UNA TERCERA PARTE
            witCte = (width / 3)
            witPgo = 225
        }
        /***       FIN VARIABLES       **/
        for (i in 0 until numClientes) {
            //MD SE GENERA EL OBJETO CLIENTE DEL ARREGLO PARA ACCEDER A SUS DATOS
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
            couta.text = FuncionesGlobales.convertPesos(mCuota,0)
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
            //pago.width = 140
            pago.width = witPgo
            //pago.maxWidth = 180
            pago.maxWidth = witPgo
            pago.setMaxLength(7)
            pago.background.setColorFilter(colorAzul, PorterDuff.Mode.SRC_ATOP)

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
            checlSol.setColorFilter(colorAzul)
            lS.setOnClickListener { v: View ->
                showOpcionesSolidario(v, R.menu.opciones_solidario, idT, idS, idtxtPago, mCuota)
            }

            //MD CUANDO ES COPIA DE MONTOS SE ASIGNA EL MONTO DE LA CUOTA AL PAGO Y SE PONE NO APLICA AL SOLIDARIO
            if (esCopia) {
                pago.setText(cte.getString("pay"))// cte.getString("pay")
                sol.setText("NA")
                checlSol.visibility = View.GONE
                LoadingScreen.hideLoading()
            }
            //MD SE AGREGAN LOS VALORES
            cliente.text = cte.getString("customer_name")//+ " $witCte w$width $witPgo"
            //MD SE AGREGA A LA LISTA DE IDs PARA ACCEDER A SU CONTENIDO EN LA VALIDACION DE DATOS
            listClientes.put(cte.getInt("credit_id"), CteIds(idtxtPago, idT, idCk))

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

    /* MD SE GENERA FUNCION PARA VALIDAR SI E SPOSIBLE GUARDAR LOS DATOS DE LA JUNTA
          PARA ELLO SE VALIDA QUE TODOS LOS DATOS ESTEN CAPTURADOS
          Y SE CONSTRUYE EL JSON DE LOS DATOS DE CLIENTES
       */
    private fun guardarJunta() {
        //VARIABLE PARA SABER SI HAY VACIOS PARA CONTINUAR ESTA DEBE DE ESTAR EN CERO
        var hayVacios = 0

        //Si no hay foto tomada manda alert para realizar la actividad
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val rutaFotoActual = prefs.getString("RUTA_FOTO", "")

        //MD SE GENERA EL JSON Y SE ENVIA EL METODO PARA ENVIARLO AL WS
        //var clientes = "\"clientes\" : [ "
        var clientes = "\"tasks\" : [ "
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
                    //respuesta += "montoPago = ${txtPago.text}"
                    //se agrega al string del json
                    clientes += "{" +
                            " \"credit_id\" : ${cte.key}," +
                            " \"amount\" : \"${txtPago.text}\","
                } else { //CONTRARIO SE MANDA EL ERROR
                    txtPago.error = "Es requerido"
                    hayVacios++
                }
                clientes += " \"description\" : null,"                          //se agrega description siempre en blanco
                //clientes += " \"result_type_id\" : \"1\","                      //se agrega result_type_id siempre en 1 //se reemplaza por el valor del solidario
                if (txtSolidario.text.isNotEmpty()) {
                    //respuesta += "solidario = ${txtSolidario.text}"
                    //clientes += " \"solidario\" : \"${txtSolidario.text}\" }," //MD SE QUITA Y SE USA RESULT-TYPE-ID
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
                    clientes += " \"result_type_id\" : \"${solidario_v}\"," //MD SE QUITA Y SE USA RESULT-TYPE-ID
                } else {
                    txtSolidario.error = "Es requerido"
                    hayVacios++
                }
                clientes += " \"validate\" : \"${if (chkAsistencia.isChecked) "1" else "0"}\"}," //se agrega la asistencia de la clienta
                //Toast.makeText(this, "$respuesta , hayVacios $hayVacios ", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                //Toast.makeText(this, "ex ${e.message}", Toast.LENGTH_SHORT).show()
                Toast.makeText(
                    this,
                    "Ocurrio un error, favor de intentarlo más tarde ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        var clientesString = clientes.substring(0, clientes.length - 1)
        clientesString += " ]," //se modifica agrenaod una coma para agregar la georeferencia

        //SE MUESTRA PARA PRUEBAS
        //findViewById<TextView>(R.id.txtJson).text = clientesString

        //MD SI HAY VACIOS ES 0 CONTINUA DE LO CONTRARIO NO
        if (hayVacios == 0 && rutaFotoActual != "") {
            val alertCorrecto = FuncionesGlobales.mostrarAlert(
                this,
                "cuestion",
                true,
                "¿Está seguro de continuar?",
                "na",
                true
            )
            alertCorrecto.setPositiveButton("Aceptar") { _, _ ->
                //MD SE VALIDA QUE EXISTA CONEXION PARA ENVIAR LOS DATOS
                if (ValGlobales.validarConexion(this)) {
                    generarJson(clientesString)
                }
            }
            alertCorrecto.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }
            alertCorrecto.create()
            alertCorrecto.show()
        } else if (rutaFotoActual == "") {
            val alert = FuncionesGlobales.mostrarAlert(
                this,
                "error",
                true,
                "Guardar Junta",
                "Es necesario tomar foto como evidencia, para continuar",
                false
            )
            alert.create()
            alert.show()
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
    private fun generarJson(clientesString: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val prestamo = prefs.getInt("CREDITO_ID", 0)
        val parametros = this.intent.extras
        val idPago = parametros!!.getInt("idPago", 0)
        val fechaPago = parametros.getString("fechaPago", "")
        //MD SE GENERA EL JSON COMPLEMENTARIO DE LA SOLICITUD
        var JSJunta = ""
        val hoy = FuncionesGlobales.obtenerFecha("yyyy-MM-dd")
        val valSolicitud = "{" +
                " \"credit_id\" :$prestamo," +
                " \"date_accrual\" : \"$fechaPago\"," + //fecha de pago
                " \"close_task\" : 1," +
                " \"date_task\" : \"$hoy\"," +
                " \"idPago\" :$idPago,"

        //Se obtienen las coordenadas de las ultima ubicacion registrada
        val latitude = prefs.getString("LATITUD", "")
        val longitude = prefs.getString("LONGITUD", "")
        val rutaFotoActual = prefs.getString("RUTA_FOTO", "")
        val imagenB64 = obtenerCadenaB64DeImagen(rutaFotoActual!!)
        val valGeoreferencia = "\"georeference_data\" : { " +
                " \"longitude\" :$longitude," +
                " \"latitude\" : $latitude," +
                " \"image\" : \"data:image/jpeg;base64,$imagenB64\"}"

        JSJunta = "$valSolicitud $clientesString $valGeoreferencia }"
        val jsonJunta = JSONObject(JSJunta)
        //findViewById<TextView>(R.id.txtJson).text = jsonJunta.toString()
        //Log.e("MyActivity", jsonJunta.toString());
        enviarJunta(jsonJunta)
    }

    private fun enviarJunta(jsonJunta: JSONObject) {
        /* MD FUNCION QUE TOMA EL JSON DE LOS DATOS DE LA JUNTA Y LO ENVIA */
        LoadingScreen.displayLoadingWithText(this, "Enviando Información...", false)
        /**************   MD  ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val alertError = FuncionesGlobales.mostrarAlert(
            this,
            "error",
            true,
            "Guardar Junta",
            getString(R.string.error),
            false
        )
        try {
            val request = object : JsonObjectRequest(
                Method.POST,
                getString(R.string.urlGuardarJunta),
                jsonJunta,// <- MD SE ENVIA DIRECTAMENTE EL JSON GENERADO CON LOS DATOS DE LA JUNTA
                Response.Listener { response ->
                    try {
                        //Obtiene su respuesta json
                        //Toast.makeText(this, "Respuesta: $response", Toast.LENGTH_SHORT).show()
                        val jsonData =
                            JSONTokener(response.getString("data")).nextValue() as JSONObject
                        if (jsonData.getInt("code") == 201)//si la peticion fue correcta se continua con el login
                        {
                            val alertCorrecto = FuncionesGlobales.mostrarAlert(
                                this,
                                "correcto",
                                true,
                                "Datos guardados correctamente",
                                "na",
                                true
                            )
                            alertCorrecto.setPositiveButton("Aceptar") { _, _ ->

                                //Se elimina la ruta de la fotografia y ubicacion para generar una nueva junta
                                eliminaVariableSesion(this, "RUTA_FOTO")
                                eliminaVariableSesion(this, "LATITUD")
                                eliminaVariableSesion(this, "LONGITUD")

                                //Se eliminan las fotos de la carpeta de la app
                                val directorio =
                                    getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()
                                eliminarFotos(directorio)

                                //MD SE FINALIZA LA ACTIVIDAD
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
            val queue = Volley.newRequestQueue(this)
            //MD PRIMERO BORRAMOS EL CACHE Y ENVIAMOS DESPUES LA PETICION
            queue.cache.clear()
            queue.add(request)
        } catch (e: Exception) {
            alertError.show()
        }
        /*******  FIN ENVIO   *******/
    }

    /*
    private fun pintarTablaJuntaSinJson() {
        //se obtiene la tabla
        val tabla = findViewById<TableLayout>(R.id.tblJunta)
        //ENCABEZADO
        val fontTh = 16F
        val fontTr = 14F
        val trEn = TableRow(this)
        /* val rol = TextView(this)
         rol.text = "Rol"
         rol.setPadding(30, 20, 0, 20)
         rol.gravity = Gravity.RIGHT
         //rol.maxWidth = 50
         rol.setTextColor(Color.WHITE)
         rol.setTypeface(null, Typeface.BOLD_ITALIC)
         rol.textSize = fontTh
         trEn.addView(rol)*/

        val cliente = TextView(this)
        cliente.text = "Cliente"
        cliente.setPadding(0, 20, -20, 20)
        cliente.gravity = Gravity.CENTER
        cliente.setTextColor(Color.WHITE)
        cliente.setTypeface(null, Typeface.BOLD_ITALIC)
        cliente.textSize = fontTh
        cliente.maxWidth = 200
        trEn.addView(cliente)

        val cuota = TextView(this)
        cuota.text = "Cuota"
        cuota.setPadding(0, 20, -20, 20)
        cuota.gravity = Gravity.CENTER
        cuota.setTextColor(Color.WHITE)
        cuota.setTypeface(null, Typeface.BOLD_ITALIC)
        cuota.textSize = fontTh
        trEn.addView(cuota)

        val pago = TextView(this)
        pago.text = "Pago"
        pago.setPadding(0, 20, 0, 20)
        pago.gravity = Gravity.CENTER
        pago.setTextColor(Color.WHITE)
        pago.setTypeface(null, Typeface.BOLD_ITALIC)
        pago.textSize = fontTh
        trEn.addView(pago)

        val sol = TextView(this)
        sol.text = "Solidario"
        sol.setPadding(0, 20, 0, 20)
        sol.gravity = Gravity.CENTER
        sol.setTextColor(Color.WHITE)
        sol.setTypeface(null, Typeface.BOLD_ITALIC)
        sol.textSize = fontTh
        trEn.addView(sol)

        val so = TextView(this)
        so.text = "______"
        so.setPadding(0, 20, 0, 20)
        so.gravity = Gravity.CENTER
        so.setTextColor(resources.getColor(R.color.Verde2))
        so.setTypeface(null, Typeface.BOLD_ITALIC)
        so.textSize = fontTh
        trEn.addView(so)

        trEn.gravity = Gravity.CENTER
        trEn.setBackgroundResource(R.drawable.redondo_verde)
        tabla.addView(
            trEn, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        val numClientes = 12
        //for del numero de clientes
        for (i in 1..numClientes) {
            val tr = TableRow(this)
            if (i != numClientes) {
                tr.setBackgroundResource(R.drawable.borde)
            } else {
                tr.setBackgroundResource(R.drawable.borde_redondeado_verde)
            }

            val l = LinearLayout(this)
            val vRol = ImageView(this)
            vRol.setImageResource(R.drawable.miembro)
            vRol.setColorFilter(resources.getColor(R.color.Verde3))
            vRol.setPadding(-5, 0, 15, 0)

            val cliente = TextView(this)
            cliente.setPadding(0, 0, 5, 0)
            cliente.setTextColor(resources.getColor(R.color.Azul1))
            cliente.textSize = fontTr
            cliente.maxWidth = 300

            val couta = TextView(this)
            couta.text = formatPesos.format(2000)
            couta.setPadding(5, 20, 15, 20)
            couta.setTextColor(resources.getColor(R.color.Azul1))
            couta.textSize = fontTr

            /*val pago = TextView(this)
            pago.text = formatPesos.format(200)
            pago.setPadding(5, 20, 10, 20)
            pago.setTextColor(resources.getColor(R.color.Azul1))
            pago.setTypeface(null, Typeface.BOLD)
            pago.textSize = fontTr*/
            val pago = EditText(this)
            pago.setBackgroundResource(R.drawable.borde)
            pago.inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            pago.keyListener = DigitsKeyListener.getInstance(".0123456789")
            pago.setPadding(5, 20, 10, 20)
            pago.setTextColor(resources.getColor(R.color.Azul1))
            pago.textSize = fontTr
            pago.gravity = Gravity.CENTER
            pago.width = 160
            pago.maxWidth = 250

            val lS = LinearLayout(this)
            val sol = TextView(this)
            val idT = generateViewId()
            sol.id = idT
            sol.setTypeface(null, Typeface.BOLD)
            sol.textSize = (fontTr + 1)
            sol.setTextColor(resources.getColor(R.color.Verde5))
            sol.setPadding(3, 10, 10, 10)
            sol.gravity = Gravity.CENTER

            val checlSol = ImageView(this)
            val idS = generateViewId()
            checlSol.id = idS
            checlSol.setImageResource(R.drawable.drop_down_36)
            checlSol.setColorFilter(resources.getColor(R.color.Azul1))
            lS.setOnClickListener { v: View ->
                showOpcionesSolidario(v, R.menu.opciones_solidario, idT, idS)
            }

            //SE AGREGAN LOS VALORES
            var cte: String
            when (i) {
                1 -> {
                    cte = "Arellano lara lopez lola Fonseca Ana Bertha"
                    vRol.setImageResource(R.drawable.presidenta)
                }
                2 -> {
                    cte = "Delgadillo Lara Martha"
                    vRol.setImageResource(R.drawable.secretaria)
                }
                3 -> {
                    cte = "Valenzuela Sanches Martha"
                    vRol.setImageResource(R.drawable.tesorera)
                }
                4 -> {
                    cte = "Quiroz Garcia Maria Fernanda"
                }
                5 -> {
                    cte = "Campos Maysen Sonia"

                }
                6 -> {
                    cte = "Rosas Carmona Connie"
                }
                7 -> {
                    cte = "Pintle Salinas Cerda Lourdes Maria Leticia"
                }
                else -> {
                    cte = "Trejo Contreras Maria"
                }
            }
            cliente.text = cte

            //SE AGREGA COLUMNA DEL ROL
            l.addView(vRol)
            l.addView(cliente)
            tr.addView(l)
            //SE AGREGA COLUMNA CLIENTE
            //tr.addView(cliente)
            //SE AGREGA COLUMNA COUTA
            tr.addView(couta)
            //SE AGREGA COLUMNA PAGO
            tr.addView(pago)
            //SE COLUMNA AGREGA SOLIDARIO
            lS.addView(sol)
            lS.addView(checlSol)
            tr.addView(lS)

            tr.gravity = Gravity.CENTER
            tabla.addView(
                tr, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            )
        }
    }*/
}

