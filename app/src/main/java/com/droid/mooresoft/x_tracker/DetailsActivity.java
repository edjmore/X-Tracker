package com.droid.mooresoft.x_tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;

/**
 * Created by Ed on 6/11/15.
 */
public class DetailsActivity extends ActionBarActivity implements OnMapReadyCallback {

    private MapFragment mMapFragment;
    private ArrayList<Location> mLocations;
    private GroundOverlay mOverlay;
    private long mRouteId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details_activity);

        init();

        // load the map
        mMapFragment = MapFragment.newInstance();
        mMapFragment.getMapAsync(this);
        getFragmentManager().beginTransaction().add(R.id.map_container, mMapFragment).commit();

        // get data from intent and populate views
        Intent details = getIntent();
        float meters = details.getFloatExtra(DatabaseHelper.ROUTE_DISTANCE, 0);
        // convert to appropriate units and format
        String units = mPrefs.getString(getString(R.string.key_preference_units), "miles");
        float uDist;
        if (units.equals("miles")) {
            uDist = Utils.metersToMiles(meters);
        } else {
            uDist = Utils.metersToKilometers(meters);
        }
        mDistView.setText(Utils.toFormatedDistance(uDist));
        mUnitsView.setText(units);
        mTimeView.setText(details.getStringExtra(DatabaseHelper.ROUTE_ELAPSED_TIME));
    }

    private TextView mDistView, mTimeView, mCalView, mUnitsView;
    private SharedPreferences mPrefs;

    private void init() {
        // text views
        mDistView = (TextView) findViewById(R.id.details_distance);
        mTimeView = (TextView) findViewById(R.id.details_stopwatch);
        mCalView = (TextView) findViewById(R.id.details_calories);
        mUnitsView = (TextView) findViewById(R.id.details_units);
        // user preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.details_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.details_delete:
                // delete this route
                DataSource dataSrc = new DataSource(this);
                try {
                    dataSrc.open();
                    dataSrc.deleteRoute(mRouteId);
                } catch (SQLiteException sqle) {
                    sqle.printStackTrace();
                } finally {
                    if (dataSrc != null) dataSrc.close();
                }
                // done with this activity
                finish();
                return true;
        }
        return false;
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        addScrim();
        mLocations = fetchRouteLocations();
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                if (mLocations != null && !mLocations.isEmpty()) drawRoute(googleMap, mLocations);
            }
        });
    }

    private ArrayList<Location> fetchRouteLocations() {
        // get route ID
        mRouteId = getIntent().getLongExtra(DatabaseHelper.ROUTE_ID, -1);

        if (mRouteId == -1) {
            return null;

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
            // may be null
            return locations;
        }
    }

    private void drawRoute(final GoogleMap googleMap, final ArrayList<Location> locations) {
        // disable user interation until all drawing is complete
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
        // padding is one twentieth of map view width
        int padding = mMapFragment.getView().getWidth() / 20;

        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding),
                new GoogleMap.CancelableCallback() { // need to wait for zoom before we draw the route

                    @Override
                    public void onFinish() {
                        // want the overlay to cover all locations
                        final Projection projection = googleMap.getProjection();
                        Point upperRight = projection.toScreenLocation(bounds.northeast),
                                lowerLeft = projection.toScreenLocation(bounds.southwest);
                        int width = upperRight.x - lowerLeft.x,
                                height = lowerLeft.y - upperRight.y; // pixels

                        // ensure non-zero bitmap
                        final Bitmap bitmap = Bitmap.createBitmap(width + 1 + width, height + 1 + height,
                                Bitmap.Config.ARGB_8888);
                        final Canvas canvas = new Canvas(bitmap);
                        final Point origin = new Point(lowerLeft.x, upperRight.y); // upper left of canvas

                        // gradient paint
                        final Paint paint = new Paint();
                        paint.setAntiAlias(true); // sharpen diagonals
                        paint.setDither(true); // improve colors

                        // want the width of this paint to be just smaller than a road width
                        Location upRight = new Location("dummy_provider"),
                                lowLeft = new Location(upRight);
                        // set to corners
                        upRight.setLatitude(bounds.northeast.latitude);
                        upRight.setLongitude(bounds.northeast.longitude);
                        lowLeft.setLatitude(bounds.southwest.latitude);
                        lowLeft.setLongitude(bounds.southwest.longitude);
                        // get geographic distance and pixel distance between corners
                        float geoDistance = upRight.distanceTo(lowLeft);
                        float pixelDistance = Utils.euclidDistance(width, height);
                        float pixPerMeter = pixelDistance / geoDistance;
                        // width is ~27 meters (due of latitude distortion, this is a big opproximation
                        // for routes which cover large portions of the world map)
                        paint.setStrokeWidth(27 * pixPerMeter);

                        final Handler uiHandler = new Handler(); // for posting to UI thread
                        final Runnable overlayToMap = new Runnable() {
                            @Override
                            public void run() {
                                // add the overlay to the map
                                GroundOverlayOptions overlayOptions = new GroundOverlayOptions();
                                overlayOptions.positionFromBounds(bounds);
                                overlayOptions.image(BitmapDescriptorFactory.fromBitmap(bitmap));
                                // keep a reference to the overlay so we can edit or remove it later
                                mOverlay = googleMap.addGroundOverlay(overlayOptions);

                                // enable map interaction again
                                uiSettings.setAllGesturesEnabled(true);
                            }
                        };
                        // do all drawing on a background thread
                        new Thread() {
                            @Override
                            public void run() {
                                // use max speed to determine color scale
                                ColorScale colorScale = new ColorScale(maxSpeed);
                                for (int i = 0; i < locations.size(); i++) {
                                    Location l0 = locations.get(i);

                                    if (i + 1 < locations.size()) { // can't draw a line if there's no next point
                                        Location l1 = locations.get(i + 1);
                                        Point p0 = toCanvasPoint(l0, origin, projection),
                                                p1 = toCanvasPoint(l1, origin, projection);
                                        int color0 = colorScale.getColor(l0.getSpeed()),
                                                color1 = colorScale.getColor(l1.getSpeed());
                                        // gradient shader
                                        LinearGradient shader = new LinearGradient(p0.x, p0.y, p1.x, p1.y,
                                                color0, color1, Shader.TileMode.CLAMP);
                                        paint.setShader(shader);

                                        // draw a line between the two points
                                        canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint);
                                    }
                                    // TODO: mark each point with speed?
                                }
                                uiHandler.post(overlayToMap);
                            }
                        }.start();
                    }

                    @Override
                    public void onCancel() {
                        // TODO: retry the camera update
                    }
                });
    }

    private void addScrim() {
        FrameLayout parent = (FrameLayout) findViewById(R.id.map_container);
        ImageView scrim = new ImageView(DetailsActivity.this);
        scrim.setImageDrawable(getResources().getDrawable(R.drawable.rectangle_gradient));
        parent.addView(scrim, FrameLayout.LayoutParams.MATCH_PARENT);
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
            return Color.argb(255, (int) r, (int) g, (int) b);
        }
    }

    private Point toCanvasPoint(Location location, Point canvasOrigin, Projection projection) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        Point screenPoint = projection.toScreenLocation(latLng),
                canvasPoint = new Point();
        canvasPoint.x = screenPoint.x - canvasOrigin.x;
        canvasPoint.y = screenPoint.y - canvasOrigin.y;
        // canvas dimensions are doubled to improve quality
        canvasPoint.x *= 2;
        canvasPoint.y *= 2;
        return canvasPoint;
    }
}
