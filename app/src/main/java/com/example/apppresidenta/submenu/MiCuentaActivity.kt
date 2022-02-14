package com.example.apppresidenta.submenu

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.text.HtmlCompat
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
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return false
    }
}
