package com.example.apppresidenta

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Html
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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apppresidenta.FuncionesGlobales.Companion.setMaxLength
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.text.NumberFormat
import java.util.*



class JuntaActivity : CameraBaseActivity() {
    //FORMATO EN PESOS MXM
    private val mx = Locale("es", "MX")
    private val formatPesos: NumberFormat = NumberFormat.getCurrencyInstance(mx)
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

    var listClientes: MutableMap<Int, CteIds> = mutableMapOf(0 to CteIds(1, 1,1))

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.junta_activity)
        //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "true")

          val parametros = this.intent.extras
          val esEditar = parametros!!.getBoolean("esEdicion", false)
          val semana = parametros.getInt("numPago", 1)
          val fechaPago = parametros.getString("fechaPago", "")

        if(esEditar){
            solicitarUsoUbicacion(this)
        }
        /*findViewById<TextView>(R.id.txtTitle).text = "Ingresa los pagos de los integrantes"
        if (!esEditar) {
            findViewById<TextView>(R.id.txtTitle).text = "Los pagos ya fueron guardados"
        }*/
        val fecha = FuncionesGlobales.convertFecha(fechaPago,"dd/MM/yyyy")
        findViewById<TextView>(R.id.txtDatosPago).text = "  Fecha de pago: $fecha   "
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardarJunta() }
        //findViewById<Button>(R.id.btnGuardar).setOnClickListener { this.onBackPressed() } //ejeuta el persionar atras
        /*Se agrega logo y titlulo del la actividad*/
        val actionBar: ActionBar? = supportActionBar
        actionBar?.title = "SEMANA $semana"
        actionBar?.setLogo(R.mipmap.icono_app)
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayUseLogoEnabled(true)

        //pintarTablaJunta()
        mostrarFormato(false)
        //pintarTablaJunta()
        if (ValGlobales.validarConexion(this)) {
            datosJunta(fechaPago, false)
        } else {
            findViewById<TextView>(R.id.txtCargando).text = getString(R.string.noConexion)
            findViewById<TextView>(R.id.txtCargando).gravity = Gravity.CENTER
            findViewById<TextView>(R.id.txtCargando).visibility = View.VISIBLE
            progressBar = findViewById(R.id.cargando)
            progressBar.visibility = View.INVISIBLE
        }
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val width = metrics.widthPixels // ancho absoluto en pixels
        val height = metrics.heightPixels // alto absoluto en pixels
        val widthd = this.resources.displayMetrics.widthPixels
        val heightd = this.resources.displayMetrics.heightPixels
       // findViewById<TextView>(R.id.txtJson).text = "width = $width $widthd height = $height $heightd"
    }

    //AGREGA EL MENU DE OPCIONES
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu to use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_junta, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //FUNCIONES DE CADA OPTION
    @RequiresApi(Build.VERSION_CODES.M)
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
        AlertDialog.Builder(this)
            .setTitle(Html.fromHtml("<font color='#1F2C49'>Pago completo</font>"))
            .setMessage("¿Está seguro que todas las clientas pagaron completo?")
            .setPositiveButton("Aceptar") { _, _ ->
                val parametros = this.intent.extras
                val fechaPago = parametros!!.getString("fechaPago", "")
                //solo pruebas
                //val fechaPago = "2021-09-07"
                if (ValGlobales.validarConexion(this)) {
                    LoadingScreen.displayLoadingWithText(this,"Cargando ...",false)
                     datosJunta(fechaPago, true)
                } else {
                    findViewById<TextView>(R.id.txtCargando).text = getString(R.string.noConexion)
                    findViewById<TextView>(R.id.txtCargando).gravity = Gravity.CENTER
                    findViewById<TextView>(R.id.txtCargando).visibility = View.VISIBLE
                    progressBar = findViewById(R.id.cargando)
                    progressBar.visibility = View.INVISIBLE
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
            // Create the AlertDialog object and return it
            .create()
            .show()
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

    /*private fun tomarFoto() {
        Toast.makeText(this, "TOMAR FOTOGRAFIA", Toast.LENGTH_SHORT).show()
    }*/

    private fun showOpcionesSolidario(v: View,menuRes: Int,idTxt: Int,idSelect: Int,idtxtPago: Int,montoCuota: Double) { //idtxtPago,mCuota
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
            //se obtiene el monto capturado en el pago para compararlo con la couta
            val montoPago = findViewById<EditText>(idtxtPago)
            if((montoPago.text.toString().toDouble() == montoCuota) && (tipoSolidario == "DA" || tipoSolidario == "RE")){
           //   Toast.makeText(this, "monto pago debe de ser 0", Toast.LENGTH_SHORT).show()
                montoPago.setText("0")
            }
        } catch (e: Exception) {
          //Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

    }

    //se reutiliza la funcion de datos del grupo del fragment MiGrupo
    private fun datosJunta(fechaPago: String, esCopia: Boolean) {
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val prestamo = prefs.getInt("CREDITO_ID", 0)
        //val prestamo = 119483 //para pruebas
        val dialogNo = AlertDialog.Builder(this, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
            .setTitle(Html.fromHtml("<font color='#3C8943'>Ingresar</font>"))
            .setMessage("OCURRIO UN ERROR, FAVOR DE INTENTARLO MAS TARDE.")
            .setPositiveButton("Aceptar") { dialog, which ->
                dialog.cancel()
            }
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
                        dialogNo.setMessage(getString(R.string.error))
                        //  dialogNo.show()
                    }
                },
                Response.ErrorListener {
                    val codigoError = it.networkResponse.statusCode
                    /*findViewById<TextView>(R.id.txtPruebas).text = "$codigoError"
                    Toast.makeText(this, "$it", Toast.LENGTH_SHORT).show()*/
                    if (codigoError == 422) {
                        dialogNo.setMessage(Html.fromHtml("El ID de Crédito no se encontro."))
                    } else {
                        dialogNo.setMessage("Ocurrio un error")
                    }
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
        val queue = Volley.newRequestQueue(this)
        //primero borramos el cache y enviamos despues la peticion
        queue.cache.clear()
        queue.add(request)
        /*******  FIN ENVIO   *******/
    }

    @SuppressLint("SetTextI18n")
    private fun pintarTablaJunta(jsonClientes: JSONArray, esCopia: Boolean) {
        //se obtiene la tabla
        val tabla = findViewById<TableLayout>(R.id.tblJunta)
        tabla.removeAllViews()
        //ENCABEZADO
        val fontTh = 16F
        val fontTr = 14F
        val trEn = TableRow(this)
        trEn.setPadding(0,20,0,20)
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
        //so.setPadding(0, 20, 0, 20)
        so.gravity = Gravity.CENTER
        so.setTextColor(resources.getColor(R.color.Verde2))
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
        //SE BORRA LA LISTA DE CTES
        listClientes.clear()
        for (i in 0 until numClientes) {
            //SE GENERA EL OBJETO CLIENTE DEL ARREGLO
            val cte: JSONObject = jsonClientes.getJSONObject(i)
            val tr = TableRow(this)
            tr.setPadding(0,10,0,10)
            if (i != numClientes) {
                tr.setBackgroundResource(R.drawable.borde)
            } else {
                tr.setBackgroundResource(R.drawable.borde_redondeado_verde)
            }

            val l = LinearLayout(this)
            val cliente = TextView(this)
            cliente.setPadding(0, 10, 0, 10)
            //cliente.setPadding(0, 20, 0, 20)
            cliente.setTextColor(resources.getColor(R.color.Azul1))
            cliente.textSize = (fontTr - 1)
            cliente.maxWidth = 230

            val chk = CheckBox(this)
            val idCk = generateViewId()
            chk.id = idCk
            chk.gravity = Gravity.CENTER_HORIZONTAL
            l.addView(chk)

            val couta = TextView(this)
            val mCuota = cte.getDouble("pay")
            couta.text = formatPesos.format(mCuota)
            //couta.setPadding(0, 20, 15, 20)
            //couta.setPadding(0, 10, 0, 10)
            couta.setTextColor(resources.getColor(R.color.Azul1))
            couta.textSize = fontTr

            val pago = EditText(this)
            //pago.setBackgroundResource(R.drawable.borde)
            val idtxtPago = cte.getInt("credit_id")
            pago.id = idtxtPago
            //pago.hint = idtxtPago.toString()
            pago.inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            pago.keyListener = DigitsKeyListener.getInstance(".0123456789")
            pago.setTextColor(resources.getColor(R.color.Azul1))
            pago.textSize = fontTr
            pago.gravity = Gravity.CENTER
            pago.width = 140
            pago.maxWidth = 180
            pago.setMaxLength(7)

            val lS = LinearLayout(this)
            val sol = TextView(this)
            val idT = generateViewId()
            sol.id = idT
            sol.setTypeface(null, Typeface.BOLD)
            sol.textSize = (fontTr + 1)
            sol.setTextColor(resources.getColor(R.color.Verde5))
            sol.setPadding(15,0,0,0)
            sol.gravity = Gravity.CENTER

            val checlSol = ImageView(this)
            val idS = generateViewId()
            checlSol.id = idS
            checlSol.setImageResource(R.drawable.drop_down_36)
            checlSol.setColorFilter(resources.getColor(R.color.Azul1))
            lS.setOnClickListener { v: View ->
                showOpcionesSolidario(v, R.menu.opciones_solidario, idT, idS, idtxtPago, mCuota)
            }

            //CUANDO ES COPIA DE MONTOS SE ASIGNA EL MONTO DE LA CUOTA AL PAGO Y SE PONE NO APLICA AL SOLIDARIO
            if (esCopia) {
                pago.setText(cte.getString("pay"))// cte.getString("pay")
                sol.setText("NA")
                checlSol.visibility = View.GONE
                LoadingScreen.hideLoading()
            }
            //SE AGREGAN LOS VALORES
            cliente.text = cte.getString("customer_name")
            //SE AGREGA A LA LISTA DE IDs
            listClientes.put(cte.getInt("credit_id"), CteIds(idtxtPago, idT, idCk))

            //SE AGREGA COLUMNA CLIENTE
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

    private fun guardarJunta() {
        //VARIABLE PARA SABER SI HAY VACIOS PARA CONTINUAR ESTA DEBE DE ESTAR EN CERO
        var hayVacios = 0
        //se genera el json y se envia el metodo para enviarlo al ws
        //var clientes = "\"clientes\" : [ "
        var clientes = "\"tasks\" : [ "
        //SE RECORRE EL LISTADO DE CLIENTES Y LOS IDS DE SUS INPUTS
        for (cte in listClientes) {
            try {
                val txtPago = findViewById<EditText>(cte.value.idTxtPago)       //TEXT DE MONTO PAGO
                val txtSolidario = findViewById<TextView>(cte.value.idSolidario)//TEXT DE SOLIDARIO (NA,DA,RE)
                val chkAsistencia = findViewById<CheckBox>(cte.value.idChk)     //TEXT DE SOLIDARIO (NA,DA,RE)
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
                clientes += " \"result_type_id\" : \"1\","                      //se agrega result_type_id siempre en 1
                clientes += " \"validate\" : \"${if (chkAsistencia.isChecked) "1" else "0"}\"," //se agrega la asistencia de la clienta
                if (txtSolidario.text.isNotEmpty()) {
                    //respuesta += "solidario = ${txtSolidario.text}"
                    clientes += " \"solidario\" : \"${txtSolidario.text}\" },"
                } else {
                    txtSolidario.error = "Es requerido"
                    hayVacios++
                }
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
        clientesString += " ]"

        //SE MUESTRA PARA PRUEBAS
        //findViewById<TextView>(R.id.txtJson).text = clientesString

        //SI HAY VACIOS ES 0 CONTINUA DE LO CONTRARIO NO
        if (hayVacios == 0) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(Html.fromHtml("<font color='#1F2C49'>¿Está seguro de continuar?</font>"))
                .setPositiveButton("Aceptar",
                    DialogInterface.OnClickListener { _, _ ->
                        //LoadingScreen.displayLoadingWithText(this,"Please wait...",false)
                        generarJson(clientesString)
                    })
                .setNegativeButton("Cancelar",
                    DialogInterface.OnClickListener { dialog, _ ->
                        dialog.cancel()
                    })
            // Create the AlertDialog object and return it
            builder.create()
            builder.show()
        } else {
            AlertDialog.Builder(this)
                .setTitle(Html.fromHtml("<font color='#1F2C49' size='10' >Guardar Junta</font>"))
                .setMessage("Debe capturar todos los datos, para continuar")
                .setPositiveButton("Aceptar", null)
                .create()
                .show()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun generarJson(clientesString: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val prestamo = prefs.getInt("CREDITO_ID", 0)
        val parametros = this.intent.extras
        val idPago = parametros!!.getInt("idPago", 0)
        val fechaPago = parametros.getString("fechaPago", "")
        //se genera el json
        var JSJunta = ""
        val hoy = FuncionesGlobales.obtenerFecha("yyyy-MM-dd")
        val valSolicitud = "{" +
                " \"credit_id\" :$prestamo," +
                " \"date_accrual\" : \"$fechaPago\","+ //fecha de pago
                " \"close_task\" : 1," +
                " \"date_task\" : \"$hoy\","+
                " \"idPago\" :$idPago,"
        JSJunta = "$valSolicitud $clientesString }"
        val jsonJunta = JSONObject(JSJunta)
        //findViewById<TextView>(R.id.txtJson).text = jsonJunta.toString()
        Log.e("MyActivity", jsonJunta.toString());
        enviarJunta(jsonJunta)
    }

    private fun enviarJunta(jsonJunta: JSONObject) {
        LoadingScreen.displayLoadingWithText(this, "Enviando Información...",false)
        /**************     ENVIO DE DATOS AL WS PARA GENERAR LA SOLICITUD Y GUARDA LA RESPUESTA EN SESION   **************/

        val dialogNo = AlertDialog.Builder(this, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
            .setTitle(Html.fromHtml("<font color='#3C8943'>Ingresar</font>"))
            .setMessage("OCURRIO UN ERROR, FAVOR DE INTENTARLO MAS TARDE.")
            .setPositiveButton("Aceptar") { dialog, which ->
                dialog.cancel()
            }

        val request = object : JsonObjectRequest(
            Method.POST,
            getString(R.string.urlGuardarJunta),
            jsonJunta,
            Response.Listener { response ->
                try {
                    //Obtiene su respuesta json
                    //Toast.makeText(this, "Respuesta: $response", Toast.LENGTH_SHORT).show()
                    val jsonData = JSONTokener(response.getString("data")).nextValue() as JSONObject
                    if (jsonData.getInt("code") == 201)//si la peticion fue correcta se continua con el login
                    {
                        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                        AlertDialog.Builder(this)
                            .setTitle(Html.fromHtml("<font color='#1F2C49'>Datos guardados correctamente</font>"))
                            .setPositiveButton("Aceptar") { _, _ ->
                                //se finaliza la actividad
                                finish()
                            }
                            .create()
                            .show()

                    } else {
                        dialogNo.setMessage("$response")
                        dialogNo.show()
                    }
                    LoadingScreen.hideLoading()
                } catch (e: Exception) {
                    LoadingScreen.hideLoading()
                    dialogNo.setMessage(e.message)
                    dialogNo.show()
                }
            },
            Response.ErrorListener { error ->
                //val errorD = VolleyError(String(error.networkResponse.data))
                val responseError = String(error.networkResponse.data)
                val dataError = JSONObject(responseError)
                try {
                    val jsonData = JSONTokener(dataError.getString("error")).nextValue() as JSONObject
                    val message = jsonData.getString("message")
                    dialogNo.setMessage(message)
                }catch (e: Exception){
                    dialogNo.setMessage(getString(R.string.error))
                }
                dialogNo.show()
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
            val queue = Volley.newRequestQueue(this)
            //primero borramos el cache y enviamos despues la peticion
            queue.cache.clear()
            queue.add(request)
        }catch(e: Exception){
            //dialogNo.setMessage("Ocurrio un error ${e.message}")
            dialogNo.setMessage(getString(R.string.error))
            dialogNo.show()
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

