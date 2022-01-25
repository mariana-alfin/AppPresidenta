package com.example.apppresidenta

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.apppresidenta.databinding.ActivityNavegacionBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class Navegacion : AppCompatActivity() {

    private lateinit var binding: ActivityNavegacionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNavegacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.fragmNavegacion)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(
                R.id.navHome, R.id.navMiGrupo, R.id.navigation_notifications
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        //AGREGA LA IMAGEN
        //supportActionBar?.setBackgroundDrawable(getResources().getDrawable(R.drawable.bannerprueba))
        supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.drawable.banner4))
        supportActionBar?.setLogo(R.mipmap.icono_app)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
    }
    override fun onBackPressed() {
        //FUNCION QUE SE EJECUTA AL PERSIONAR EL BOTON ATRAS DE MOMENTO NO DEBE DE HACER NADA
        //Toast.makeText(this, "PULSO HACIA ATRAS", Toast.LENGTH_SHORT).show()
    }

   //FUNCIONES DE CADA OPTION
   override fun onOptionsItemSelected(item: MenuItem): Boolean {
       return when (item.itemId) {
           R.id.iCalculadora -> {
               //showOption(item.title)
               //redireccionarOpcion("CALCULADORA")
               startActivity(FuncionesGlobales.redireccionarOpcion(this,"CALCULADORA"))
               true
           }
           R.id.iHistorial -> {
               startActivity(FuncionesGlobales.redireccionarOpcion(this,"MI_HISTORIAL"))
               true
           }
           R.id.iSesion-> {
               startActivity(FuncionesGlobales.cerrarSesion(this))
               true
           }
           else -> super.onOptionsItemSelected(item)
       }
   }
}