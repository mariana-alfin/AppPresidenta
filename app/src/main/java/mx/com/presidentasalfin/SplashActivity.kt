package mx.com.presidentasalfin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import mx.com.presidentasalfin.generales.FuncionesGlobales.Companion.obtenerTokenNotificaciones
import mx.com.presidentasalfin.generales.TemaConfiguracion
import mx.com.presidentasalfin.navegacion.Navegacion

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaConfiguracion.aplicarTema(this)
        obtenerTokenNotificaciones(this)
        val packagename = this.applicationContext.packageName;
      //  Toast.makeText(this,"nombrePaquete " +packagename,Toast.LENGTH_SHORT).show()
        validarSesion()
    }


    private fun validarSesion() {
        //SE VALIDA SI LA VARIABLE DE SESION EXISTE DE LO CONTRARIO SE ENVIA A LOGIN
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val sesionActiva = prefs.getBoolean("SESION_ACTIVA", false)

        val intent: Intent = if (sesionActiva) {
            Intent(this, Navegacion::class.java)
            //Intent(this, BonificacionesActivity::class.java)
        } else {
            //Intent(this, MainActivity::class.java)
            Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}