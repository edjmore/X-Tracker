package com.droid.mooresoft.x_tracker;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.InputMismatchException;

/**
 * Created by Ed on 6/11/15.
 */
public class MapActivity extends ActionBarActivity implements OnMapReadyCallback {

    private MapFragment mMapFragment;
    private GroundOverlay mOverlay;

    private long mRouteId = -1;

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        ArrayList<Location> locations = fetchRouteLocations();
        drawRoute(googleMap, locations);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);

        // get route ID
        mRouteId = getIntent().getLongExtra("id", -1);

        // load map
        mMapFragment = MapFragment.newInstance();
        mMapFragment.getMapAsync(this);
        getFragmentManager().beginTransaction().add(R.id.map_container, mMapFragment).commit();
    }

    private ArrayList<Location> fetchRouteLocations() {
        if (mRouteId == -1) {
            return getMockLocationHistory();
        } else {
            ArrayList<Location> locations = null;
            DataSource dataSrc = new DataSource(this);
            try {
                dataSrc.open();
                locations = dataSrc.fetchLocationData(mRouteId);
            } catch (SQLiteException sqle) {
                sqle.printStackTrace();
            } finally {
                dataSrc.close();
            }
            if (locations == null) {
                return getMockLocationHistory();
            } else {
                return locations;
            }
        }
    }

    private void drawRoute(final GoogleMap googleMap, final ArrayList<Location> locations) {
        // temporarily disable user interation until all drawing is complete
        final UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setAllGesturesEnabled(false);
        // remove the current overlay
        if (mOverlay != null) {
            mOverlay.remove();
        }

        // want to zoom in on the area in which the route will be completely visible
        LatLngBounds.Builder builder = LatLngBounds.builder();
        float ms = 0; // max speed
        for (Location l : locations) {
            LatLng latLng = new LatLng(l.getLatitude(), l.getLongitude());
            builder.include(latLng);

            ms = Math.max(ms, l.getSpeed());
        }
        final float maxSpeed = ms; // will need this later
        final LatLngBounds bounds = builder.build();
        // padding is one tenth of map view width
        int padding = mMapFragment.getView().getWidth() / 10;

        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding),
                new GoogleMap.CancelableCallback() { // need to wait for zoom before we draw the route
                    @Override
                    public void onFinish() {
                        // want the overlay to cover all locations
                        Projection projection = googleMap.getProjection();
                        Point upperRight = projection.toScreenLocation(bounds.northeast),
                                lowerLeft = projection.toScreenLocation(bounds.southwest);
                        int width = upperRight.x - lowerLeft.x,
                                height = lowerLeft.y - upperRight.y; // pixels

                        final Bitmap bitmap = Bitmap.createBitmap(width + 1, height + 1, Bitmap.Config.ARGB_8888);
                        final Canvas canvas = new Canvas(bitmap);
                        Point origin = new Point(lowerLeft.x, upperRight.y); // upper left of canvas

                        // gradient paint
                        final Paint paint = new Paint();
                        paint.setStrokeWidth(24);
                        paint.setAntiAlias(true);
                        paint.setDither(true);

                        // use max speed to determine color scale
                        ColorScale colorScale = new ColorScale(maxSpeed);
                        for (int i = 0; i < locations.size(); i++) {
                            Location l0 = locations.get(i);

                            if (i + 1 < locations.size()) { // can't draw a line if there's no next point
                                Location l1 = locations.get(i + 1);
                                Point p0 = getCanvasPoint(l0, origin, projection),
                                        p1 = getCanvasPoint(l1, origin, projection);
                                int color0 = colorScale.getColor(l0.getSpeed()),
                                        color1 = colorScale.getColor(l1.getSpeed());
                                // gradient shader
                                LinearGradient shader = new LinearGradient(p0.x, p0.y, p1.x, p1.y,
                                        color0, color1, Shader.TileMode.CLAMP);
                                paint.setShader(shader);

                                // draw a line between the two points
                                canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint);
                            }

                            // mark the current point
                            MarkerOptions options = new MarkerOptions();
                            options.position(new LatLng(l0.getLatitude(), l0.getLongitude()));
                            options.title(l0.getSpeed() + " m/s"); // show speed on click
                            options.icon(BitmapDescriptorFactory.fromResource(
                                    R.drawable.abc_btn_rating_star_on_mtrl_alpha)); // TODO: change icon
                            googleMap.addMarker(options);
                        }

                        // add the overlay to the map
                        GroundOverlayOptions options = new GroundOverlayOptions();
                        options.positionFromBounds(bounds);
                        options.image(BitmapDescriptorFactory.fromBitmap(bitmap));
                        // keep a reference to the overlay so we can edit or remove it later
                        mOverlay = googleMap.addGroundOverlay(options);

                        // enable map interaction again
                        uiSettings.setAllGesturesEnabled(true);
                    }

                    @Override
                    public void onCancel() {
                        // TODO: retry the camera update
                    }
                });
    }

    private ArrayList<Location> getMockLocationHistory() {
        ArrayList<Location> locationHistory = new ArrayList<>();
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location l0 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        l0.setSpeed(0);
        Location l1 = new Location(l0);
        l1.setLongitude(l0.getLongitude() + 0.01);
        l1.setSpeed(2);
        Location l2 = new Location(l1);
        l2.setLatitude(l1.getLatitude() + 0.02);
        l2.setSpeed(8);
        Location l3 = new Location(l2);
        l3.setLongitude(l2.getLongitude() - 0.01);
        l3.setSpeed(1);
        Location l4 = new Location(l3);
        l4.setLongitude(l3.getLongitude() - 0.03);
        l4.setLatitude(l3.getLatitude() - 0.01);
        l4.setSpeed(3);
        Location l5 = new Location(l4);
        l5.setLatitude(l4.getLatitude() - 0.01);
        l5.setLongitude(l4.getLongitude() - 0.02);
        l5.setSpeed(5);
        locationHistory.add(l0);
        locationHistory.add(l1);
        locationHistory.add(l2);
        locationHistory.add(l3);
        locationHistory.add(l4);
        locationHistory.add(l5);
        return locationHistory;
    }

    private class ColorScale {

        float mMaxSpeed;

        ColorScale(float maxSpeed) {
            mMaxSpeed = maxSpeed;
        }

        int getColor(float speed) {
            float r = ((mMaxSpeed - speed) / mMaxSpeed) * 255, // slower speeds are red...
                    g = (speed / mMaxSpeed) * 255, // ...and faster speeds are more green
                    b = 0;
            return Color.argb(222, (int) r, (int) g, (int) b);
        }
    }

    private Point getCanvasPoint(Location location, Point canvasOrigin, Projection projection) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        Point screenPoint = projection.toScreenLocation(latLng),
                canvasPoint = new Point();
        canvasPoint.x = screenPoint.x - canvasOrigin.x;
        canvasPoint.y = screenPoint.y - canvasOrigin.y;
        return canvasPoint;
    }
}
