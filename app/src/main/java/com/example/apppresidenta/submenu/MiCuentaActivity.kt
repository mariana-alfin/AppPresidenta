package com.example.apppresidenta.submenu

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat.getColor
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import com.example.apppresidenta.R
import com.example.apppresidenta.generales.FuncionesGlobales
import com.example.apppresidenta.generales.FuncionesGlobales.Companion.setMaxLength
import com.example.apppresidenta.generales.FuncionesGlobales.Companion.toDp
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout


class MiCuentaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mi_cuenta_activity)
        //MD SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
        FuncionesGlobales.guardarPestanaSesion(this, "true")
        /*SE AGREGA LOGO Y TITULO DEL LA ACTIVIDAD*/
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = HtmlCompat.fromHtml("<font color='#FFFFFF'>Mi Cuenta</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        //supportActionBar?.title = HtmlCompat.fromHtml("<font color='#FFFFFF'>Bienvenida</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_v6))
        findViewById<ImageButton>(R.id.btnEditarNip).setOnClickListener { ingresarNip(false) }
        datosCliente()

    }
    private fun ingresarNip(esObligatorio: Boolean) {
        val context = this
        val constraintLayout = ConstraintLayout(context)
        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        constraintLayout.layoutParams = layoutParams
        constraintLayout.id = View.generateViewId()
        val textInputLayout = TextInputLayout(context)
        textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        textInputLayout.counterMaxLength = 4
        textInputLayout.isCounterEnabled = true
        textInputLayout.isErrorEnabled = true
        textInputLayout.boxStrokeColor = getColor(this, R.color.Naranja)
        layoutParams.setMargins(
            90.toDp(context),
            20.toDp(context),
            90.toDp(context),
            20.toDp(context)
        )
        textInputLayout.layoutParams = layoutParams
        textInputLayout.hint = "NIP a 4 dígitos."
        textInputLayout.counterMaxLength = 4
        textInputLayout.id = View.generateViewId()
        textInputLayout.tag = "textInputLayoutTag"

        val input = TextInputEditText(context)
        input.id = View.generateViewId()
        input.tag = "textInputEditTextTag"
        input.setMaxLength(4)//maximo largo del mensaje
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.transformationMethod = PasswordTransformationMethod.getInstance()
        input.gravity = Gravity.CENTER
        textInputLayout.addView(input)

        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintLayout.addView(textInputLayout)

        val alert = androidx.appcompat.app.AlertDialog.Builder(this, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
        alert.setTitle("Actualizar NIP")
        alert.setIcon(R.drawable.ic_nip2)
                if(!esObligatorio){
                    alert.setMessage("Para continuar, es necesario que indiques tu NIP actual")
                }else{
                    alert.setMessage(HtmlCompat.fromHtml("<font color='#D63031'>Para continuar, es necesario que indiques tu NIP actual</font>", HtmlCompat.FROM_HTML_MODE_LEGACY))
                    input.error = "El NIP es requerido"
                }
        alert.setView(constraintLayout) // .setView(input)
        alert.setNegativeButton("CANCELAR") { dialog, which ->
                // Respond to negative button press
                dialog.cancel()
            }
        alert.setPositiveButton("ACEPTAR") { dialog, which ->
                // Respond to positive button press
                if(input.text.toString().isNotEmpty()) {
                     validaNip(input.text.toString())
                }else{
                     ingresarNip(true)
                    }

                }
            alert.show()
    }

    private fun validaNip(nipCaptura: String) {
        //PARA PRUEBAS SE VALIDA CONTRA UN NIP X PARA CONTINUAR
        val NIP_ANTERIOR = "5253"
        //Toast.makeText(this, "NIP :$nipCaptura", Toast.LENGTH_SHORT).show()
        if(nipCaptura == NIP_ANTERIOR){
            //MD INICIA OTRA ACTIVIDAD
            actualizarNip()
        }else{
            //MD NO PERMITE CONTINUAR
          val alertError =  FuncionesGlobales.mostrarAlert(
                this,
                "error",
                true,
                "Actualizar NIP",
                "El nip ingresado es incorrecto. Favor de intentarlo nuevamente.",
                true
            )
            alertError.setPositiveButton("Aceptar"){ dialog, which ->
                ingresarNip(true)
            }
            alertError.setNegativeButton("Cancelar") { dialog, which ->
                // Respond to negative button press
                dialog.cancel()
            }
            alertError.show()
        }
    }

    private fun actualizarNip() {
        val rn = Intent(this, RecuperarNIPActivity::class.java)
        startActivity(rn)
        finish()
    }

    private fun datosCliente() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val idPrestamo = prefs.getInt("CREDITO_ID", 0)
        val idCliente = prefs.getString("ID_PRESIDENTA", "--")
        val presidenta = prefs.getString("PRESIDENTA","SIN NOMBRE")
        val tarjeta = prefs.getString("TARGETA","SIN TARGETA")

        findViewById<TextView>(R.id.txtPresidenta).text = presidenta
        findViewById<TextView>(R.id.idCredito).text = idPrestamo.toString()
        findViewById<TextView>(R.id.idCliente).text = idCliente
        //findViewById<TextView>(R.id.tarjetaCliente).text = tarjeta
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        finish()
        return false
    }

}


