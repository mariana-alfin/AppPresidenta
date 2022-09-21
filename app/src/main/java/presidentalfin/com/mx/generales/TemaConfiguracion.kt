package presidentalfin.com.mx.generales

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import presidentalfin.com.mx.R


object TemaConfiguracion {

    fun aplicarTema(modo: Mode?) {
        when (modo) {
            Mode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun aplicarTema(context: Context) {
        val defaultSharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val value = defaultSharedPreferences.getString(
            context.getString(R.string.configuracion_tema_llave),
            Mode.DEFAULT.name
        )
        aplicarTema(value?.let { Mode.valueOf(it) })
    }

    enum class Mode {
        DEFAULT, DARK, LIGHT
    }
}