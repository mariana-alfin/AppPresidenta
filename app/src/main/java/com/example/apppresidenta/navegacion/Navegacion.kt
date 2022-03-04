package com.example.apppresidenta.navegacion

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.apppresidenta.R
import com.example.apppresidenta.databinding.NavegacionActivityBinding
import com.example.apppresidenta.generales.FuncionesGlobales
import com.google.android.material.bottomnavigation.BottomNavigationView

class Navegacion : AppCompatActivity() {

    private lateinit var binding: NavegacionActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = NavegacionActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.fragmNavegacion)

        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(
                R.id.navHome, R.id.navMiGrupo, R.id.navigation_notifications
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        //MD AGREGA LA IMAGEN A LA BARRA
        //supportActionBar?.setBackgroundDrawable(getResources().getDrawable(R.drawable.bannerprueba))
        //supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.drawable.banner4))
        supportActionBar?.setBackgroundDrawable(getDrawable(R.drawable.barra_a800))

        //MD SE AGREGA ICONO Y BOTONES A LA BARRA
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
    }
    override fun onBackPressed() {
        //FUNCION QUE SE EJECUTA AL PERSIONAR EL BOTON ATRAS DE MOMENTO NO DEBE DE HACER NADA
        //Toast.makeText(this, "PULSO HACIA ATRAS", Toast.LENGTH_SHORT).show()
    }

    //MD FUNCIONES DE CADA OPTION
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.iCuenta -> {
                //showOption(item.title)
                //redireccionarOpcion("CALCULADORA")
                startActivity(FuncionesGlobales.redireccionarOpcion(this, "MI_CUENTA"))
                true
            }
          /*  R.id.iCalculadora -> {
                startActivity(FuncionesGlobales.redireccionarOpcion(this, "CALCULADORA"))
                true
            }*/
            R.id.iBonificaciones -> {
                startActivity(FuncionesGlobales.redireccionarOpcion(this, "MIS_BONIFICACIONES"))
                true
            }
            R.id.iHistorial -> {
                startActivity(FuncionesGlobales.redireccionarOpcion(this, "MI_HISTORIAL"))
                true
            }
            R.id.iSesion -> {
                startActivity(FuncionesGlobales.cerrarSesion(this))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}