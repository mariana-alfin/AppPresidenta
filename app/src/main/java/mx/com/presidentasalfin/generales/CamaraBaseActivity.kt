package mx.com.presidentasalfin.generales

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import mx.com.presidentasalfin.generales.FuncionesGlobales.Companion.ASK_FOR_PERMISSION_CAMERA
import mx.com.presidentasalfin.generales.FuncionesGlobales.Companion.PHOTO_CODE
import mx.com.presidentasalfin.generales.FuncionesGlobales.Companion.asignarNombreFoto
import mx.com.presidentasalfin.generales.FuncionesGlobales.Companion.guardarVariableSesion
import mx.com.presidentasalfin.generales.FuncionesGlobales.Companion.mostrarAlert
import mx.com.presidentasalfin.generales.ValGlobales.Companion.preguntarPorPermisos
import java.io.File
import mx.com.presidentasalfin.R

open class CameraBaseActivity : UbicacionActivity() {

    private var archivoNuevo: File? = null

    private val permisosAValidar =
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)

    @RequiresApi(Build.VERSION_CODES.M)
    fun solicitarUsarCamara(actividad: AppCompatActivity) {
        if (preguntarPorPermisos(
                this,
                permisosAValidar,
                actividad,
                ASK_FOR_PERMISSION_CAMERA
            )
        ) {
            abrirCamara(this)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ASK_FOR_PERMISSION_CAMERA) {
            if (grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                abrirCamara(this)
            } else {
                //mostrarAlertActivarPermisos(this, packageName)
                val alert = mostrarAlert(this,
                    "error",
                    false,
                    getString(R.string.permisos_denegados),
                    getString(R.string.mensaje_permisos_denegados),
                    true)
                alert.setPositiveButton(android.R.string.ok) { _, _ ->
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    this.startActivity(intent)
                }
                alert.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                alert.show()
            }
        }
    }

    private fun abrirCamara(contexto: Context) {

        var nombreImagen = asignarNombreFoto(contexto)
        nombreImagen = "$nombreImagen${contexto.getString(R.string.extension_foto)}"

        val directorio = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        archivoNuevo = File(directorio, nombreImagen)
        archivoNuevo!!.createNewFile()

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(
            MediaStore.EXTRA_OUTPUT,
            FileProvider.getUriForFile(this, packageName, archivoNuevo!!)
        )

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        //if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, PHOTO_CODE)
        //}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PHOTO_CODE) {
            try {
                Toast.makeText(this, "Evidencia Guardada", Toast.LENGTH_LONG).show()
//             registrarVariableSesion(this,"RUTA_FOTO",archivoNuevo!!.absolutePath)
                guardarVariableSesion(
                    this, "String",
                    "RUTA_FOTO", archivoNuevo!!.absolutePath)
            } catch (e: Exception) {
                mostrarAlert(this, "error", true, "Excepcion ", e.message.toString(), false).show()
            } catch (e: java.lang.Exception) {
                mostrarAlert(this,
                    "error",
                    true,
                    "Excepcion 1 ",
                    e.message.toString(),
                    false).show()
            } catch (e: RuntimeException) {
                mostrarAlert(this,
                    "error",
                    true,
                    "Excepcion 2 ",
                    e.message.toString(),
                    false).show()
            }
        }
    }
}