package com.example.apppresidenta

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class InicioApp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_inicio_app)
        validarSesion()
    }
    private fun validarSesion() {
        //SE VALIDA SI LA VARIABLE DE SESION EXISTE DE LO CONTRARIO SE ENVIA A LOGIN
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val sesionActiva = prefs.getBoolean("SESION_ACTIVA", false)
        if(sesionActiva){
              val inicio = Intent(this, Navegacion::class.java)
              startActivity(inicio)
          }else{
              val home = Intent(this, MainActivity::class.java)
              startActivity(home)
          }


    }
}
