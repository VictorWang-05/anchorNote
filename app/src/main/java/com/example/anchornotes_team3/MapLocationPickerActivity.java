package com.example.anchornotes_team3;

import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Activity for selecting a location on Google Maps with a geofence radius
 */
public class MapLocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private static final String TAG = "MapLocationPicker";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_RADIUS = "radius";
    public static final String EXTRA_ADDRESS = "address";
    
    private static final LatLng DEFAULT_LOCATION = new LatLng(34.0224, -118.2851); // USC
    private static final float DEFAULT_ZOOM = 15f;
    private static final int DEFAULT_RADIUS = 200;
    
    private GoogleMap map;
    private Circle radiusCircle;
    private LatLng selectedLocation;
    private int currentRadius = DEFAULT_RADIUS;
    private String selectedAddress = "";
    
    private Slider radiusSlider;
    private TextView tvRadiusValue;
    private TextView tvSelectedAddress;
    private MaterialButton btnConfirm;
    private MaterialButton btnCancel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location_picker);
        
        // Initialize Places API (you'll need to add API key in manifest)
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        
        // Initialize views
        radiusSlider = findViewById(R.id.slider_map_radius);
        tvRadiusValue = findViewById(R.id.tv_map_radius_value);
        tvSelectedAddress = findViewById(R.id.tv_selected_address);
        btnConfirm = findViewById(R.id.btn_confirm_location);
        btnCancel = findViewById(R.id.btn_cancel_location);
        
        // Setup slider
        radiusSlider.setValueFrom(50);
        radiusSlider.setValueTo(1000);
        radiusSlider.setValue(DEFAULT_RADIUS);
        radiusSlider.setStepSize(50);
        tvRadiusValue.setText(DEFAULT_RADIUS + " meters");
        
        radiusSlider.addOnChangeListener((slider, value, fromUser) -> {
            currentRadius = (int) value;
            tvRadiusValue.setText(currentRadius + " meters");
            updateRadiusCircle();
        });
        
        // Setup buttons
        btnConfirm.setOnClickListener(v -> confirmLocation());
        btnCancel.setOnClickListener(v -> finish());
        
        // Setup map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        // Setup Places Autocomplete
        setupPlacesAutocomplete();
        
        // Check if we have an existing location to show
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_LATITUDE) && intent.hasExtra(EXTRA_LONGITUDE)) {
            double lat = intent.getDoubleExtra(EXTRA_LATITUDE, DEFAULT_LOCATION.latitude);
            double lng = intent.getDoubleExtra(EXTRA_LONGITUDE, DEFAULT_LOCATION.longitude);
            int radius = intent.getIntExtra(EXTRA_RADIUS, DEFAULT_RADIUS);
            String address = intent.getStringExtra(EXTRA_ADDRESS);
            
            selectedLocation = new LatLng(lat, lng);
            currentRadius = radius;
            selectedAddress = address != null ? address : "";
            
            radiusSlider.setValue(currentRadius);
            tvSelectedAddress.setText(selectedAddress);
        }
    }
    
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        
        // Enable map controls
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(true);
        
        // Set map click listener
        map.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            updateMapLocation();
            reverseGeocode(latLng);
        });
        
        // If we have a selected location, show it
        if (selectedLocation != null) {
            updateMapLocation();
        } else {
            // Move to default location
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
        }
    }
    
    private void setupPlacesAutocomplete() {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        
        if (autocompleteFragment != null) {
            // Specify the types of place data to return
            autocompleteFragment.setPlaceFields(Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS
            ));
            
            // Set up place selection listener
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    Log.d(TAG, "Place selected: " + place.getName());
                    
                    if (place.getLatLng() != null) {
                        selectedLocation = place.getLatLng();
                        selectedAddress = place.getAddress() != null ? place.getAddress() : place.getName();
                        tvSelectedAddress.setText(selectedAddress);
                        updateMapLocation();
                    }
                }
                
                @Override
                public void onError(@NonNull Status status) {
                    Log.e(TAG, "Place selection error: " + status);
                    Toast.makeText(MapLocationPickerActivity.this, 
                            "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void updateMapLocation() {
        if (map == null || selectedLocation == null) return;
        
        // Clear existing markers and circles
        map.clear();
        
        // Add marker at selected location
        map.addMarker(new MarkerOptions()
                .position(selectedLocation)
                .title("Selected Location"));
        
        // Update radius circle
        updateRadiusCircle();
        
        // Move camera to location
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, DEFAULT_ZOOM));
    }
    
    private void updateRadiusCircle() {
        if (map == null || selectedLocation == null) return;
        
        // Remove existing circle
        if (radiusCircle != null) {
            radiusCircle.remove();
        }
        
        // Add new circle
        radiusCircle = map.addCircle(new CircleOptions()
                .center(selectedLocation)
                .radius(currentRadius)
                .strokeColor(Color.parseColor("#FF6200EE"))
                .strokeWidth(2)
                .fillColor(Color.parseColor("#2206A0F7")));
    }
    
    private void reverseGeocode(LatLng latLng) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        latLng.latitude, latLng.longitude, 1);
                
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(address.getAddressLine(i));
                    }
                    
                    selectedAddress = sb.toString();
                    runOnUiThread(() -> tvSelectedAddress.setText(selectedAddress));
                    Log.d(TAG, "Reverse geocoded to: " + selectedAddress);
                }
            } catch (IOException e) {
                Log.e(TAG, "Reverse geocoding failed", e);
                selectedAddress = String.format(Locale.US, "%.6f, %.6f", 
                        latLng.latitude, latLng.longitude);
                runOnUiThread(() -> tvSelectedAddress.setText(selectedAddress));
            }
        }).start();
    }
    
    private void confirmLocation() {
        if (selectedLocation == null) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Return result
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_LATITUDE, selectedLocation.latitude);
        resultIntent.putExtra(EXTRA_LONGITUDE, selectedLocation.longitude);
        resultIntent.putExtra(EXTRA_RADIUS, currentRadius);
        resultIntent.putExtra(EXTRA_ADDRESS, selectedAddress);
        
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}

