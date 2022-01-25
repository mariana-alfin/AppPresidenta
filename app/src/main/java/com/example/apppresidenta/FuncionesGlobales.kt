package com.example.apppresidenta

import android.content.Intent
import android.text.InputFilter
import android.widget.EditText
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

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
            editor.remove("MONTO_SEMANAL")
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
        //obtiene la fecha del dia en curso
        fun obtenerFecha(formato: String): String {
            val fecha = Calendar.getInstance().time
            val dateFormat = SimpleDateFormat(formato)
            return dateFormat.format(fecha)
        }
        //funcion para convertir una fecha al formato indicado
        fun convertFecha(fecha: String, formato: String): String{
            val formatoEntrada: DateFormat = SimpleDateFormat("yyyy-MM-dd")
            val formatoSalida: DateFormat = SimpleDateFormat(formato)//SimpleDateFormat("dd/MM/yyyy")
            val fechaDate: Date = formatoEntrada.parse(fecha)
            return formatoSalida.format(fechaDate)
        }   //limita el max num caracteres de un editext
        fun EditText.setMaxLength(maxLength: Int) {
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
        }
    }
}