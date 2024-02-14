package com.example.webapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION = Manifest.permission.CAMERA
    private val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    private lateinit var webView: WebView
    private lateinit var webAppInterface: WebAppInterface


    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isCameraGranted ->
            handleCameraPermissionResult(isCameraGranted)
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
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        initializeWebView()

        // Load your web page with HTML content including buttons for requesting permissions
        webView.loadDataWithBaseURL(null, getHtmlContent(), "text/html", "utf-8", null)
    }

    private fun getHtmlContent(): String {
        val htmlBuilder = StringBuilder()

        htmlBuilder.append("<html>")
        htmlBuilder.append("<head>")
        htmlBuilder.append("<style>")
        htmlBuilder.append("body { font-family: Arial, sans-serif; padding: 10px; display: flex; flex-direction: column; align-items: center; }")
        htmlBuilder.append("button { margin: 10px; padding: 10px; font-size: 16px; }")
        htmlBuilder.append("img { display: none; max-width: 100%; }")
        htmlBuilder.append("</style>")
        htmlBuilder.append("</head>")
        htmlBuilder.append("<body>")
        htmlBuilder.append("<button onclick=\"requestCameraPermission()\">Request Camera Permission</button>")
        htmlBuilder.append("<button onclick=\"requestLocationPermission()\">Request Location Permission</button>")
        htmlBuilder.append("<button onclick=\"fetchLocation()\">Fetch Location</button>")
        htmlBuilder.append("<img id=\"capturedImage\" style=\"display: none; max-width: 100%;\" />")
        htmlBuilder.append("<script>")
        htmlBuilder.append("function requestCameraPermission() { Android.requestCameraPermission(); }")
        htmlBuilder.append("function requestLocationPermission() { Android.requestLocationPermission(); }")
        htmlBuilder.append("function fetchLocation() { Android.fetchLocation(); }")
        htmlBuilder.append("function displayImage(imageData) { document.getElementById('capturedImage').src = \"data:image/png;base64,\" + imageData; document.getElementById('capturedImage').style.display = 'block'; }")
        htmlBuilder.append("</script>")
        htmlBuilder.append("</body>")
        htmlBuilder.append("</html>")

        return htmlBuilder.toString()
    }

    private fun initializeWebView() {
        webView.settings.javaScriptEnabled = true
        webAppInterface = WebAppInterface(this)
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
    }

    private fun handleCameraPermissionResult(isCameraGranted: Boolean) {
        if (isCameraGranted) {
            Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLocationPermissionResult(isLocationGranted: Boolean) {
        if (isLocationGranted) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
            webAppInterface.fetchLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
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
        if (ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
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

    private fun handleImageCaptureResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                // Convert the Bitmap to a Base64-encoded string
                val imageData = bitmapToBase64(imageBitmap)

                // Call the displayImage method in the WebView
                webAppInterface.displayImage(imageData)
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
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

    private fun isLocationEnabled(locationManager: LocationManager): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // JavaScript interface for communication between WebView and Android
    inner class WebAppInterface(private val context: Context) {
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
            webView.post {
                // Load the image data into the WebView
                webView.loadUrl("javascript:displayImage('$imageData')")
            }
        }
    }
}