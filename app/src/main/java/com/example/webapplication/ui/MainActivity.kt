package com.example.webapplication.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.webapplication.databinding.ActivityMainBinding
import com.example.webapplication.util.AndroidDownloader
import com.example.webapplication.util.Constants.CAMERA_PERMISSION
import com.example.webapplication.util.Constants.LOCATION_PERMISSION
import com.example.webapplication.util.Util
import com.example.webapplication.util.Util.getHtmlContent
import com.example.webapplication.util.Util.isLocationEnabled

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webAppInterface: WebAppInterface
    private lateinit var downloader: AndroidDownloader

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isCameraGranted ->
            handleCameraPermissionResult(isCameraGranted)
        }

    private val requestBarcodePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isCameraGranted ->
            handleBarcodePermissionResult(isCameraGranted)
        }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isLocationGranted ->
            handleLocationPermissionResult(isLocationGranted)
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleImageCaptureResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeWebView()
        // Load your web page with HTML content including buttons for requesting permissions
           binding.webView.loadDataWithBaseURL(null, getHtmlContent(), "text/html", "utf-8", null)
      //  binding.webView.loadUrl("https://www98.verizon.com/fieldops-sit/omegaapp")
        downloader = AndroidDownloader(this)
    }

    private fun initializeWebView() {
        binding.webView.settings.javaScriptEnabled = true
        webAppInterface = WebAppInterface()
        binding.webView.addJavascriptInterface(webAppInterface, "Android")
        binding.webView.webViewClient = WebViewClient()
        binding.webView.webChromeClient = WebChromeClient()
    }

    private fun handleCameraPermissionResult(isCameraGranted: Boolean) {
        if (isCameraGranted) {
            Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleBarcodePermissionResult(isCameraGranted: Boolean) {
        if (isCameraGranted) {
            Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            openBarcode()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBarcode() {
        startActivity(Intent(this, BarcodeScannerActivity::class.java))
    }

    private fun handleLocationPermissionResult(isLocationGranted: Boolean) {
        if (isLocationGranted) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
            webAppInterface.fetchLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImageCaptureResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                // Convert the Bitmap to a Base64-encoded string
                val imageData = Util.bitmapToBase64(imageBitmap)

                // Call the displayImage method in the WebView
                webAppInterface.displayImage(imageData)
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(cameraIntent)
        } else {
            Toast.makeText(this, "Camera app not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchLocationNative() {
        if (ContextCompat.checkSelfPermission(
                this,
                LOCATION_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (isLocationEnabled(locationManager)) {
                val location = getLastKnownLocation(locationManager)
                Toast.makeText(
                    this,
                    "Location: ${location?.latitude}, ${location?.longitude}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "GPS is not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Use the instance of MainActivity to call the method
            webAppInterface.requestLocationPermission()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(locationManager: LocationManager): Location? {
        val providers: List<String> = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                bestLocation = location
            }
        }
        return bestLocation
    }


    // JavaScript interface for communication between WebView and Android
    inner class WebAppInterface() {
        @JavascriptInterface
        fun requestCameraPermission() {
            requestCameraPermissionLauncher.launch(CAMERA_PERMISSION)
        }

        @JavascriptInterface
        fun requestLocationPermission() {
            requestLocationPermissionLauncher.launch(LOCATION_PERMISSION)
        }

        @JavascriptInterface
        fun fetchLocation() {
            fetchLocationNative()
        }

        @JavascriptInterface
        fun displayImage(imageData: String) {
            binding.webView.post {
                // Load the image data into the WebView
                binding.webView.loadUrl("javascript:displayImage('$imageData')")
            }
        }

        @JavascriptInterface
        fun requestBarcodePermission() {
            requestBarcodePermissionLauncher.launch(CAMERA_PERMISSION)
        }

        @JavascriptInterface
        fun downloadFile() {
            downloader.downloadImageFile("https://drive.google.com/file/d/1neOXwlY7kKqcP5l8Dw4sWjqxtb1gHdz3/view?usp=sharing")
            downloader.downloadPdfFile("https://drive.google.com/file/d/1oMxtapF4RIFh7KVFRud3crmwXvMAMfes/view?usp=sharing")
            downloader.downloadDocFile("https://docs.google.com/document/d/12pW6NY4h9M0BLmsRjXe8NYlMk4LFnNrh/edit?usp=sharing&ouid=101116084773656515411&rtpof=true&sd=true")
            downloader.downloadApkFile("https://drive.google.com/file/d/1klYUGMgqc9oXV36MNJThV73fiFgbw6Iu/view?usp=sharing")
        }
    }
}