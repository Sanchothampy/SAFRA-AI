package com.safra.app.ui.activity;

import com.safra.app.R;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import java.util.Calendar;
import com.safra.app.ai.RiskEngine;
import com.safra.app.ai.RouteSafetyAnalyzer;
import android.graphics.Color;
import android.widget.TextView;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "MapActivity";

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private RequestQueue requestQueue;

    private LatLng userLatLng;

    // API Key will be loaded from the manifest
    private String MAPS_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // CRITICAL: Get the API Key from AndroidManifest.xml
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            // Key name must match the one in AndroidManifest.xml
            MAPS_API_KEY = bundle.getString("com.google.android.geo.API_KEY");
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Failed to load API Key from manifest.", e);
            MAPS_API_KEY = null; // Set to null if failed
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        checkLocationPermissions();
    }

    // ---------------------------------------------------------------------------------------------
    // LOCATION PERMISSION HANDLING
    // ---------------------------------------------------------------------------------------------

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            googleMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissions();
            } else {
                Toast.makeText(this, "Location permission is required to show your position and nearby places.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // GET USER LOCATION AND START SEARCH
    // ---------------------------------------------------------------------------------------------

    private void getCurrentLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f));

                            // >>> CRITICAL: START THE SEARCH <<<
                            if (MAPS_API_KEY != null && !MAPS_API_KEY.isEmpty()) {
                                searchNearbyPoliceStations(userLatLng);
                            } else {
                                Toast.makeText(this, "API Key not loaded. Check Manifest.", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "API Key is missing or failed to load from Manifest.");
                            }

                        } else {
                            Toast.makeText(this, "Could not get current location. Ensure GPS is enabled.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Log.e(TAG, "Error getting location", e);
                        Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permissions not fully granted.", e);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // GOOGLE PLACES NEARBY SEARCH
    // ---------------------------------------------------------------------------------------------

    private void searchNearbyPoliceStations(LatLng userLocation) {
        int radius = 5000;
        String type = "police";

        String url = String.format("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=%d&type=%s&key=%s",
                userLocation.latitude, userLocation.longitude, radius, type, MAPS_API_KEY);

        // Volley String Request
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // SUCCESS: Log the response to check for 'status: ZERO_RESULTS' or other issues
                        Log.d(TAG, "Places API Success Response: " + response);
                        parsePoliceStations(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // ERROR: Log the full error
                        Log.e(TAG, "Volley Network/API Error: " + error.toString());
                        Toast.makeText(MapActivity.this, "Error searching: Check Internet/API Key.", Toast.LENGTH_LONG).show();
                    }
                });

        requestQueue.add(stringRequest);
    }

    private void parsePoliceStations(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);

            // CRITICAL CHECK: Look for the status field
            String status = root.getString("status");
            if (userLatLng == null) {
                Log.e(TAG, "User location is null. Cannot calculate risk.");
                return;
            }

            if (!status.equals("OK")) {
                Log.w(TAG, "Places API Status Not OK: " + status);
                // If status is "ZERO_RESULTS", there are no police stations nearby (common in simulators)
                if (status.equals("ZERO_RESULTS")) {
                    Toast.makeText(this, "No police stations found within 5km.", Toast.LENGTH_LONG).show();
                } else {
                    // Could be REQUEST_DENIED (API Key issue) or INVALID_REQUEST
                    Toast.makeText(this, "Place search failed: " + status, Toast.LENGTH_LONG).show();
                }
                return;
            }

            JSONArray results = root.getJSONArray("results");
            int policeCount = results.length();
            double minDistance = Double.MAX_VALUE;

            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.getJSONObject(i);

                String name = place.getString("name");
                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");

                float[] distanceResult = new float[1];

                android.location.Location.distanceBetween(
                        userLatLng.latitude,
                        userLatLng.longitude,
                        lat,
                        lng,
                        distanceResult
                );

                double distance = distanceResult[0];

                if (distance < minDistance) {
                    minDistance = distance;
                }

                LatLng placeLatLng = new LatLng(lat, lng);

                googleMap.addMarker(new MarkerOptions()
                        .position(placeLatLng)
                        .title(name)
                        .snippet("Police Station"));
            }

            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            int riskScore = RiskEngine.calculateRisk(
                    currentHour,
                    minDistance,
                    policeCount,
                    5.0,
                    false
            );

            String riskLevel = RouteSafetyAnalyzer.getRiskLevel(riskScore);
            String recommendation = RouteSafetyAnalyzer.getRecommendation(riskScore);

            TextView riskText = findViewById(R.id.riskTextView);

            riskText.setText("Risk Score: " + riskScore + "%\n" + "Level: " + riskLevel + "\n" + recommendation);
            if (riskScore > 60) {
                riskText.setTextColor(Color.RED);
            } else if (riskScore > 30) {
                riskText.setTextColor(Color.parseColor("#FFA500"));
            } else {
                riskText.setTextColor(Color.GREEN);
            }

        } catch (JSONException e) {
            Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
            Toast.makeText(this, "Failed to process place data.", Toast.LENGTH_SHORT).show();
        }
    }
}