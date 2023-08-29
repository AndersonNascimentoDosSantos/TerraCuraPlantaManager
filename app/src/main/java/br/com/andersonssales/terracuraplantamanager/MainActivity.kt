package br.com.andersonssales.terracuraplantamanager

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberImagePainter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.log

class MainActivity : ComponentActivity() {

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private val viewModel: PlantIdentificationViewModel by viewModels()
    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)
    private var shouldShowPlantInfo: MutableState<Boolean> = mutableStateOf(false)
    private var plantInfo: MutableState<JSONObject> = mutableStateOf(JSONObject("{}"))
    private lateinit var photoUri: Uri
    private var shouldShowPhoto: MutableState<Boolean> = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("kilo", "Permission granted")
            shouldShowCamera.value = true
        } else {
            Log.i("kilo", "Permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (shouldShowCamera.value) {
                CameraView(
                    outputDirectory = outputDirectory,
                    executor = cameraExecutor,
                    onImageCaptured = ::handleImageCapture,
                    onError = { Log.e("kilo", "View error:", it) }
                )
            }

            if (shouldShowPhoto.value) {
                Image(
                    painter = rememberImagePainter(photoUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (shouldShowPlantInfo.value) {
                PlantIdentificationScreen(viewModel)
            }
        }

        requestCameraPermission()

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("kilo", "Permission previously granted")
                shouldShowCamera.value = true
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> Log.i("kilo", "Show camera permissions dialog")

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    //    private fun handleImageCapture(uri: Uri) {
//        Log.i("kilo", "Image captured: $uri")
//        shouldShowCamera.value = false
//
//        photoUri = uri
//        shouldShowPhoto.value = true
//    }
    fun handleImageCapture(uri: Uri) {
        Log.i("kilo", "Image captured: $uri")
        shouldShowCamera.value = false

        photoUri = uri
        shouldShowPhoto.value = true

        // Encode the image to base64
        val file = File(photoUri.path)
        val imageBytes = FileInputStream(file).readBytes()
        val imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)

        // Create the JSON object
        val jsonObject = JSONObject()
        jsonObject.put("images", "[${imageBase64}]")
        jsonObject.put("latitude", 49.207)
        jsonObject.put("longitude", 16.608)
        jsonObject.put("similar_images", true)

        // Send the JSON object to the endpoint
        val request = Request.Builder()
            .url("https://plant.id/api/v3/identification?common_names,url,description,taxonomy,rank,gbif_id,inaturalist_id,image,synonyms,edible_parts,watering,propagation_methods&language=br,en")
            .post(RequestBody.create(MediaType.parse("application/json"), jsonObject.toString()))
            .addHeader("Api-Key", "GJA2ik5Ir2PDmPm3SogNxMp3nt7wcNkQvfEcG3Su46gxeKB3YX")
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("kilo", "Failed to send image: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body()?.string()
                    if (jsonResponse != null) {
//                        val jsonObject = JSONObject(jsonResponse)
                        Log.e("response", jsonResponse)
                        processApiResponse(jsonResponse)
                    } else {
                        Log.e("kilo", "Empty response body")
                    }
                } else {
                    Log.e("kilo", "Failed to send image: ${response.code()}")
                }
            }
        })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }

        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }
    // Função para processar a resposta da API e atualizar o estado da composição
    fun processApiResponse(jsonResponse: String) {
        val jsonObject = JSONObject(jsonResponse)

        viewModel.updatePlantInfo(jsonObject)
        // Atualize o estado com as informações do JSON (substitua as chaves pelos valores reais)
        plantInfo.value = jsonObject

        // Atualize o estado para exibir as informações na tela
        shouldShowPlantInfo.value = true
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}