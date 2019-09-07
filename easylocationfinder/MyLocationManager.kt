package freny.christian.easylocationfinder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions

class MyLocationManager(var context: Activity , var locationChangeListener: LocationChangeListener) : LocationListener,
    GoogleApiClient.ConnectionCallbacks , GoogleApiClient.OnConnectionFailedListener , android.location.LocationListener {

    val REQUEST_CHECK_SETTINGS_GPS = 2001

    val TAG = "MyLocationManager"
    var mGoogleApiClient: GoogleApiClient? = null
    internal var locationRequest: LocationRequest? = null

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.e(TAG , "onConnectionFailed : ${p0.errorMessage}")

    }

    override fun onConnected(p0: Bundle?) {
        Log.e(TAG , "onConnected")
        requestLocationUpdates()
    }

    override fun onConnectionSuspended(p0: Int) {

    }

    override fun onLocationChanged(p0: Location?) {
        locationChangeListener.onLocationChange(p0)
    }


    fun connectApiClient() = context.runWithPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION ,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) {
        Log.e(TAG , "In an ConnectApiClient ${getGoogleApiClient().isConnected}")
        if (!getGoogleApiClient().isConnected) {
            getGoogleApiClient().connect()
        } else {
            requestLocationUpdates()
        }
    }

    fun getGoogleApiClient(): GoogleApiClient {
        if (mGoogleApiClient == null)
            mGoogleApiClient = GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
        return mGoogleApiClient!!

    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() =
        context.runWithPermissions(Manifest.permission.ACCESS_COARSE_LOCATION , Manifest.permission.ACCESS_FINE_LOCATION) {
            Log.e(TAG , "In an requestLocationUpdates")
            if (getGoogleApiClient().isConnected) {

                LocationServices.FusedLocationApi.requestLocationUpdates(
                    getGoogleApiClient() ,
                    getLocationRequest() ,
                    this
                )
                checkLocationGPS()

            } else {
                getGoogleApiClient().connect()
            }
        }

    private fun getLocationRequest(): LocationRequest {
        if (locationRequest == null) {
            locationRequest = LocationRequest()
            locationRequest!!.interval = 1000
            locationRequest!!.fastestInterval = 1000
            locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        return locationRequest as LocationRequest
    }

    fun checkLocationGPS() {
        Log.e(TAG , "in checkLocationGPS")
        val builder = LocationSettingsRequest.Builder().addLocationRequest(getLocationRequest())
        val result = LocationServices.SettingsApi.checkLocationSettings(getGoogleApiClient() , builder.build())

        result.setResultCallback { it ->
            val status = it.status
            Log.e(TAG , "status : $status")
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> {
                    val permissionLocation = ContextCompat.checkSelfPermission(
                        context ,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                        val location = getLastLocation(getGoogleApiClient())
                        Log.e(TAG , "location in result callback$location")
                        if (location != null) {
                            Log.e(TAG , "Location : $location")
                            onLocationChanged(location)
                        } else {
                            checkLocationGPS()
                        }
                    }
                }

                LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->

                    //if(prefHelper.currentLatitude.isNullOrEmpty() || prefHelper.currentLongitude.isNullOrEmpty()) {
                    try {
                        status.startResolutionForResult(
                            context ,
                            REQUEST_CHECK_SETTINGS_GPS
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                // }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                }
                LocationSettingsStatusCodes.CANCELED -> {
                    Log.e(TAG , "status 2 : $status")

                }
            }
        }

    }

    fun getLastLocation(googleApiClient: GoogleApiClient): Location? {
        if (ActivityCompat.checkSelfPermission(
                context ,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context ,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
    }

    override fun onStatusChanged(provider: String? , status: Int , extras: Bundle?) {

        Log.e(TAG , "onStatusChanged  $status")
        checkLocationGPS()
    }

    override fun onProviderEnabled(provider: String?) {
        checkLocationGPS()
    }

    override fun onProviderDisabled(provider: String?) {
        checkLocationGPS()

    }

    fun removeCallback() {
        LocationServices.FusedLocationApi.removeLocationUpdates(getGoogleApiClient(), this)

    }

    interface LocationChangeListener {
        fun onLocationChange(location: Location?)
    }

}