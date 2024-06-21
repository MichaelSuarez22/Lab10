package cr.ac.una.wikipedia

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager
    private var contNotificacion = 2
    private val notifiedPlaces = mutableSetOf<String>()
    private var lastLatitude = 0.0
    private var lastLongitude = 0.0
    private val LOCATION_DIFFERENCE_THRESHOLD = 0.01

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Places.initialize(applicationContext, "AIzaSyBLiFVeg7U_Ugu5bMf7EQ_TBEfPE3vOSF4")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        startForeground(1, createNotification("Service running"))

        requestLocationUpdates()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "locationServiceChannel",
            "Location Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, "locationServiceChannel")
            .setContentTitle("Location Service")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000
        ).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { location ->
                if (isSignificantLocationChange(location.latitude, location.longitude)) {
                    getPlaceName(location.latitude, location.longitude)
                }
            }
        }
    }

    private fun isSignificantLocationChange(latitude: Double, longitude: Double): Boolean {
        val latitudeDifference = abs(latitude - lastLatitude)
        val longitudeDifference = abs(longitude - lastLongitude)
        return if (latitudeDifference > LOCATION_DIFFERENCE_THRESHOLD || longitudeDifference > LOCATION_DIFFERENCE_THRESHOLD) {
            lastLatitude = latitude
            lastLongitude = longitude
            true
        } else {
            false
        }
    }

    private fun getPlaceName(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null) {
                if (addresses.isNotEmpty() && addresses?.get(0)!!.locality != null) {
                    val cityName = addresses?.get(0)?.locality
                    sendNotification("Ubicación actual: $cityName (Lat: $latitude, Long: $longitude)", cityName)
                    if (cityName != null) {
                        fetchRelatedWikipediaContent(cityName)
                    }
                } else {
                    sendNotification("Ubicación: Latitud: $latitude, Longitud: $longitude", null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            sendNotification("Ubicación: Latitud: $latitude, Longitud: $longitude", null)
        }
    }

    private fun fetchRelatedWikipediaContent(placeName: String) {
        val url = "https://en.wikipedia.org/api/rest_v1/page/related/${placeName.replace(" ", "_")}"
        Executors.newSingleThreadExecutor().execute {
            try {
                val apiResponse = URL(url).readText()
                val jsonObject = JSONObject(apiResponse)
                val pages = jsonObject.getJSONArray("pages")

                if (pages.length() > 0) {
                    val relatedArticles = mutableListOf<String>()
                    for (i in 0 until pages.length()) {
                        val page = pages.getJSONObject(i)
                        val title = page.getString("title")
                        relatedArticles.add(title)
                    }
                    if (relatedArticles.isNotEmpty()) {
                        sendNotification("Contenidos relacionados en Wikipedia: ${relatedArticles.joinToString(", ")}", placeName)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendNotification(message: String, placeName: String?) {
        contNotificacion++

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("place_name", placeName)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "locationServiceChannel")
            .setContentTitle("Notificación de Servicio de Ubicación")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(R.mipmap.ic_launcher, "Mostrar", pendingIntent)
            .build()

        notificationManager.notify(contNotificacion, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
