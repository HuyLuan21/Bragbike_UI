package com.example.bragbike.users;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bragbike.R;
import com.example.bragbike.api.MapboxService;
import com.example.bragbike.databinding.ActivityBookingBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.JsonObject;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.LayerUtils;
import com.mapbox.maps.extension.style.layers.generated.CircleLayer;
import com.mapbox.maps.extension.style.layers.generated.LineLayer;
import com.mapbox.maps.extension.style.sources.SourceUtils;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.animation.CameraAnimationsUtils;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BookingActivity extends AppCompatActivity {

    private ActivityBookingBinding binding;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    
    // IDs cho Route
    private static final String ROUTE_SOURCE_ID = "route-source-id";
    private static final String ROUTE_LAYER_ID = "route-layer-id";
    
    // IDs cho Markers
    private static final String ORIGIN_SOURCE_ID = "origin-source-id";
    private static final String DEST_SOURCE_ID = "dest-source-id";
    private static final String ORIGIN_LAYER_ID = "origin-layer-id";
    private static final String DEST_LAYER_ID = "dest-layer-id";

    private FusedLocationProviderClient fusedLocationClient;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private SuggestionAdapter suggestionAdapter;
    private MapboxService mapboxService;

    private Point originPoint;
    private Point destinationPoint;
    private boolean isBikeSelected = true;
    private boolean isSearchingOrigin = true;
    private boolean isProgrammaticChange = false;

    private final double pricePerKmBike = 5000; 
    private final double pricePerKmCar = 12000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initMapboxRetrofit();
        initMap();
        setupBottomSheet();
        setupRecyclerView();
        setupClickListeners();
        setupSearchInput();
    }

    private void initMapboxRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.mapbox.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mapboxService = retrofit.create(MapboxService.class);
    }

    private void initMap() {
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            initLocationComponent();
            checkPermissionAndGetLocation();
            initRouteLayers(style);
            initMarkerLayers(style);
        });
    }

    private void initRouteLayers(Style style) {
        GeoJsonSource routeSource = new GeoJsonSource.Builder(ROUTE_SOURCE_ID).build();
        SourceUtils.addSource(style, routeSource);

        LineLayer routeLayer = new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID);
        routeLayer.lineColor(Color.parseColor("#4285F4"));
        routeLayer.lineWidth(5.0);
        LayerUtils.addLayer(style, routeLayer);
    }

    private void initMarkerLayers(Style style) {
        // Marker điểm đón (Xanh)
        GeoJsonSource originSource = new GeoJsonSource.Builder(ORIGIN_SOURCE_ID).build();
        SourceUtils.addSource(style, originSource);
        CircleLayer originLayer = new CircleLayer(ORIGIN_LAYER_ID, ORIGIN_SOURCE_ID);
        originLayer.circleColor(Color.parseColor("#4285F4"));
        originLayer.circleRadius(8.0);
        originLayer.circleStrokeColor(Color.WHITE);
        originLayer.circleStrokeWidth(2.0);
        LayerUtils.addLayer(style, originLayer);

        // Marker điểm đến (Vàng)
        GeoJsonSource destSource = new GeoJsonSource.Builder(DEST_SOURCE_ID).build();
        SourceUtils.addSource(style, destSource);
        CircleLayer destLayer = new CircleLayer(DEST_LAYER_ID, DEST_SOURCE_ID);
        destLayer.circleColor(Color.parseColor("#F4B400"));
        destLayer.circleRadius(8.0);
        destLayer.circleStrokeColor(Color.WHITE);
        destLayer.circleStrokeWidth(2.0);
        LayerUtils.addLayer(style, destLayer);
    }

    private void updateMarkerPositions() {
        binding.mapView.getMapboxMap().getStyle(style -> {
            GeoJsonSource oSource = (GeoJsonSource) SourceUtils.getSource(style, ORIGIN_SOURCE_ID);
            if (oSource != null && originPoint != null) {
                oSource.geometry(originPoint);
            }
            GeoJsonSource dSource = (GeoJsonSource) SourceUtils.getSource(style, DEST_SOURCE_ID);
            if (dSource != null && destinationPoint != null) {
                dSource.geometry(destinationPoint);
            }
        });
    }

    private void initLocationComponent() {
        LocationComponentPlugin locationComponentPlugin = LocationComponentUtils.getLocationComponent(binding.mapView);
        locationComponentPlugin.setEnabled(true);
    }

    private void setupRecyclerView() {
        suggestionAdapter = new SuggestionAdapter(feature -> {
            Point point = feature.center();
            if (point != null) {
                isProgrammaticChange = true;
                if (isSearchingOrigin) {
                    originPoint = point;
                    binding.etOrigin.setText(feature.placeName());
                } else {
                    destinationPoint = point;
                    binding.etDestination.setText(feature.placeName());
                }
                isProgrammaticChange = false;

                binding.rvSuggestions.setVisibility(View.GONE);
                hideKeyboard(binding.getRoot());
                updateMarkerPositions();
                checkAndShowServices();
            }
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestions.setAdapter(suggestionAdapter);
    }

    private void setupSearchInput() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isProgrammaticChange) return;
                if (s.length() > 2) fetchSuggestions(s.toString());
                else binding.rvSuggestions.setVisibility(View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etOrigin.addTextChangedListener(textWatcher);
        binding.etDestination.addTextChangedListener(textWatcher);

        binding.etOrigin.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isSearchingOrigin = true;
                if (binding.etOrigin.getText().length() > 2) binding.rvSuggestions.setVisibility(View.VISIBLE);
            }
        });

        binding.etDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isSearchingOrigin = false;
                if (binding.etDestination.getText().length() > 2) binding.rvSuggestions.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fetchSuggestions(String query) {
        String token = getString(R.string.mapbox_access_token);
        MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
                .accessToken(token)
                .query(query)
                .country("VN")
                .autocomplete(true)
                .limit(5)
                .build();

        mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().features().isEmpty()) {
                    suggestionAdapter.setSuggestions(response.body().features());
                    binding.rvSuggestions.setVisibility(View.VISIBLE);
                } else {
                    binding.rvSuggestions.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                binding.rvSuggestions.setVisibility(View.GONE);
            }
        });
    }

    private void checkAndShowServices() {
        if (originPoint != null && destinationPoint != null) {
            getRoute(originPoint, destinationPoint);
        } else if (originPoint != null || destinationPoint != null) {
            // Nếu mới chỉ chọn 1 trong 2 điểm, di chuyển camera tới điểm đó
            moveCameraToPoint(originPoint != null ? originPoint : destinationPoint);
        }
    }

    private void getRoute(Point origin, Point destination) {
        String coordinates = String.format(Locale.US, "%f,%f;%f,%f", 
                origin.longitude(), origin.latitude(), 
                destination.longitude(), destination.latitude());
        
        String token = getString(R.string.mapbox_access_token);
        
        mapboxService.getDirections("driving", coordinates, token, "polyline6", "full")
                .enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.has("routes") && body.getAsJsonArray("routes").size() > 0) {
                        JsonObject route = body.getAsJsonArray("routes").get(0).getAsJsonObject();
                        String geometry = route.get("geometry").getAsString();
                        double distance = route.get("distance").getAsDouble();
                        
                        drawRoute(geometry);
                        updatePrices(distance);
                        updateMarkerPositions();
                        
                        binding.layoutServiceContent.setVisibility(View.VISIBLE);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        updateServiceSelectionUI();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("BookingActivity", "Error: " + t.getMessage());
                Toast.makeText(BookingActivity.this, "Không thể tìm đường đi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawRoute(String geometry) {
        binding.mapView.getMapboxMap().getStyle(style -> {
            GeoJsonSource source = (GeoJsonSource) SourceUtils.getSource(style, ROUTE_SOURCE_ID);
            if (source != null) {
                LineString lineString = LineString.fromPolyline(geometry, 6);
                source.geometry(lineString);
                
                CameraOptions cameraOptions = binding.mapView.getMapboxMap().cameraForGeometry(
                        lineString,
                        new EdgeInsets(100.0, 100.0, 300.0, 100.0),
                        null, null
                );
                CameraAnimationsUtils.flyTo(binding.mapView.getMapboxMap(), cameraOptions, null, null);
            }
        });
    }

    private void updatePrices(double distanceInMeters) {
        double distanceInKm = distanceInMeters / 1000.0;
        long bikePrice = Math.round(distanceInKm * pricePerKmBike);
        long carPrice = Math.round(distanceInKm * pricePerKmCar);

        if (bikePrice < 10000) bikePrice = 10000;
        if (carPrice < 25000) carPrice = 25000;

        binding.tvBikePrice.setText(String.format(Locale.getDefault(), "%,dđ", bikePrice));
        binding.tvCarPrice.setText(String.format(Locale.getDefault(), "%,dđ", carPrice));
        binding.tvBikeInfo.setText(String.format(Locale.getDefault(), "Tiết kiệm • %.1f km", distanceInKm));
        binding.tvCarInfo.setText(String.format(Locale.getDefault(), "Thoải mái • %.1f km", distanceInKm));
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSwap.setOnClickListener(v -> {
            isProgrammaticChange = true;
            String tempText = binding.etOrigin.getText().toString();
            binding.etOrigin.setText(binding.etDestination.getText().toString());
            binding.etDestination.setText(tempText);
            isProgrammaticChange = false;
            
            Point tempPoint = originPoint;
            originPoint = destinationPoint;
            destinationPoint = tempPoint;

            updateMarkerPositions();
            if (originPoint != null && destinationPoint != null) getRoute(originPoint, destinationPoint);
        });

        binding.optionBike.setOnClickListener(v -> { isBikeSelected = true; updateServiceSelectionUI(); });
        binding.optionCar.setOnClickListener(v -> { isBikeSelected = false; updateServiceSelectionUI(); });
        binding.btnConfirm.setOnClickListener(v -> {
            String service = isBikeSelected ? "BragBike" : "BragCar";
            Toast.makeText(this, "Đang đặt " + service, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateServiceSelectionUI() {
        int activeColor = ContextCompat.getColor(this, R.color.primary_light);
        int inactiveColor = ContextCompat.getColor(this, R.color.white);
        binding.optionBike.setCardBackgroundColor(isBikeSelected ? activeColor : inactiveColor);
        binding.optionBike.setStrokeWidth(isBikeSelected ? 2 : 0);
        binding.optionCar.setCardBackgroundColor(!isBikeSelected ? activeColor : inactiveColor);
        binding.optionCar.setStrokeWidth(!isBikeSelected ? 2 : 0);
        
        String service = isBikeSelected ? "BragBike" : "BragCar";
        binding.btnConfirm.setText(String.format("Đặt %s", service));
    }

    private void moveCameraToPoint(Point point) {
        CameraOptions options = new CameraOptions.Builder().center(point).zoom(15.0).build();
        CameraAnimationsUtils.flyTo(binding.mapView.getMapboxMap(), options, null, null);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void checkPermissionAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    originPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
                    isProgrammaticChange = true;
                    binding.etOrigin.setText("Vị trí hiện tại");
                    isProgrammaticChange = false;
                    moveCameraToPoint(originPoint);
                    updateMarkerPositions();
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }
}
