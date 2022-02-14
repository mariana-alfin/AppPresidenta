package com.example.apppresidenta

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.apppresidenta.utils.GeneralUtils
import com.example.apppresidenta.utils.PermisosUtils
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*


open class UbicacionActivity : AppCompatActivity() {

    private val permisosAValidar = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    private var manejadorUbicacion: LocationManager? = null
    lateinit var clienteUbicacionFusionada: FusedLocationProviderClient

    /*Variables a usar para poder mostrar alert de google (cuando Gps esta apagado) y prenderlo en automatico*/
    private var clienteAjustes: SettingsClient? = null
    private var solicitudConfiguracionUbicacion: LocationSettingsRequest? = null
    private val SOLICITUD_PERMISO_UBICACION = 101

    /*Se necesita declarar e inicializar ciertos valores para poder acceder al alert y ajustes del GPS*/
    companion object {
        val solicitudUbicacion: LocationRequest = LocationRequest.create()
            .apply {
                interval = 0
                fastestInterval = 0
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
    }

    /*Se inicializa un builder para mostrar el alert*/
    init {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(solicitudUbicacion)
        solicitudConfiguracionUbicacion = builder.build()
        builder.setAlwaysShow(true)
    }



    @RequiresApi(Build.VERSION_CODES.M)
    fun solicitarUsoUbicacion(actividad: AppCompatActivity){
        if(PermisosUtils.preguntarPorPermisos(this, permisosAValidar,actividad, GeneralUtils.ASK_FOR_PERMISSION_GPS)){
            clienteAjustes = LocationServices.getSettingsClient(this) //Se le debe asignar un contexto al cliente de ajustes para poder usarlo
            activarGPS()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GeneralUtils.ASK_FOR_PERMISSION_GPS) {
            if (grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                clienteAjustes = LocationServices.getSettingsClient(this) //Se le debe asignar un contexto al cliente de ajustes para poder usarlo
                activarGPS()
            } else {
                GeneralUtils.mostrarAlertActivarPermisos(this, packageName)
            }
        }
    }

    private fun verificarEstatusGPS(): Boolean {
        manejadorUbicacion = getSystemService(LOCATION_SERVICE) as LocationManager

        return (manejadorUbicacion!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || manejadorUbicacion!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    @SuppressLint("MissingPermission")
    fun obtenerUbicacion(){

        clienteUbicacionFusionada = LocationServices.getFusedLocationProviderClient(this)

        clienteUbicacionFusionada.lastLocation.addOnCompleteListener(this) {
            solicitarNuevaUbicacion()
        }
    }

    @SuppressLint("MissingPermission")
    private fun solicitarNuevaUbicacion() {
        val solicitudUbicacion = LocationRequest()
        solicitudUbicacion.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        solicitudUbicacion.interval = 0
        solicitudUbicacion.fastestInterval = 0
        solicitudUbicacion.numUpdates = 1

        clienteUbicacionFusionada = LocationServices.getFusedLocationProviderClient(this)
        clienteUbicacionFusionada.requestLocationUpdates(
            solicitudUbicacion, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val ultimaUbicacion: Location = locationResult.lastLocation
            Toast.makeText(this@UbicacionActivity
                , "Ubicacion actual: Lat: ${ultimaUbicacion.latitude} , Lon: ${ultimaUbicacion.longitude}"
                , Toast.LENGTH_LONG).show()

            //Se guarda en variables de sesion la ubicacion mas actual
            GeneralUtils.registrarVariableSesion(this@UbicacionActivity, "LATITUD", (ultimaUbicacion.latitude).toString())
            GeneralUtils.registrarVariableSesion(this@UbicacionActivity, "LONGITUD", (ultimaUbicacion.longitude).toString())
        }
    }

    private fun activarGPS() {

        if (verificarEstatusGPS()) {
            obtenerUbicacion()
        } else {
            clienteAjustes!!
                .checkLocationSettings(solicitudConfiguracionUbicacion)
                .addOnSuccessListener(this as Activity) {
                    // GPS ya esta habilitado
                    obtenerUbicacion()
                }
                .addOnFailureListener(this) { e ->
                    when ((e as ApiException).statusCode) {

                        /*Si la ubicacion no esta prendida se muestra el alert al usuario*/
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->

                            try {
                                val resolverExcepcion = e as ResolvableApiException
                                resolverExcepcion.startResolutionForResult(
                                    this,
                                    SOLICITUD_PERMISO_UBICACION
                                )
                            } catch (sie: SendIntentException) {
                                Log.i(ContentValues.TAG, "No fue posible ejecutar la solicitud.")
                            }

                        /*Si por algun motivo no se puede activar de forma automatica
                        se envia a los ajustes para que lo haga de forma manual*/
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                             val intent = Intent()
                                intent.action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                    this.startActivity(intent)
                        }
                    }
                }
        }
    }

    /*Funcion que recibe la respuesta del alert mostrado al usuario para activar la ubicacion*/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == SOLICITUD_PERMISO_UBICACION) {
            //El usuario acepta prender del GPS
            obtenerUbicacion() //Se obtiene la ubicacion
        }
        else if(resultCode != Activity.RESULT_OK && requestCode == SOLICITUD_PERMISO_UBICACION){
            //En caso de rechazar el activar la ubicacion se muestra un mensaje y lo saca de la actividad
            GeneralUtils.mostrarAlertActivacionGPS(this,this)
        }
    }
}