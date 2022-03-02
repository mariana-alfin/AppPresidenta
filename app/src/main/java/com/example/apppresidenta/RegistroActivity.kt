package com.example.apppresidenta

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.example.apppresidenta.generales.AppSignatureHelper
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.ReceptorSMS
import com.google.android.gms.auth.api.phone.SmsRetriever
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.random.Random

class RegistroActivity : AppCompatActivity(), ReceptorSMS.OTPReceiveListener {
    private var smsReceiver: ReceptorSMS? = null
    var recuperarNip: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registro_activity)
        //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "MainActivity")
        //ME SE ENVIA EL MENSAJE CON EL CODIGO
        enviarCodigo()
        /*MD se agrega logo y titulo del la actividad*/
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)//flecha atras
        supportActionBar?.title = HtmlCompat.fromHtml("<font color='#2C3B62'>"+ getString(R.string.vCodigo)+"</font>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_bc6))

        textChangueCodigo()//onchangue de text
        val prefs = getDefaultSharedPreferences(this)
        val parametros = this.intent.extras
        val celularEnvio = parametros!!.getString("celular", "")
        recuperarNip = parametros!!.getBoolean("recuperarNip", false)
        findViewById<TextView>(R.id.txtCodigoP).text = prefs.getString("CODIGO_VERIFICADOR", "0")
        findViewById<TextView>(R.id.textView8).text = "Hemos enviado un mensaje SMS con tu código de verificación al número $celularEnvio, por favor ingrésalo."

        //MD SE OBTIENE EL HASHKEY DE LA APP PARA LA LECTURA DEL SMS
        val appSignature = AppSignatureHelper(this)
        findViewById<TextView>(R.id.txtAppHashKey).text = appSignature.appSignatures.toString() //SOLO PARA PRUEBAS SE MUESTRA
        startSMSListener()//SE INICIA EL RECEPTOR DE LOS SMS

        findViewById<Button>(R.id.btnVCodigo).setOnClickListener { validarCodigo() }
        findViewById<Button>(R.id.btnReenvCodigo).setOnClickListener { reenviarCodigo() }
        findViewById<EditText>(R.id.cod1).requestFocus()
    }
    //MD FUNCION QUE EJECUTA UNA ACTCION DE ACUERDO ALA TECLA PRECIONADA
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> {
                validarCodigo()
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }
    override fun onBackPressed() {
        //FUNCION QUE SE EJECUTA AL PERSIONAR EL BOTON ATRAS DE MOMENTO NO DEBE DE HACER NADA
        //Toast.makeText(this, "PULSO HACIA ATRAS", Toast.LENGTH_SHORT).show()
        FuncionesGlobales.mostrarAlert(
            this,
            "advertencia",
            true,
            "Registro",
            "Para continuar debe de terminar el proceso.",
            false
        ).show()
    }

    fun textChangueCodigo(){
        findViewById<EditText>(R.id.cod1).doAfterTextChanged {
            val v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.cod2).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.cod1).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.cod2).doAfterTextChanged {
            val v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.cod3).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.cod1).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.cod2).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.cod3).doAfterTextChanged {
            val v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.cod4).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.cod2).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.cod3).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.cod4).doAfterTextChanged {
            val v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<Button>(R.id.btnVCodigo).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.cod3).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.cod4).requestFocus()
                }
            }
        }
    }
    fun validarCodigo(){
        val codigoVal = findViewById<EditText>(R.id.cod1).text.toString() +
                findViewById<EditText>(R.id.cod2).text.toString() +
                findViewById<EditText>(R.id.cod3).text.toString() +
                findViewById<EditText>(R.id.cod4).text.toString()
        val prefs = getDefaultSharedPreferences(this)
        val codigoVerificador = prefs.getString("CODIGO_VERIFICADOR", "0")
        var respuesta = ""
        var continua = false
        if (codigoVal == codigoVerificador){
            respuesta = "El codigo ingresado es correcto, espere un momento"
            continua = true
        }else{
            respuesta = "El codigo ingresado no es correcto, verifique por favor"
        }
        //SI EL CODIGO ES CORRECTO SE CONTINUA
        if(continua){
            val parametros = this.intent.extras
            val idCliente = parametros!!.getString("idCliente", "0")

            val registraNip = Intent(this, NIPActivity::class.java)
            registraNip.putExtra("recuperarNip", recuperarNip)
            registraNip.putExtra("idCliente", idCliente)
            startActivity(registraNip)
        }else{
            Toast.makeText(applicationContext,respuesta,Toast.LENGTH_SHORT).show()
        }
    }

    fun colocarCodigo(codigo: String){
        //MD FUNCION QUE COLOCA EL CODIGO EN LOS INPUTS
        findViewById<TextView>(R.id.cod1).text = codigo.substring(0,1)
        findViewById<TextView>(R.id.cod2).text = codigo.substring(1,2)
        findViewById<TextView>(R.id.cod3).text = codigo.substring(2,3)
        findViewById<TextView>(R.id.cod4).text = codigo.substring(3,4)
        findViewById<TextView>(R.id.cod4).clearFocus()
        val colorExito = ContextCompat.getColor(this, R.color.Verde5)
        findViewById<TextView>(R.id.cod1).background.setTint(colorExito)
        findViewById<TextView>(R.id.cod2).background.setTint(colorExito)
        findViewById<TextView>(R.id.cod3).background.setTint(colorExito)
        findViewById<TextView>(R.id.cod4).background.setTint(colorExito)

        //INMEDIATAMENTE DESPUES DE LEER EL CODIGO SE DEBE DE VALIDAR QUE SEA CORRECTO PARA ABRIR VENTANA DE NIP
        validarCodigo()
    }
    fun enviarCodigo(){
        //SE DEBE DE COMPLETAR CON EL WS PENDIENTE
        //SE GENERA EL CODIGO Y SE ENVIA EN EL SMS
        val codigo = Random.nextInt(1000,9999)
        FuncionesGlobales.guardarVariableSesion(this,"String","CODIGO_VERIFICADOR",codigo.toString())
        //SOLO PARA PRUEBAS
        findViewById<TextView>(R.id.txtCodigoP).text = codigo.toString()
    }
    fun reenviarCodigo(){
        //ENVIA NUEVAMENTE EL CODIGO
        //LIMPIA INPUTS
        findViewById<TextView>(R.id.cod1).text = ""
        findViewById<TextView>(R.id.cod2).text = ""
        findViewById<TextView>(R.id.cod3).text = ""
        findViewById<TextView>(R.id.cod4).text = ""
        findViewById<TextView>(R.id.cod1).requestFocus()

        findViewById<TextView>(R.id.cod1).background.setTint(Color.GRAY)
        findViewById<TextView>(R.id.cod2).background.setTint(Color.GRAY)
        findViewById<TextView>(R.id.cod3).background.setTint(Color.GRAY)
        findViewById<TextView>(R.id.cod4).background.setTint(Color.GRAY)
        //METODO PENDIENTE
        enviarCodigo()
        //INICIA EL METODO QUE ESCUCHA
        startSMSListener()
    }
    /********  MD FUNCIONES PARA EJECUTAR LA ESCUCHA DE SMS   ********/
    /**
     * INICIA SMSRETRIEVER(CLASE RECEPTOR SMS), QUE ESPERA UN MENSAJE SMS COINCIDENTE HASTA QUE SE AGOTE EL TIEMPO DE ESPERA
     * (5 MINUTOS). EL MENSAJE SMS COINCIDENTE SE ENVIARÁ A TRAVÉS DE UN BROADCAST INTENT DE TRANSMISIÓN CON
     * LA ACCION DEL SMS_RETRIEVED_ACTION
     */
    private fun startSMSListener() {
        try {
            smsReceiver = ReceptorSMS()
            smsReceiver!!.initOTPListener(this)

            val intentFilter = IntentFilter()
            intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION)
            this.registerReceiver(smsReceiver, intentFilter)

            val client = SmsRetriever.getClient(this)

            val task = client.startSmsRetriever()
            task.addOnSuccessListener {
                // API successfully started
            }
            task.addOnFailureListener {
                // Fail to start API
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    //MD SI LA LECTURA ES SUCCESS
    override fun onOTPReceived(otp: String) {
        var codigo = ""
        //SE EXTRAE EL CODIGO
        val p: Pattern = Pattern.compile("\\b\\d{4}\\b");
        val m: Matcher = p.matcher(otp)
        while (m.find()) {
            codigo = m.group(0)
        }
        //SE ENVIA A LA FUNCION QUE LO MUESTRA
        colocarCodigo(codigo)


////////////////////////
        if (smsReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(smsReceiver!!)
        }
    }
    //SI LA LECTURA NO SE REALIZA SE MUESTRA LA ALERTA
    override fun onOTPTimeOut() {
        try {
            findViewById<TextView>(R.id.txtAlerta).visibility = View.VISIBLE
        }catch (e: Exception){

        }
    }

}