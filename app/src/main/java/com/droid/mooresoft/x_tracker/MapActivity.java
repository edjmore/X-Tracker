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
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location l0 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        l0.setSpeed(0);
        Location l1 = new Location(l0);
        l1.setLongitude(l0.getLongitude() + 0.01);
        l1.setSpeed(2);
        Location l2 = new Location(l1);
        l2.setLatitude(l1.getLatitude() + 0.01);
        l2.setSpeed(8);
        Location l3 = new Location(l2);
        l3.setLongitude(l2.getLongitude() - 0.01);
        l3.setSpeed(1);
        mLocationHistory.add(l0);
        mLocationHistory.add(l1);
        mLocationHistory.add(l2);
        mLocationHistory.add(l3);
    }

    private MapFragment mMapFragment;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLngBounds bounds = latLngBounds(mLocationHistory);
        LatLng target = center(bounds);
        float zoom = 14, tilt = 0, bearing = 0;
        CameraPosition center = new CameraPosition(target, zoom, tilt, bearing);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(center);
        googleMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);

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
        map.clear(); // clear current map
        Projection mapProjection = map.getProjection();
        Rect bitmapBox = graphicsPerimeter(bounds, mapProjection); // dimensions for canvas

        final Bitmap bitmap = Bitmap.createBitmap(bitmapBox.width(), bitmapBox.height(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawDebugGraphics(canvas);
        // translation between geographic distances and pixels
        OverlayProjection overlayProjection = new OverlayProjection(bounds, mapProjection);

        final Paint gradientPaint = new Paint();
        gradientPaint.setStrokeWidth(10);
        gradientPaint.setAntiAlias(true);
        gradientPaint.setDither(true);
        // pick gradient color based on speed at each point
        float maxSpeed = maxSpeed(locations);
        ColorPicker colorPicker = new ColorPicker(maxSpeed);

        for (int i = 0; i < locations.size(); i++) {
            // get pair of consecutive locations
            Location l0 = locations.get(i);
            Point p0 = overlayProjection.toScreenPoint(l0);
            if (i + 1 < locations.size()) {
                Location l1 = locations.get(i + 1);
                Point p1 = overlayProjection.toScreenPoint(l1);

                // build shader
                int color0 = colorPicker.color(l0.getSpeed());
                int color1 = colorPicker.color(l1.getSpeed());
                LinearGradient shader = new LinearGradient(p0.x, p0.y, p1.x, p1.y, color0, color1,
                        Shader.TileMode.CLAMP);

                gradientPaint.setShader(shader);
                // draw connecting line
                canvas.drawLine(p0.x, p0.y, p1.x, p1.y, gradientPaint);
            }
            // mark this location with a circle
            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(new LatLng(l0.getLatitude(), l0.getLongitude()));
            circleOptions.radius(16);
            circleOptions.fillColor(0xff000000); // solid black
            map.addCircle(circleOptions);
        }

        // create overlay from bitmap we drew on
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

    private float maxSpeed(ArrayList<Location> locations) {
        float maxSpeed = 0;
        for (Location l : locations) {
            maxSpeed = Math.max(maxSpeed, l.getSpeed());
        }
        return maxSpeed;
    }

    private class OverlayProjection {

        double pixelsPerMeter;
        Location northWest; // northwest corner of ground overlay

        OverlayProjection(LatLngBounds bounds, Projection mapProjection) {
            // get the northwest corner location
            northWest = new Location("dummy_provider");
            northWest.setLatitude(bounds.northeast.latitude);
            northWest.setLongitude(bounds.southwest.longitude);
            // create two arbitrary locations
            LatLng ll0 = new LatLng(0, -1), ll1 = new LatLng(0, 1);
            Point p0 = mapProjection.toScreenLocation(ll0), p1 = mapProjection.toScreenLocation(ll1);
            double dPixels = p1.x - p0.x;
            Location l0 = new Location("dummy_provider");
            l0.setLatitude(ll0.latitude);
            l0.setLongitude(ll0.longitude);
            Location l1 = new Location("dummy_provider");
            l1.setLatitude(ll1.latitude);
            l1.setLongitude(ll1.longitude);
            double dMeters = l0.distanceTo(l1);
            // scale between geographic distance and screen pixels
            pixelsPerMeter = dPixels / dMeters;
        }

        Point toScreenPoint(Location location) {
            // create locations which differ from the northwest point in only one dimension
            Location dLat = new Location(location);
            dLat.setLongitude(northWest.getLongitude());
            Location dLng = new Location(location);
            dLng.setLatitude(northWest.getLatitude());
            // get distances in meters
            double dLatMeters = northWest.distanceTo(dLat);
            double dLngMeters = northWest.distanceTo(dLng);
            // convert to pixels
            double dxPixels = pixelsPerMeter * dLngMeters;
            double dyPixels = pixelsPerMeter * dLatMeters;
            return new Point((int) dxPixels, (int) dyPixels);
        }
    }

    private class ColorPicker {

        float maxSpeed;

        ColorPicker(float maxSpeed) {
            this.maxSpeed = maxSpeed;
        }

        int color(float speed) {
            float r = ((maxSpeed - speed) / maxSpeed) * 255; // get less red at higher speeds
            float g = (speed / maxSpeed) * 255; // more green at higher speeds
            float b = 0;
            float mostlyOpaque = 222;
            return Color.argb((int) mostlyOpaque, (int) r, (int) g, (int) b);
        }
    }

    private void drawDebugGraphics(final Canvas canvas) {
        canvas.drawColor(0x770000ff); // semi-transparent blue
    }
}