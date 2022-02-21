package com.example.apppresidenta.submenu

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import com.example.apppresidenta.R

class MiCuentaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mi_cuenta_activity)
        /*SE AGREGA LOGO Y TITULO DEL LA ACTIVIDAD*/
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = HtmlCompat.fromHtml("<font color='#FFFFFF'>Mi Cuenta</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        //supportActionBar?.title = HtmlCompat.fromHtml("<font color='#FFFFFF'>Bienvenida</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_v6))

        datosCliente()
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
        return false
    }

}
