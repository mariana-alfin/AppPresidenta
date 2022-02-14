package com.example.apppresidenta

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.apppresidenta.utils.GeneralUtils
import com.example.apppresidenta.utils.GeneralUtils.Companion.mostrarAlertActivarPermisos
import com.example.apppresidenta.utils.GeneralUtils.Companion.registrarVariableSesion
import com.example.apppresidenta.utils.PermisosUtils
import java.io.File

open class CameraBaseActivity : UbicacionActivity() {

    private var archivoNuevo: File? = null

    private val permisosAValidar =
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)

    @RequiresApi(Build.VERSION_CODES.M)
    fun solicitarUsarCamara(actividad: AppCompatActivity) {
        if (PermisosUtils.preguntarPorPermisos(
                this,
                permisosAValidar,
                actividad,
                GeneralUtils.ASK_FOR_PERMISSION_CAMERA
            )
        ) {
            abrirCamara(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GeneralUtils.ASK_FOR_PERMISSION_CAMERA) {
            if (grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                abrirCamara(this)
            } else {
                mostrarAlertActivarPermisos(this, packageName)
            }
        }
    }

    private fun abrirCamara(contexto: Context) {

        var nombreImagen = GeneralUtils.asignarNombreFoto(contexto)
        nombreImagen = "$nombreImagen${contexto.getString(R.string.extension_foto)}"

        val directorio = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        archivoNuevo = File(directorio,nombreImagen)
        archivoNuevo!!.createNewFile()

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(
            MediaStore.EXTRA_OUTPUT,
            FileProvider.getUriForFile(this, packageName, archivoNuevo!!)
        )

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, GeneralUtils.PHOTO_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
         if (resultCode == Activity.RESULT_OK && requestCode == GeneralUtils.PHOTO_CODE){
            Toast.makeText(this, "Evidencia Guardada", Toast.LENGTH_LONG).show()
             registrarVariableSesion(this,"RUTA_FOTO",archivoNuevo!!.absolutePath)
        }
    }
}