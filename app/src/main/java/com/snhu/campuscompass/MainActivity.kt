package com.snhu.campuscompass


import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fromSpinner: Spinner
    private lateinit var toSpinner: Spinner
    private lateinit var findPathButton: Button
    private val campusMarkers = mutableListOf<CampusMarker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        fromSpinner = findViewById(R.id.spinnerFrom)
        toSpinner = findViewById(R.id.spinnerTo)
        findPathButton = findViewById(R.id.findPathButton)

        // Set up the map fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Fetch markers from PostgreSQL in a separate thread
        thread {
            Log.d("MainActivity", "Fetching markers from API")
            // Call the REST API
            fetchMarkersFromAPI()
        }

        // Set up the button click listener
        findPathButton.setOnClickListener {
            val from = fromSpinner.selectedItem.toString()
            val to = toSpinner.selectedItem.toString()
            highlightPath(from, to)
        }
    }

    private fun fetchMarkersFromAPI() {
        val apiUrl = "http://10.0.2.2:3000/campus_markers" // Use 10.0.2.2 for Android emulator

        NetworkClient.fetchMarkers(apiUrl) { response, error ->
            runOnUiThread {
                if (error != null) {
                    Log.e("MainActivity", "Error fetching markers: ${error.message}")
                    return@runOnUiThread
                }

                response?.let {
                    parseMarkers(it)
                }
            }
        }
    }

    private fun parseMarkers(response: String) {
        val markersList = mutableListOf<CampusMarker>()
        val jsonArray = JSONArray(response)

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val marker = CampusMarker(
                id = jsonObject.getInt("id"),
                name = jsonObject.getString("name"),
                latitude = jsonObject.getDouble("latitude"),
                longitude = jsonObject.getDouble("longitude")
            )
            markersList.add(marker)
        }

        // Update the UI with the markers and populate spinners
        runOnUiThread {
            campusMarkers.clear()
            campusMarkers.addAll(markersList)
            addMarkersToMap(markersList)
            populateSpinners(markersList)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val snhuLocation = LatLng(42.9956, -71.4548)  // Example SNHU coordinates
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(snhuLocation, 10f))
    }


    private fun populateSpinners(markers: List<CampusMarker>) {
        val markerNames = markers.map { it.name }

        Log.d("MainActivity", "Populating spinners with: $markerNames")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, markerNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fromSpinner.adapter = adapter
        toSpinner.adapter = adapter
    }

    private fun addMarkersToMap(markers: List<CampusMarker>) {
        markers.forEach { marker ->
            val position = LatLng(marker.latitude, marker.longitude)
            mMap.addMarker(MarkerOptions().position(position).title(marker.name))
        }
    }

    private fun highlightPath(from: String, to: String) {
        val fromMarker = campusMarkers.find { it.name == from }
        val toMarker = campusMarkers.find { it.name == to }

        if (fromMarker != null && toMarker != null) {
            val fromPosition = LatLng(fromMarker.latitude, fromMarker.longitude)
            val toPosition = LatLng(toMarker.latitude, toMarker.longitude)

            // Draw a line between the two markers
            mMap.addPolyline(
                com.google.android.gms.maps.model.PolylineOptions()
                    .add(fromPosition, toPosition)
                    .width(5f)
                    .color(android.graphics.Color.BLUE)
            )

            // Move camera to the path center
            val centerPosition = LatLng(
                (fromMarker.latitude + toMarker.latitude) / 2,
                (fromMarker.longitude + toMarker.longitude) / 2
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerPosition, 10f))
        }
    }
}
