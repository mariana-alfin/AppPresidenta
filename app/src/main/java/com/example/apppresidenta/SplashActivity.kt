package com.example.apppresidenta

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import com.example.apppresidenta.navegacion.Navegacion

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        validarSesion()
    }


    private fun validarSesion() {
        //SE VALIDA SI LA VARIABLE DE SESION EXISTE DE LO CONTRARIO SE ENVIA A LOGIN
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val sesionActiva = prefs.getBoolean("SESION_ACTIVA", false)

        val intent: Intent = if(sesionActiva){
            Intent(this, Navegacion::class.java)
        }else{
            //Intent(this, MainActivity::class.java)
            Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}