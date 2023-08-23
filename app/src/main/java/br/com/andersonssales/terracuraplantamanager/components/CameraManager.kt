package br.com.andersonssales.terracuraplantamanager.components

import android.content.Context
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService

class CameraManager(
    private val cameraExecutor: ExecutorService,
    private val context: Context,
    private val previewView: PreviewView
   ) {
    private var cameraProvider: ProcessCameraProvider? = null

    fun startCamera() {
        Log.d("CameraManager", "Start camera initialization")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))

    }

    private fun bindCameraUseCases() {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
//        val binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)


        val rotation = previewView.display?.rotation ?: 0 // Default rotation value
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(rotation)
            .build()



        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .build()

        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(
            context as LifecycleOwner,
            cameraSelector,
            useCaseGroup
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}