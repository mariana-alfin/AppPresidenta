package com.example.apppresidenta.generales

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.InputFilter
import android.util.TypedValue
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.apppresidenta.LoginActivity
import com.example.apppresidenta.R
import com.example.apppresidenta.submenu.BonificacionesActivity
import com.example.apppresidenta.submenu.CalculadoraActivity
import com.example.apppresidenta.submenu.HistorialActivity
import com.example.apppresidenta.submenu.MiCuentaActivity
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/* MD SE GENERA ARCHIVO CON FUNCIONES GLOBALES PARA MANDAR ALLAR EN TODO EL PROYECTO*/
class FuncionesGlobales {
    companion object {
        fun guardarPestanaSesion(activity: AppCompatActivity, pestañaActiva: String) {
            //SE GUARDA EN SESSION EN QUE PESTAÑA SE QUEDO
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            val editor = prefs.edit()
            if (pestañaActiva != "MainActivity") {
                editor.putBoolean("SESION_ACTIVA", true)
            } else {
                editor.putBoolean("SESION_ACTIVA", false)
            }
            editor.apply()
            //Toast.makeText(activity, "SE GUARDA EN SESION: $pestañaActiva", Toast.LENGTH_SHORT).show()
        }

        fun eliminaSesion(activity: AppCompatActivity) {
            //SE ELIMINAN TODAS LAS KEYS GUARDADAS AL MOMENTO
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            val editor = prefs.edit()
            editor.remove("CREDITO_ID")
            editor.remove("PRESIDENTA")
            editor.remove("NOMBRE_GPO")
            editor.remove("ID_PRESIDENTA")
            editor.remove("SESION_ACTIVA")
            editor.remove("MONTO_SEMANAL")
            editor.remove("FECHA_PAGO_CONCILIACION")
            //DEL SMS
            editor.remove("CODIGO_VERIFICADOR")
            editor.apply()
        }
        fun cerrarSesion(activity: AppCompatActivity): Intent {
            //SE ELIMINAN TODAS LAS KEYS GUARDADAS AL MOMENTO
            eliminaSesion(activity)
            //REDIRECCIONAMOS AL INICIO
            //val inicio = Intent(activity, MainActivity::class.java)
            val inicio = Intent(activity, LoginActivity::class.java)
            return inicio
        }
        fun guardarVariableSesion(context: Context, tipoVal: String, variable: String, valorVariable: String) {
            //SE GUARDA EN SESION LA VARIABLE Y EL VALOR RECIVIDOS
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
           if(tipoVal == "Float"){
               editor.putFloat(variable,valorVariable.toFloat())
           }else if(tipoVal == "Int"){
               editor.putInt(variable,valorVariable.toInt())
           }else if(tipoVal == "String"){
               editor.putString(variable,valorVariable)
           }
            //editor.putFloat("MONTO_SEMANAL",jsonResults.getDouble("pay").toFloat())
            editor.apply()
        }

        fun redireccionarOpcion(activity: AppCompatActivity, option: String): Intent {
            val activity: Intent = when (option) {
                "CALCULADORA" -> {
                    Intent(activity, CalculadoraActivity::class.java)
                }
                "MIS_BONIFICACIONES" -> {
                    Intent(activity, BonificacionesActivity::class.java)
                }
                "MI_HISTORIAL" -> {
                    Intent(activity, HistorialActivity::class.java)
                }
                "MI_CUENTA" -> {
                    Intent(activity, MiCuentaActivity::class.java)
                }
                else -> {
                    Intent(activity, HistorialActivity::class.java)
                }
            }
            return activity
        }

        //OBTIENE LA FECHA DEL DIA EN CURSO
        @SuppressLint("SimpleDateFormat")
        fun obtenerFecha(formato: String): String {
            val fecha = Calendar.getInstance().time
            val dateFormat = SimpleDateFormat(formato)
            return dateFormat.format(fecha)
        }

        //FUNCION PARA CONVERTIR UNA FECHA AL FORMATO INDICADO
        @SuppressLint("SimpleDateFormat")
        fun convertFecha(fecha: String, formato: String): String {
            val formatoEntrada: DateFormat = SimpleDateFormat("yyyy-MM-dd")
            val formatoSalida: DateFormat =
                SimpleDateFormat(formato)//SimpleDateFormat("dd/MM/yyyy")
            val fechaDate: Date = formatoEntrada.parse(fecha)
            return formatoSalida.format(fechaDate)
        }
        //LIMITA EL MAX NUM CARACTERES DE UN EDITEXT
        fun EditText.setMaxLength(maxLength: Int) {
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
        }
        fun Int.toDp(context: Context): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics
        ).toInt()
        //SE GENERA UNA FUNCION PARA MOSTRAR ALERT'S PERSONALIZADOS
        fun mostrarAlert(activity: Activity, tipo: String,esPersonalizado: Boolean, titulo: String, mensaje: String, ejecutaAccion: Boolean): AlertDialog.Builder {
            val dialog = AlertDialog.Builder(activity, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
            dialog.setTitle(titulo)
            if(mensaje != "na"){
                dialog.setMessage(mensaje)
            }
            when (tipo) { //DE ACUERDO AL TIPO DE ALERT SE AGREEGA EL ICONO A MOSTAR
                "advertencia" -> {
                    dialog.setIcon(R.drawable.ic_warning)
                }
                "error" -> {
                    dialog.setIcon(R.drawable.ic_error)
                }
                "cuestion" -> {
                    dialog.setIcon(R.drawable.ic_alert)
                }
                "correcto" -> {
                    dialog.setIcon(R.drawable.ic_correcto)
                }
            }
            if(!ejecutaAccion){//SI NO SE VA A EJECUTAR ALGUNA ACCION SE AGREGA QU EL BOTON CANCELE EL ALERT DE LO CONTRARO EN SU IMPLEMENTACION SE AGREGAN LAS FUNCIONES
                dialog.setPositiveButton("Aceptar") { dialog, which ->
                    dialog.cancel()
                }
            }
            return dialog
        }
        fun convertPesos(monto: Double, numDecimales: Int): String {
            /** MD SE GENERA FUNCION PARA DAR FORMATO DE PESOS
             * RECIVE EL MONTO Y EL NUMERO DE DECIMALES A MOSTRAR
             * RETRORNA EL VALOR YA FORMATEADO
             */
            val mx = Locale("es", "MX")
            val formatPesos: NumberFormat = NumberFormat.getCurrencyInstance(mx)
            formatPesos.maximumFractionDigits = numDecimales //MD ES PARA QUE NO TENGA DECIMALES
            return formatPesos.format(monto)
        }

    }
}
