package mx.com.presidentasalfin.generales

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import mx.com.presidentasalfin.R
import mx.com.presidentasalfin.databinding.ActivityCamaraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CamaraActivity : AppCompatActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private val REQUEST_CODE_PERMISSIONS = 10

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    private var FILENAME: String = ""
    lateinit var binding: ActivityCamaraBinding

    private var preview: Preview? = null

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var cameraProvider: ProcessCameraProvider? = null

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File

    private lateinit var cameraExecutor: ExecutorService

    var rutaimagen = "";
    var orientacion_pantalla = 0;
    var photoFile: File? = null;
    /*************/

    /*************/
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityCamaraBinding.inflate(layoutInflater)
            setContentView(binding.root)
            cameraExecutor = Executors.newSingleThreadExecutor()
            outputDirectory = getOutputDirectory()
            supportActionBar?.hide()
            /************SI EXISTE EL ARCHIVO, SE ELIMINA************/
           /* FILENAME = getString(R.string.app_name)
            val photoFile = File(outputDirectory, "$FILENAME.jpg")
            if (photoFile.exists()) {
                photoFile.deleteOnExit()
            }*/
            /************************/
            binding.imgCheck.setOnClickListener { aceptarFoto() }
            binding.imgNCheck.setOnClickListener { noCheck() }

            binding.cameraCaptureButton.setOnClickListener { takePhoto() }
            binding.cameraSwitchButton.setOnClickListener {
                lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                bindCamera()
            }
            if (allPermissionsGranted()) startCamera()
            else ActivityCompat.requestPermissions(this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS)



        } catch (e: Exception) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(e.message.toString())
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            // Create the AlertDialog
            val alertDialog: AlertDialog = builder.create()
            // Set other dialog properties
            alertDialog.show()
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "wildRunning").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun bindCamera() {
        try {
            val metrics = DisplayMetrics().also { binding.viewFinder.display.getRealMetrics(it) }
            val screenAspectRatio = aspectRadio(metrics.widthPixels, metrics.heightPixels)
            val rotation = binding.viewFinder.display.rotation

            val cameraProvider =
                cameraProvider ?: throw IllegalStateException("Fallo al iniciar la camara")
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)

        } catch (exc: java.lang.Exception) {
            Log.e("CameraWildRunning", "Fallo al vincular la camara", exc)
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(exc.message.toString())
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            // Create the AlertDialog
            val alertDialog: AlertDialog = builder.create()
            // Set other dialog properties
            alertDialog.show()
        }
    }

    private fun aspectRadio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)

        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startCamera() {
        val cameraProviderFinnaly = ProcessCameraProvider.getInstance(this)
        cameraProviderFinnaly.addListener(Runnable {

            cameraProvider = cameraProviderFinnaly.get()

            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("No tenemos camara")
            }

            manageSwitchButton()

            bindCamera()

        }, ContextCompat.getMainExecutor(this))

    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    private fun manageSwitchButton() {
        val switchButton = binding.cameraSwitchButton
        try {
            switchButton.isEnabled = hasBackCamera() && hasFrontCamera()

        } catch (exc: CameraInfoUnavailableException) {
            switchButton.isEnabled = false
        }

    }

    private fun takePhoto() {
        val sdf = SimpleDateFormat("hh:mm:ss")
        val currentDate = sdf.format(Date())
        FILENAME = getString(R.string.app_name) + currentDate
        FILENAME = FILENAME.replace(":", "")
        FILENAME = FILENAME.replace("/", "")
        //photoFile = File(outputDirectory, "$FILENAME.jpg")

        var nombreImagen = FuncionesGlobales.asignarNombreFoto(this)
        nombreImagen = "$nombreImagen${this.getString(R.string.extension_foto)}"

        photoFile = File(outputDirectory, nombreImagen)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile!!).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    rutaimagen = photoFile!!.absolutePath
                    verImagen(photoFile!!.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    var clMain = findViewById<ConstraintLayout>(R.id.clMain)
                    Snackbar.make(clMain, "Error al guardar la imagen", Snackbar.LENGTH_LONG)
                        .setAction("OK") {
                            clMain.setBackgroundColor(Color.CYAN)
                        }.show()
                }
            })
    }

    fun guardarImagen() {
        val savedUri = Uri.fromFile(photoFile)

        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(savedUri.toFile().extension)
        MediaScannerConnection.scanFile(
            baseContext,
            arrayOf(savedUri.toFile().absolutePath),
            arrayOf(mimeType)
        ) { _, uri ->
        }
        FuncionesGlobales.guardarVariableSesion(
            this, "String",
            "RUTA_FOTO", savedUri.toFile().absolutePath)
    }

    private fun verImagen(PathImagen: String) {
        try {
            if (PathImagen != "") {
                if (!binding.llimg.isVisible) {
                    val archivo = File(PathImagen)
                    //VERIFICA QUE EXISTA.
                    if (archivo.exists()) {
                        //SE MUESTRA LA FOTOGRAFIA
                        val photoBitmap: Bitmap = BitmapFactory.decodeFile(PathImagen)
                        binding.imgPrevia.setImageBitmap(photoBitmap)
                        binding.llimg.visibility = View.VISIBLE
                        binding.viewFinder.visibility = View.GONE
                        binding.cameraCaptureButton.visibility = View.GONE
                        binding.cameraSwitchButton.visibility = View.GONE
                    }
                } else {
                    binding.llimg.visibility = View.GONE
                    binding.viewFinder.visibility = View.VISIBLE
                    binding.cameraCaptureButton.visibility = View.VISIBLE
                    binding.cameraSwitchButton.visibility = View.VISIBLE
                }
            } else {
                binding.llimg.visibility = View.GONE
                binding.viewFinder.visibility = View.VISIBLE
                binding.cameraCaptureButton.visibility = View.VISIBLE
                binding.cameraSwitchButton.visibility = View.VISIBLE
            }
        } catch (e: Exception) {

        }
    }

    fun noCheck() {
        //no se acepto la imagen por tanto se elimina y se mueestra nuevamente la camara
        if (rutaimagen != "") {
            val archivo = File(rutaimagen)
            if (archivo.exists()) {
                archivo.delete()
                FuncionesGlobales.eliminaVariableSesion(this, "RUTA_FOTO")
            }
        }
        binding.llimg.visibility = View.GONE
        binding.viewFinder.visibility = View.VISIBLE
        binding.cameraCaptureButton.visibility = View.VISIBLE
        binding.cameraSwitchButton.visibility = View.VISIBLE
    }

    fun aceptarFoto() {
        /*   var clMain = findViewById<ConstraintLayout>(R.id.clMain)
           Snackbar.make(clMain, "Imagen guardada con éxito", Snackbar.LENGTH_LONG)
               .setAction("OK") { this.finish()
               }.show()
        */
        guardarImagen()
        val clMain = findViewById<ConstraintLayout>(R.id.clMain)
        Snackbar.make(clMain, "Imagen guardada con éxito", Snackbar.LENGTH_LONG).show()
        this.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}