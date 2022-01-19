package com.example.apppresidenta

import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity

class FuncionesGlobales {
    companion object {
        fun guardarPestanaSesion(activity: AppCompatActivity, pestañaActiva: String){
            //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            val editor = prefs.edit()
            if(pestañaActiva != "MainActivity"){
                editor.putBoolean("SESION_ACTIVA",true)
            }else{
                editor.putBoolean("SESION_ACTIVA",false)
            }
            editor.apply()
            //Toast.makeText(activity, "SE GUARDA EN SESION: $pestañaActiva", Toast.LENGTH_SHORT).show()
        }
        fun cerrarSesion(activity: AppCompatActivity): Intent{
            //SE ELIMINAN TODAS LAS KEYS GUARDADAS AL MOMENTO
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            val editor = prefs.edit()
            editor.remove("CREDITO_ID")
            editor.remove("PRESIDENTA")
            editor.remove("SESION_ACTIVA")
            editor.apply()
            //REDIRECCIONAMOS AL INICIO
            val inicio = Intent(activity, MainActivity::class.java)
            return inicio
        }
        fun redireccionarOpcion(activity: AppCompatActivity,option: String): Intent {
            val activity: Intent = when (option) {
                "CALCULADORA" -> {
                    Intent(activity, CalculadoraActivity::class.java)
                }
                "MI_HISTORIAL" -> {
                    Intent(activity, HistorialActivity::class.java)
                }
                else -> {
                    Intent(activity, MainActivity::class.java)
                }
            }
            return activity
        }
    }
}