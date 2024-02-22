package com.example.geofencingtest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("GeofenceBroadcastReceiver: onReceive Called", "On Recieve called")

        val geofencingEvent = intent?.let { GeofencingEvent.fromIntent(it) }
        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()) {
                val errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.errorCode)
                Log.e("GeofenceBroadcastReceiver", errorMessage)
                return
            }
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent?.geofenceTransition

        Log.d("GeofenceBroadcastReceiver: geofenceTransition", geofenceTransition.toString())


        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL
        ) {
            val localBroadcastManager = LocalBroadcastManager.getInstance(context!!)
            val localIntent = Intent()
            localIntent.action = "geofence_event"
            localIntent.putExtra("geofenceTransition", geofenceTransition)
            localBroadcastManager.sendBroadcast(localIntent)
            Log.d("GeofenceBroadcastReceiver: geofenceTransition", geofenceTransition.toString())
        } else {
            // Log the error.
            Log.e("GeofenceBroadcastReceiver", "Invalid Transition")
        }
    }
}
