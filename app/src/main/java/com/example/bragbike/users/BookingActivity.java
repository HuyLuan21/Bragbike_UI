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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bragbike.R;
import com.example.bragbike.api.ApiService;
import com.example.bragbike.api.MapboxService;
import com.example.bragbike.api.RetrofitClient;
import com.example.bragbike.databinding.ActivityBookingBinding;
import com.example.bragbike.model.PeakHour;
import com.example.bragbike.model.VehiclePricing;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.JsonObject;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
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
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BookingActivity extends AppCompatActivity {

    private ActivityBookingBinding binding;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    
    private static final String ROUTE_SOURCE_ID = "route-source-id";
    private static final String ROUTE_LAYER_ID = "route-layer-id";
    private static final String ORIGIN_SOURCE_ID = "origin-source-id";
    private static final String DEST_SOURCE_ID = "dest-source-id";
    private static final String ORIGIN_LAYER_ID = "origin-layer-id";
    private static final String DEST_LAYER_ID = "dest-layer-id";

    private FusedLocationProviderClient fusedLocationClient;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private SuggestionAdapter suggestionAdapter;
    private MapboxService mapboxService;
    private ApiService apiService;

    private Point originPoint;
    private Point destinationPoint;
    private boolean isBikeSelected = true;
    private boolean isSearchingOrigin = true;
    private boolean isProgrammaticChange = false;

    private List<PeakHour> peakHoursList = new ArrayList<>();
    private VehiclePricing bikePricing;
    private VehiclePricing carPricing;

    private double currentDistanceKm = 0;
    private double currentFare = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        loadPricingConfigs();
        initMapboxRetrofit();
        initMap();
        setupBottomSheet();
        setupRecyclerView();
        setupClickListeners();
        setupSearchInput();
    }

    private void loadPricingConfigs() {
        apiService.getPeakHours().enqueue(new Callback<List<PeakHour>>() {
            @Override
            public void onResponse(Call<List<PeakHour>> call, Response<List<PeakHour>> response) {
                if (response.isSuccessful() && response.body() != null) peakHoursList = response.body();
            }
            @Override
            public void onFailure(Call<List<PeakHour>> call, Throwable t) {}
        });

        apiService.getPricingByType("MOTORBIKE").enqueue(new Callback<VehiclePricing>() {
            @Override
            public void onResponse(Call<VehiclePricing> call, Response<VehiclePricing> response) {
                if (response.isSuccessful() && response.body() != null) bikePricing = response.body();
            }
            @Override
            public void onFailure(Call<VehiclePricing> call, Throwable t) {}
        });

        apiService.getPricingByType("CAR_4").enqueue(new Callback<VehiclePricing>() {
            @Override
            public void onResponse(Call<VehiclePricing> call, Response<VehiclePricing> response) {
                if (response.isSuccessful() && response.body() != null) carPricing = response.body();
            }
            @Override
            public void onFailure(Call<VehiclePricing> call, Throwable t) {}
        });
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(0);
        binding.layoutServiceContent.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        suggestionAdapter = new SuggestionAdapter(feature -> {
            selectLocation(feature);
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestions.setAdapter(suggestionAdapter);
    }

    private void selectLocation(CarmenFeature feature) {
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
            
            if (originPoint != null && destinationPoint != null) {
                getRoute(originPoint, destinationPoint);
            } else {
                moveCameraToPoint(point);
            }
        }
    }

    private void setupSearchInput() {
        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isProgrammaticChange && s.length() > 2) fetchSuggestions(s.toString());
                else if (s.length() <= 2) binding.rvSuggestions.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        binding.etOrigin.addTextChangedListener(tw);
        binding.etDestination.addTextChangedListener(tw);

        binding.etDestination.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                if (suggestionAdapter.getItemCount() > 0) {
                    selectLocation(suggestionAdapter.getFirstFeature());
                }
                return true;
            }
            return false;
        });

        binding.etOrigin.setOnFocusChangeListener((v, h) -> { if(h) isSearchingOrigin = true; });
        binding.etDestination.setOnFocusChangeListener((v, h) -> { if(h) isSearchingOrigin = false; });
    }

    private void fetchSuggestions(String query) {
        String token = getString(R.string.mapbox_access_token);
        MapboxGeocoding.builder()
                .accessToken(token)
                .query(query)
                .country("VN")
                .autocomplete(true)
                .limit(5)
                .build()
                .enqueueCall(new retrofit2.Callback<GeocodingResponse>() {
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
                        drawRoute(route.get("geometry").getAsString());
                        
                        currentDistanceKm = route.get("distance").getAsDouble() / 1000.0;
                        updatePrices(currentDistanceKm);
                        
                        binding.layoutServiceContent.setVisibility(View.VISIBLE);
                        bottomSheetBehavior.setPeekHeight(800); 
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        updateServiceSelectionUI();
                    }
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private void drawRoute(String geometry) {
        binding.mapView.getMapboxMap().getStyle(style -> {
            GeoJsonSource source = (GeoJsonSource) SourceUtils.getSource(style, ROUTE_SOURCE_ID);
            if (source != null) {
                LineString lineString = LineString.fromPolyline(geometry, 6);
                source.geometry(lineString);
                
                CameraOptions options = binding.mapView.getMapboxMap().cameraForGeometry(
                        lineString, 
                        new EdgeInsets(150.0, 100.0, 900.0, 100.0),
                        null, null);
                CameraAnimationsUtils.flyTo(binding.mapView.getMapboxMap(), options, null, null);
            }
        });
    }

    private void updatePrices(double distanceInKm) {
        double bikePrice = calculateFareOffline(bikePricing, distanceInKm);
        double carPrice = calculateFareOffline(carPricing, distanceInKm);
        binding.tvBikePrice.setText(String.format(Locale.getDefault(), "%,.0fđ", bikePrice));
        binding.tvCarPrice.setText(String.format(Locale.getDefault(), "%,.0fđ", carPrice));
        binding.tvBikeInfo.setText(String.format(Locale.getDefault(), "Xe máy • %.1f km", distanceInKm));
        binding.tvCarInfo.setText(String.format(Locale.getDefault(), "Ô tô 4 chỗ • %.1f km", distanceInKm));
        updateCurrentFare();
    }

    private double calculateFareOffline(VehiclePricing pricing, double distanceKm) {
        if (pricing == null) return 0;
        double total = pricing.getBaseFare() + (distanceKm * pricing.getPricePerKm());
        return Math.max(Math.floor(total / 1000) * 1000, pricing.getMinFare());
    }

    private void updateCurrentFare() {
        currentFare = isBikeSelected ? 
                calculateFareOffline(bikePricing, currentDistanceKm) : 
                calculateFareOffline(carPricing, currentDistanceKm);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.optionBike.setOnClickListener(v -> { isBikeSelected = true; updateCurrentFare(); updateServiceSelectionUI(); });
        binding.optionCar.setOnClickListener(v -> { isBikeSelected = false; updateCurrentFare(); updateServiceSelectionUI(); });
        binding.btnConfirm.setOnClickListener(v -> createRideOnServer());
    }

    private void createRideOnServer() {
        if (originPoint == null || destinationPoint == null) {
            Toast.makeText(this, "Thiếu thông tin điểm đón/đến", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnConfirm.setEnabled(false);
        binding.btnConfirm.setText("Đang đặt...");

        Map<String, Object> body = new HashMap<>();
        body.put("vehicle_type", isBikeSelected ? "MOTORBIKE" : "CAR_4");
        body.put("pickup_address", binding.etOrigin.getText().toString());
        body.put("pickup_lat", originPoint.latitude());
        body.put("pickup_lng", originPoint.longitude());
        body.put("drop_address", binding.etDestination.getText().toString());
        body.put("drop_lat", destinationPoint.latitude());
        body.put("drop_lng", destinationPoint.longitude());
        body.put("distance_km", currentDistanceKm);
        body.put("total_price", currentFare);

        apiService.createRide(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BookingActivity.this, "Đặt thành công!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    binding.btnConfirm.setEnabled(true);
                    updateServiceSelectionUI();
                    Toast.makeText(BookingActivity.this, "Lỗi: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                binding.btnConfirm.setEnabled(true);
                updateServiceSelectionUI();
                Toast.makeText(BookingActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateServiceSelectionUI() {
        int activeColor = ContextCompat.getColor(this, R.color.primary_light);
        int inactiveColor = ContextCompat.getColor(this, R.color.white);
        binding.optionBike.setCardBackgroundColor(isBikeSelected ? activeColor : inactiveColor);
        binding.optionBike.setStrokeWidth(isBikeSelected ? 2 : 0);
        binding.optionCar.setCardBackgroundColor(!isBikeSelected ? activeColor : inactiveColor);
        binding.optionCar.setStrokeWidth(!isBikeSelected ? 2 : 0);
        binding.btnConfirm.setText(isBikeSelected ? "Đặt BragBike" : "Đặt BragCar");
    }

    private void initMapboxRetrofit() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.mapbox.com/").addConverterFactory(GsonConverterFactory.create()).build();
        mapboxService = retrofit.create(MapboxService.class);
    }

    private void initMap() {
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            LocationComponentUtils.getLocationComponent(binding.mapView).setEnabled(true);
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
        routeLayer.lineWidth(6.0);
        LayerUtils.addLayer(style, routeLayer);
    }

    private void initMarkerLayers(Style style) {
        SourceUtils.addSource(style, new GeoJsonSource.Builder(ORIGIN_SOURCE_ID).build());
        LayerUtils.addLayer(style, new CircleLayer(ORIGIN_LAYER_ID, ORIGIN_SOURCE_ID).circleColor(Color.parseColor("#4285F4")).circleRadius(8.0));
        SourceUtils.addSource(style, new GeoJsonSource.Builder(DEST_SOURCE_ID).build());
        LayerUtils.addLayer(style, new CircleLayer(DEST_LAYER_ID, DEST_SOURCE_ID).circleColor(Color.parseColor("#F4B400")).circleRadius(8.0));
    }

    private void updateMarkerPositions() {
        binding.mapView.getMapboxMap().getStyle(style -> {
            GeoJsonSource o = (GeoJsonSource) SourceUtils.getSource(style, ORIGIN_SOURCE_ID);
            if (o != null && originPoint != null) o.geometry(originPoint);
            GeoJsonSource d = (GeoJsonSource) SourceUtils.getSource(style, DEST_SOURCE_ID);
            if (d != null && destinationPoint != null) d.geometry(destinationPoint);
        });
    }

    private void moveCameraToPoint(Point point) {
        CameraAnimationsUtils.flyTo(binding.mapView.getMapboxMap(), new CameraOptions.Builder().center(point).zoom(14.0).build(), null, null);
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
                    
                    String token = getString(R.string.mapbox_access_token);
                    MapboxGeocoding reverseGeocode = MapboxGeocoding.builder()
                            .accessToken(token)
                            .query(originPoint)
                            .geocodingTypes("address")
                            .build();

                    reverseGeocode.enqueueCall(new retrofit2.Callback<GeocodingResponse>() {
                        @Override
                        public void onResponse(retrofit2.Call<GeocodingResponse> call, retrofit2.Response<GeocodingResponse> response) {
                            if (response.isSuccessful() && response.body() != null && !response.body().features().isEmpty()) {
                                CarmenFeature feature = response.body().features().get(0);
                                isProgrammaticChange = true;
                                binding.etOrigin.setText(feature.placeName());
                                isProgrammaticChange = false;
                            } else {
                                isProgrammaticChange = true;
                                binding.etOrigin.setText("Vị trí hiện tại");
                                isProgrammaticChange = false;
                            }
                        }
                        @Override
                        public void onFailure(retrofit2.Call<GeocodingResponse> call, Throwable t) {
                            isProgrammaticChange = true;
                            binding.etOrigin.setText("Vị trí hiện tại");
                            isProgrammaticChange = false;
                        }
                    });

                    moveCameraToPoint(originPoint);
                    updateMarkerPositions();
                }
            });
        }
    }
}
