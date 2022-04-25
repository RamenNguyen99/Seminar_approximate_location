package com.example.approximate_location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var map: GoogleMap
    private lateinit var myLocation: LatLng

    //    private lateinit var destination: LatLng
    private var targetMarker: Marker? = null
    private var locationPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapFragment = supportFragmentManager.findFragmentById(R.id.ggMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Construct a PlacesClient

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        Log.i("TAG", "Goi getLocationPermission: Tu onCreate: ")
        getLocationPermission()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.i("TAG", "onMapReady: onMapReady")
        map = googleMap
        map.setOnMapClickListener(this)
        updateLocationUI()
        showOnGoogleMap()
    }

    override fun onMapClick(target: LatLng) {
        Log.i("onMapClick", "onMapClick: $target")
        targetMarker?.remove()
        targetMarker = map.addMarker(
            MarkerOptions().position(target).title("Target Location")
        )
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                target,
                15f
            )
        )
//        destination = target
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSION_REQUEST_ACCESS_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.i("TAG", "onRequestPermissionsResult: Granted")
                    Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show()
                    locationPermissionGranted = true
                } else {
                    Log.i("TAG", "onRequestPermissionsResult: Denied")
                    Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show()
                }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        updateLocationUI()
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        try {
            if (locationPermissionGranted) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
            } else {
                map.isMyLocationEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
//                lastKnownLocation = null
                Log.i("TAG", "Goi getLocationPermission: Tu updateLocationUI: ")
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getLocationPermission() {
        Log.i("TAG", "getLocationPermission: ")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            Log.i("TAG", "Goi requestPermission(): tu getLocationPermission")
            requestPermission()
        }
    }

    private fun requestPermission() {
        Log.i("TAG", "requestPermission: ")
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    private fun showOnGoogleMap() {
        if (isLocationEnable()) {
            if (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i("TAG", "Goi requestPermission(): tu showOnGoogleMap")
                requestPermission()
                return
            }

            // final latitude & longitude code here
            fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                val location: Location? = task.result
                if (location == null) {
                    Toast.makeText(this, "Null receiver", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Get location success", Toast.LENGTH_SHORT).show()
                    myLocation = LatLng(location.latitude, location.longitude)
                    tvLatitude.text = location.latitude.toString()
                    tvLongitude.text = location.longitude.toString()
                    Log.i("TAG", "showOnGoogleMap: Lay thanh cong")
                    map.addMarker(
                        MarkerOptions().position(myLocation).title("My Location")
                    )
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            myLocation,
                            15f
                        )
                    )
                }
            }
        } else {
            // setting open here
            Toast.makeText(this, "Please turn on location", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    private fun isLocationEnable(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
