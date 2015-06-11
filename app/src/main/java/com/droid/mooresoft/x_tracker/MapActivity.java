package com.droid.mooresoft.x_tracker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Ed on 6/10/15.
 */
public class MapActivity extends Activity {

    public static final String LAT = "lat", LNG = "lng";

    private LatLng mLatLng;
    private MapFragment mMapFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);
        init();
    }

    private final OnMapReadyCallback initMap = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            float zoom = 14, tilt = 0, bearing = 0;
            CameraPosition initialPosition = new CameraPosition(mLatLng, zoom, tilt, bearing);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(initialPosition);
            googleMap.animateCamera(cameraUpdate);
        }
    };

    private void init() {
        // location to center map camera upon
        Intent intent = getIntent();
        double lat = intent.getDoubleExtra(LAT, 0);
        double lng = intent.getDoubleExtra(LNG, 0);
        mLatLng = new LatLng(lat, lng);
        // fetch map
        mMapFragment = MapFragment.newInstance();
        mMapFragment.getMapAsync(initMap);
        getFragmentManager().beginTransaction().add(R.id.map, mMapFragment).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        doBindService();
    }

    private GeoTrackingService mBoundService;
    private boolean mIsBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundService = ((GeoTrackingService.CustomBinder) service).getService();
            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            doUnbindService();
        }
    };

    private void doBindService() {
        bindService(new Intent(this, GeoTrackingService.class), mConnection, BIND_ABOVE_CLIENT);
    }

    private void doUnbindService() {
        if (mIsBound) {
            unbindService(mConnection);
            mBoundService = null;
            mIsBound = false;
        }
    }

    public void drawHistory(View v) {
        final GoogleMap map = mMapFragment.getMap();
        map.clear();
        final CircleOptions options = new CircleOptions();
        options.fillColor(android.R.color.black);
        options.radius(3);
        if (mBoundService != null) {
            for (Location location : mBoundService.getLocationHistory()) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                options.center(latLng);
                map.addCircle(options);
            }
        }
    }

    @Override
    public void onDestroy() {
        doUnbindService();
        super.onDestroy();
    }
}
