package com.example.apppresidenta.submenu

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import com.example.apppresidenta.R
import com.example.apppresidenta.generales.FuncionesGlobales

class RecuperarNIPActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recuperar_nip_activity)
        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "true")
        /*SE AGREGA LOGO Y TITULO DEL LA ACTIVIDAD*/
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = HtmlCompat.fromHtml("<font color='#FFFFFF'>Actualizar NIP</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_v6))
        textChangueNip()
        findViewById<Button>(R.id.btnConfirmarNip).setOnClickListener { confirmarNIP() }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return false
    }
    fun confirmarNIP(){
        //OBTIENE EL NIP
        val nip = findViewById<EditText>(R.id.nip1).text.toString() +
                findViewById<EditText>(R.id.nip3).text.toString() +
                findViewById<EditText>(R.id.nip4).text.toString() +
                findViewById<EditText>(R.id.nip2).text.toString()
        //OBTIENE EL NIP DE CONFIRMACION
        val nipC = findViewById<EditText>(R.id.nipC1).text.toString() +
                findViewById<EditText>(R.id.nipC2).text.toString() +
                findViewById<EditText>(R.id.nipC3).text.toString() +
                findViewById<EditText>(R.id.nipC4).text.toString()

        if (nip.length == 4 && nipC.length == 4) {
            //SON IGUALES
            if (nip == nipC) {
                actualizarNip(nip)
            } else {
                findViewById<EditText>(R.id.nip1).setText("")
                findViewById<EditText>(R.id.nip3).setText("")
                findViewById<EditText>(R.id.nip4).setText("")
                findViewById<EditText>(R.id.nip2).setText("")
                findViewById<EditText>(R.id.nipC1).setText("")
                findViewById<EditText>(R.id.nipC3).setText("")
                findViewById<EditText>(R.id.nipC4).setText("")
                findViewById<EditText>(R.id.nipC2).setText("")
                findViewById<EditText>(R.id.nip1).requestFocus()
                FuncionesGlobales.mostrarAlert(
                    this,
                    "advertencia",
                    true,
                    "Actualización NIP",
                    "El NIP y la Confirmación deben de ser iguales.",
                    false
                ).show()
            }
        } else {
            FuncionesGlobales.mostrarAlert(
                this,
                "error",
                true,
                "Actualización NIP",
                "El NIP debe de ser a 4 dígitos.",
                false
            ).show()
        }
    }
    private fun actualizarNip(nip: String) {
        //SE ENVIAN DATO Y SI ES CORRECTO ENVIA A LOGIN
        var alertContinuar = FuncionesGlobales.mostrarAlert(this,"correcto",true,"Actualizar NIP","Actualización exitosa.",true)
        alertContinuar.setPositiveButton("Aceptar") { dialog, which ->
            //MD SE DEBE DE HACER LA PETICION AL WS
            finish()
        }
        alertContinuar.show()
    }
    //MD ONCHANGUE DE INPUTS DE NIP PARA QUE AL PONNER UN DIGITO SE PASE AL SIGUIENTE INPUT
    fun textChangueNip() {
        findViewById<EditText>(R.id.nip1).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nip2).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nip1).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nip2).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nip3).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nip1).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nip2).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nip3).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nip4).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nip2).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nip3).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nip4).doAfterTextChanged {

            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nipC1).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nip3).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nip4).requestFocus()
                }
            }
        }

        //INPUTS DE LA CONFIRMACION
        findViewById<EditText>(R.id.nipC1).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nipC2).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nipC1).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nipC2).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nipC3).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nipC1).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nipC2).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nipC3).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<EditText>(R.id.nipC4).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nipC2).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nipC3).requestFocus()
                }
            }
        }
        findViewById<EditText>(R.id.nipC4).doAfterTextChanged {
            var v = it.toString()
            when (v.length) {
                1 -> {
                    findViewById<Button>(R.id.btnConfirmarNip).requestFocus()
                }
                0 -> {
                    findViewById<EditText>(R.id.nipC3).requestFocus()
                }
                else -> {
                    findViewById<EditText>(R.id.nipC4).requestFocus()
                }
            }
        }
    }
    //MD FUNCION QUE EJECUTA UNA ACTCION DE ACUERDO ALA TECLA PRECIONADA
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> {
                confirmarNIP()
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }
}