//package com.example.bragbike;
//
//import android.os.Bundle;
//import androidx.appcompat.app.AppCompatActivity;
//import com.mapbox.geojson.Point;
//import com.mapbox.maps.CameraOptions;
//import com.mapbox.maps.MapView;
//import com.mapbox.maps.MapboxMap;
//import com.mapbox.maps.plugin.Plugin;
//import com.mapbox.maps.plugin.compass.CompassPlugin;
//import com.mapbox.maps.plugin.scalebar.ScaleBarPlugin;
//
//public class Search extends AppCompatActivity {
//
//    private MapView mapView;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        // Thiết lập layout
//        setContentView(R.layout.activity_search);
//
//        mapView = findViewById(R.id.mapView);
//        if (mapView != null) {
//            MapboxMap mapboxMap = mapView.getMapboxMap();
//
//            // Cấu hình Camera tương đương với mapViewportState trong Compose
//            CameraOptions cameraOptions = new CameraOptions.Builder()
//                    .center(Point.fromLngLat(-98.0, 39.5))
//                    .zoom(2.0)
//                    .build();
//            mapboxMap.setCamera(cameraOptions);
//
//            // Vô hiệu hóa ScaleBar và Compass (nếu chưa chỉnh trong XML)
//            ScaleBarPlugin scaleBarPlugin = mapView.getPlugin(Plugin.MAPBOX_SCALEBAR_PLUGIN_ID);
//            if (scaleBarPlugin != null) {
//                scaleBarPlugin.setEnabled(false);
//            }
//
//            CompassPlugin compassPlugin = mapView.getPlugin(Plugin.MAPBOX_COMPASS_PLUGIN_ID);
//            if (compassPlugin != null) {
//                compassPlugin.setEnabled(false);
//            }
//        }
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (mapView != null) mapView.onStart();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (mapView != null) mapView.onStop();
//    }
//
//    @Override
//    public void onLowMemory() {
//        super.onLowMemory();
//        if (mapView != null) mapView.onLowMemory();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (mapView != null) mapView.onDestroy();
//    }
//}