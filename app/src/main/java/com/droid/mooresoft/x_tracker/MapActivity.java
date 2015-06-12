package com.droid.mooresoft.x_tracker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

/**
 * Created by Ed on 6/11/15.
 */
public class MapActivity extends ActionBarActivity implements OnMapReadyCallback {

    private ArrayList<Location> mLocationHistory = new ArrayList<>();

    private void initDebugLocationHistory() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location l1 = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        l1.setSpeed(5);
        double lat = l1.getLatitude(), lng = l1.getLongitude();
        Location l2 = new Location(l1), l3 = new Location(l1), l4 = new Location(l1);
        l2.setLatitude(lat + 0.01);
        l2.setLongitude(lng + 0.01);
        l2.setSpeed(0);
        l3.setLatitude(lat);
        l3.setLongitude(lng - 0.01);
        l3.setSpeed(10);
        l4.setLatitude(lat + 0.01);
        l4.setLongitude(lng - 0.01);
        l4.setSpeed(15);
        mLocationHistory.add(l1);
        mLocationHistory.add(l2);
        mLocationHistory.add(l3);
        mLocationHistory.add(l4);
    }

    private MapFragment mMapFragment;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng target = new LatLng(mLocationHistory.get(0).getLatitude(),
                mLocationHistory.get(0).getLongitude());
        float zoom = 14, tilt = 0, bearing = 0;
        CameraPosition center = new CameraPosition(target, zoom, tilt, bearing);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(center);
        googleMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);

        // TODO: remove
        initDebugLocationHistory();

        mMapFragment = MapFragment.newInstance();
        mMapFragment.getMapAsync(this);
        getFragmentManager().beginTransaction().add(R.id.map_container, mMapFragment).commit();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.draw_hist:
                drawRoute(mLocationHistory);
                break;
        }
    }

    private void drawRoute(ArrayList<Location> locations) {
        LatLngBounds bounds = latLngBounds(locations); // geographic perimeter of route
        GoogleMap map = mMapFragment.getMap();
        Projection projection = map.getProjection();
        Rect bitmapBox = graphicsPerimeter(bounds, projection); // dimensions for canvas

        final Bitmap bitmap = Bitmap.createBitmap(bitmapBox.width(), bitmapBox.height(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawDebugGraphics(canvas, bitmapBox);

        GroundOverlayOptions overlayOptions = new GroundOverlayOptions();
        overlayOptions.image(BitmapDescriptorFactory.fromBitmap(bitmap));
        LatLng center = center(bounds);
        Pair<Float, Float> dimensions = overlayDimensions(bounds);
        overlayOptions.position(center, dimensions.first, dimensions.second);
        map.addGroundOverlay(overlayOptions);
    }

    private LatLngBounds latLngBounds(ArrayList<Location> locations) {
        double minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
        for (Location l : locations) {
            double currLat = l.getLatitude();
            minLat = Math.min(minLat, currLat);
            maxLat = Math.max(maxLat, currLat);
            double currLng = l.getLongitude();
            minLng = Math.min(minLng, currLng);
            maxLng = Math.max(maxLng, currLng);
        }
        LatLng southwest = new LatLng(minLat, minLng);
        LatLng northeast = new LatLng(maxLat, maxLng);
        return new LatLngBounds(southwest, northeast);
    }

    private LatLng center(LatLngBounds bounds) {
        double lat = (bounds.southwest.latitude + bounds.northeast.latitude) / 2;
        double lng = (bounds.southwest.longitude + bounds.northeast.longitude) / 2;
        return new LatLng(lat, lng);
    }

    private Rect graphicsPerimeter(LatLngBounds bounds, Projection projection) {
        Point upperRight = projection.toScreenLocation(bounds.northeast);
        Point lowerLeft = projection.toScreenLocation(bounds.southwest);
        return new Rect(lowerLeft.x, upperRight.y, upperRight.x, lowerLeft.y);
    }

    private Pair<Float, Float> overlayDimensions(LatLngBounds bounds) {
        // empty locations
        Location west = new Location("dummy_provider"), north = new Location("dummy_provider"),
                south = new Location("dummy_provider"), east = new Location("dummy_provider");
        west.setLongitude(bounds.southwest.longitude);
        north.setLatitude(bounds.northeast.latitude);
        south.setLatitude(bounds.southwest.latitude);
        east.setLongitude(bounds.northeast.longitude);
        float width = west.distanceTo(east);
        float height = north.distanceTo(south);
        return new Pair<>(width, height);
    }

    private void drawDebugGraphics(final Canvas canvas, Rect perimeter) {
        canvas.drawColor(0x770000ff); // semi-transparent blue
    }
}