package com.example.geofencingtest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.Calendar
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : AppCompatActivity() {

    private val CHANNEL = "gym_streak.flutter.geofence"
    private var latitude: Double = 18.5937675
    private var longitude: Double = 73.7587252
    private var radius: Int? = 500
    private var loitering: Int? = 10000
    private var id: String? = ""
    val LOCATION_REQUEST_CODE = 1000
    var enterTime: Long? = null
    var exitTime: Long? = null

    private var fusedLocationClient: FusedLocationProviderClient? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerLocationForGeofence()
        val intent = Intent(this, GeofenceService::class.java) // Build the intent for the service
        this.startForegroundService(intent)
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

        if (Build.VERSION.SDK_INT >= TIRAMISU) {
            val postNotification = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!postNotification) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    LOCATION_REQUEST_CODE
                )
            }
        }

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
            requestLocationUpdates(latitude, longitude)
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
                requestLocationUpdates(latitude, longitude)
            } else {
                Toast.makeText(
                    applicationContext,
                    "Please grant all the permissions to start geofencing",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun requestLocationUpdates(lat: Double, lon: Double) {
        val locationRequest: LocationRequest = LocationRequest.Builder(LocationRequest.PRIORITY_HIGH_ACCURACY, 10000).build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient?.requestLocationUpdates(locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    val location: Location? = locationResult.lastLocation
                    if (location != null) {
                        val distance: Double = calculateDistance(
                            location.getLatitude(), location.getLongitude(),
                            lat, lon
                        ) // Replace with your target lat/long
                        Toast.makeText(applicationContext, "Lat lon $location", Toast.LENGTH_SHORT).show()

                        if (distance < (radius!!/1000.0)) {
                            Log.d("MainActivity: distance", distance.toString())
                            if (enterTime == null) {
                                enterTime = Calendar.getInstance().time.time
                                Log.d("MainActivity: enter time set", enterTime.toString())
                                Toast.makeText(applicationContext, "Dwelling started", Toast.LENGTH_SHORT).show()
                            }
                          Toast.makeText(applicationContext, "In Geofence Area", Toast.LENGTH_SHORT).show()
                        } else {
                            if (enterTime != null) {
                                exitTime = Calendar.getInstance().time.time
                                if (exitTime!!.minus(enterTime!!) < loitering!!) {
                                    Log.d("Streak not achieved: ", exitTime!!.minus(enterTime!!).toString())
                                    Toast.makeText(applicationContext, "Streak not achieved", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.d("Streak achieved: ", exitTime!!.minus(enterTime!!).toString())
                                    Toast.makeText(applicationContext, "Streak achieved", Toast.LENGTH_SHORT).show()
                                    enterTime = null
                                    exitTime = null
                                }
                            }
                        }
                        Log.d("Distance", "Distance: $distance")
                        // Do something with the distance
                    }
                }
            }, null
        )
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = sin(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) + cos(
            Math.toRadians(lat1)
        ) * cos(
            Math.toRadians(lat2)
        ) * cos(Math.toRadians(theta))
        dist = acos(dist)
        dist = Math.toDegrees(dist)
        dist *= 60 * 1.1515
        return dist * 1.60934
    }
}