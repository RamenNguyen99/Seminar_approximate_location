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
import com.directions.route.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
    RoutingListener {

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var ggmap: GoogleMap
    private lateinit var myLocation: LatLng
    private lateinit var destination: LatLng
    private val polylines: ArrayList<Polyline>? = null
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
        ggmap = googleMap
        updateLocationUI()
        showOnGoogleMap()
        ggmap.setOnMapClickListener(this)
    }

    override fun onMapClick(target: LatLng) {
        Log.i("onMapClick", "onMapClick: $target")
        targetMarker?.remove()
        targetMarker = ggmap.addMarker(
            MarkerOptions().position(target).title("Target Location")
        )
        ggmap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                target,
                15f
            )
        )
        destination = target
        // start route finding
        findRoutes(myLocation, destination)
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
                // Sometimes request is cancelled, the result arrays are empty.
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

    override fun onRoutingFailure(p0: RouteException?) {
        Toast.makeText(this, "Routing Fails", Toast.LENGTH_SHORT).show()
        Log.i("onRoutingFailure", "Fix this: $p0")
    }

    override fun onRoutingStart() {
        Toast.makeText(this, "Finding route...", Toast.LENGTH_SHORT).show()
    }

    override fun onRoutingSuccess(route: ArrayList<Route>?, shortestRouteIndex: Int) {
        polylines?.clear()
        val polyOptions = PolylineOptions()
        var polylineStartLatLng: LatLng? = myLocation
        var polylineEndLatLng: LatLng? = destination
        ggmap.clear()

        //add route(s) to the map using polyline
        for (i in 0 until route!!.size) {
            if (i == shortestRouteIndex) {
                polyOptions.color(resources.getColor(R.color.blue))
                polyOptions.width(12f)
                polyOptions.addAll(route[shortestRouteIndex].points)
                val polyline: Polyline = ggmap.addPolyline(polyOptions)
                polylineStartLatLng = polyline.points[0]
                val k = polyline.points.size
                polylineEndLatLng = polyline.points[k - 1]
                polylines?.add(polyline)
            } else {
                // Unit
            }
        }
        ggmap.addMarker(MarkerOptions().position(polylineStartLatLng!!).title("polylineStart"))
        ggmap.addMarker(MarkerOptions().position(polylineEndLatLng!!).title("polylineEnd"))
    }

    override fun onRoutingCancelled() {
        findRoutes(myLocation, destination)
    }

    private fun findRoutes(myLocation: LatLng, destination: LatLng) {
        val routing = Routing.Builder()
            .travelMode(AbstractRouting.TravelMode.DRIVING)
            .withListener(this)
            .alternativeRoutes(true)
            .waypoints(myLocation, destination)
            .key("AIzaSyBRIva2Tz9_H4hWRlE5nuW71tSVMCwYIa8")
            .build()
        routing.execute()
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        try {
            if (locationPermissionGranted) {
                ggmap.isMyLocationEnabled = true
                ggmap.uiSettings.isMyLocationButtonEnabled = true
            } else {
                ggmap.isMyLocationEnabled = false
                ggmap.uiSettings.isMyLocationButtonEnabled = false
                Log.i("TAG", "Goi getLocationPermission: Tu updateLocationUI: ")
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getLocationPermission() {
        Log.i("TAG", "getLocationPermission: ")
        if (
//            ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) == PackageManager.PERMISSION_GRANTED &&
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
//                ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED &&
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
                    ggmap.addMarker(
                        MarkerOptions().position(myLocation).title("My Location")
                    )
                    ggmap.animateCamera(
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
