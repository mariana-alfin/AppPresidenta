package com.example.apppresidenta

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.apppresidenta.utils.GeneralUtils
import com.example.apppresidenta.utils.PermisosUtils
import com.google.android.gms.location.*

open class UbicacionActivity : AppCompatActivity() {

    private val permisosAValidar = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    protected var manejadorUbicacion: LocationManager? = null
    lateinit var clienteUbicacionFusionada: FusedLocationProviderClient

    @RequiresApi(Build.VERSION_CODES.M)
    fun solicitarUsoUbicacion(actividad: AppCompatActivity){
        if(PermisosUtils.preguntarPorPermisos(this, permisosAValidar,actividad, GeneralUtils.ASK_FOR_PERMISSION_GPS)){
            verificarEstatusGPS()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GeneralUtils.ASK_FOR_PERMISSION_GPS) {
            if (grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                verificarEstatusGPS()
            } else {
                GeneralUtils.mostrarAlertActivarPermisos(this, packageName)
            }
        }
    }

    private fun verificarEstatusGPS(){
        manejadorUbicacion = getSystemService(LOCATION_SERVICE) as LocationManager

        if(manejadorUbicacion!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || manejadorUbicacion!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            obtenerUbicacion()
        }else{
            GeneralUtils.mostrarAlertActivacionGPS(this)
        }
    }

    @SuppressLint("MissingPermission")
    fun obtenerUbicacion(){

        clienteUbicacionFusionada = LocationServices.getFusedLocationProviderClient(this)

        /*setContentView(R.layout.activity_gps)
        val msj = findViewById<View>(R.id.mensaje) as TextView*/

        manejadorUbicacion = getSystemService(LOCATION_SERVICE) as LocationManager

        val gpsEnabled: Boolean = manejadorUbicacion!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || manejadorUbicacion!!.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)

        if (gpsEnabled) {

            clienteUbicacionFusionada.lastLocation.addOnCompleteListener(this) {
                //msj.text = "Obteniedo ubicacion..."
                solicitarNuevaUbicacion()
            }
        } else {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    @SuppressLint("MissingPermission")
    private fun solicitarNuevaUbicacion() {
        var solicitudUbicacion = LocationRequest()
        solicitudUbicacion.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        solicitudUbicacion.interval = 0
        solicitudUbicacion.fastestInterval = 0
        solicitudUbicacion.numUpdates = 1

        clienteUbicacionFusionada = LocationServices.getFusedLocationProviderClient(this)
        clienteUbicacionFusionada!!.requestLocationUpdates(
            solicitudUbicacion, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var ultimaUbicacion: Location = locationResult.lastLocation
            Toast.makeText(this@UbicacionActivity
                , "Ubicacion actual: Lat: ${ultimaUbicacion.latitude} , Lon: ${ultimaUbicacion.longitude}"
                , Toast.LENGTH_SHORT).show()
            /*findViewById<TextView>(R.id.mensaje).text = "Ubicacion actual: "
            findViewById<TextView>(R.id.lat).text = ultimaUbicacion.latitude.toString()
            findViewById<TextView>(R.id.lon).text = ultimaUbicacion.longitude.toString()*/
        }
    }
}