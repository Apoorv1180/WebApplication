package com.example.webapplication.util

import android.graphics.Bitmap
import android.location.LocationManager
import android.util.Base64
import java.io.ByteArrayOutputStream

object Util {

    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun getHtmlContent(): String {
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
        htmlBuilder.append("<button onclick=\"requestBarcodePermission()\">Request Barcode Permission</button>")
        htmlBuilder.append("<button onclick=\"fetchLocation()\">Fetch Location</button>")
        htmlBuilder.append("<img id=\"capturedImage\" style=\"display: none; max-width: 100%;\" />")
        htmlBuilder.append("<script>")
        htmlBuilder.append("function requestCameraPermission() { Android.requestCameraPermission(); }")
        htmlBuilder.append("function requestLocationPermission() { Android.requestLocationPermission(); }")
        htmlBuilder.append("function requestBarcodePermission() { Android.requestBarcodePermission(); }")
        htmlBuilder.append("function fetchLocation() { Android.fetchLocation(); }")
        htmlBuilder.append("function displayImage(imageData) { document.getElementById('capturedImage').src = \"data:image/png;base64,\" + imageData; document.getElementById('capturedImage').style.display = 'block'; }")
        htmlBuilder.append("</script>")
        htmlBuilder.append("</body>")
        htmlBuilder.append("</html>")

        return htmlBuilder.toString()
    }

    fun isLocationEnabled(locationManager: LocationManager): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

}