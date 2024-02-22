package com.example.geofencingtest

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices


class MainActivity : AppCompatActivity() {

    private val CHANNEL = "gym_streak.flutter.geofence"
    private var latitude: Double? = 18.5937675
    private var longitude: Double? = 73.7587252
    private var radius: Int? = 500
    private var loitering: Int? = 10000
    private var id: String? = ""
    lateinit var geofencingClient: GeofencingClient
    val LOCATION_REQUEST_CODE = 1000

    var pendingIntent: PendingIntent? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var geofenceBroadcastReceiver = GeofenceBroadcastReceiver()
        applicationContext.registerReceiver(geofenceBroadcastReceiver, IntentFilter().also {
            it.addAction("test")
        }, RECEIVER_EXPORTED)
        registerLocationForGeofence()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                val geofenceTransition = intent?.getIntExtra("geofenceTransition", 0)

                // Communicate with Flutter via MethodChannel
                Toast.makeText(
                    applicationContext,
                    "geofenceTransition $geofenceTransition",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter("geofence_event"))

    }

    private fun registerLocationForGeofence() {
        //request location permission
        val fineLocationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val accessBackgroundLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED


        if (!fineLocationPermissionGranted || !coarseLocationPermissionGranted || (!accessBackgroundLocation && Build.VERSION.SDK_INT >= 29)) {
            // Request both fine and coarse location permissions
            var permissionArray = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
//            if (!accessBackgroundLocation && Build.VERSION.SDK_INT >= 29) {
//                permissionArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
//            }
            ActivityCompat.requestPermissions(
                this,
                permissionArray,
                LOCATION_REQUEST_CODE
            )

        } else {
            startGeofencing(id, latitude, longitude, radius, loitering)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED && (grantResults.size == 3 && grantResults[2] == PackageManager.PERMISSION_GRANTED)
            ) {
                startGeofencing(id, latitude, longitude, radius, loitering)
            } else {
                Toast.makeText(
                    applicationContext,
                    "Please grant all the permissions to start geofencing",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startGeofencing(
        id: String?,
        latitude: Double?,
        longitude: Double?,
        radius: Int?,
        loitering: Int?
    ) {
        geofencingClient = LocationServices.getGeofencingClient(this)
        if (latitude != null && longitude != null && radius != null && loitering != null) {
            val gymGeofence = Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId("gym_geofence")
                // Set the circular region of this geofence.
                .setCircularRegion(
                    latitude,
                    longitude,
                    radius.toFloat()
                )
                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .setLoiteringDelay(loitering)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                // Create the geofence.
                .build()

            Log.d("MainActivity", gymGeofence.toString())

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            getGeofencePendingIntent()?.let {
                geofencingClient.addGeofences(
                    getGeofencingRequest(gymGeofence),
                    it
                ).run {
                    addOnSuccessListener {
                        Log.d("MainActivity: Geofence", "Geofence added successfully")
                    }
                    addOnFailureListener {
                        Log.d("MainActivity: Geofence", it.message.toString())
                    }
                }
            }
        }
    }

    private fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL or GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()
    }

    private fun getGeofencePendingIntent(): PendingIntent? {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        if (pendingIntent != null) {
            return pendingIntent
        }
        pendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent
    }
}