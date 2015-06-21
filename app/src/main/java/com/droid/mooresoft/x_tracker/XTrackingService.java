package com.droid.mooresoft.x_tracker;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteException;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import java.util.ArrayList;

/**
 * Created by Ed on 6/20/15.
 */
public class XTrackingService extends Service {

    class XBinder extends Binder {
        XTrackingService getService() {
            return XTrackingService.this;
        }
    }

    private final XBinder mBinder = new XBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private LocationManager mLocationManager;

    @Override
    public void onCreate() {
        init();
    }

    private void init() {
        // register receiver for tracking control broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addCategory(CATEGORY_TRACKING_LIFECYCLE);
        registerReceiver(mReciever, filter);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    private static final String PACKAGE = "com.droid.mooresoft.x_tracker";

    // tracking controls intent category
    public static final String CATEGORY_TRACKING_LIFECYCLE = PACKAGE +
            ".CATEGORY_TRACKING_LIFECYCLE";
    // receivable actions for controlling tracking
    public static final String ACTION_RESUME = PACKAGE + ".ACTION_RESUME",
            ACTION_PAUSE = PACKAGE + ".ACTION_PAUSE",
            ACTION_END = PACKAGE + ".ACTION_END";

    private final BroadcastReceiver mReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                // perform the appropriate control action
                case ACTION_RESUME:
                    resumeTracking();
                    break;

                case ACTION_PAUSE:
                    pauseTracking();
                    break;

                case ACTION_END:
                    endTracking();
                    break;
            }
        }
    };

    private void resumeTracking() {
        // resume location updates
        String bestProvider = chooseLocationProvider();
        beginLocationTracking(bestProvider);
        // unpause the timer
        mStopwatch.start();
    }

    private void pauseTracking() {
        // stop the location updates
        mLocationManager.removeUpdates(mLocationListener);
        // pause the timer
        mStopwatch.stop();
    }

    private void endTracking() {
        // stop the location updates
        mLocationManager.removeUpdates(mLocationListener);
        // pause the timer
        final long elapsed = mStopwatch.stop();

        // store route data in the database
        final DataSource dataSrc = new DataSource(this);
        new Thread() { // do the write on a background thread
            @Override
            public void run() {
                // calculate other route data values
                float distance = calcDistance(mLocationHistory);
                long date = System.currentTimeMillis();

                try {
                    dataSrc.open();
                    // add the new route
                    dataSrc.addRoute(mLocationHistory, distance, elapsed, date);
                } catch (SQLiteException sqle) {
                    sqle.printStackTrace();
                } finally {
                    if (dataSrc != null) dataSrc.close(); // avoid database leak
                }
            }
        }.start();

        stopSelf(); // service is no longer needed
    }

    private float calcDistance(ArrayList<Location> locations) {
        float distance = 0; // accumulator
        for (int i = 0; i < locations.size(); i++) {
            if (i + 1 < locations.size()) {
                Location l0 = locations.get(i),
                        l1 = locations.get(i + 1); // consecutive locations
                float d = l0.distanceTo(l1);
                distance += d;
            }
        }
        return distance; // meters
    }

    // for tracking elapsed time
    private final Stopwatch mStopwatch = new Stopwatch();
    // location data
    private final ArrayList<Location> mLocationHistory = new ArrayList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // choose the best provider based on criteria
        String bestProvider = chooseLocationProvider();
        // start tracking user location
        if (bestProvider != null) beginLocationTracking(bestProvider);

        // begin timing the user's activity
        mStopwatch.start();

        return START_NOT_STICKY; // don't restart the service if killed
    }

    private String chooseLocationProvider() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(true);

        // TODO: handle case when there is a disabled provider that better matches our criteria...
        boolean enabledOnly = true;
        // TODO: ...or there are no enabled providers
        return mLocationManager.getBestProvider(criteria, enabledOnly);
    }

    private void beginLocationTracking(String bestProvider) {
        long minTime = 3 * 1000; // milliseconds
        float minDistance = 20; // meters
        mLocationManager.requestLocationUpdates(bestProvider, minTime, minDistance,
                mLocationListener);
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // add location point to the list
            mLocationHistory.add(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO: handle status change
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO: handle provider enabled
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO: handle provider disable
        }
    };

    // little class for easily tracking elapsed time in milliseconds
    class Stopwatch {
        long mStartTime, mElapsedTime = 0;

        void start() {
            mStartTime = System.currentTimeMillis();
        }

        long stop() {
            // add the most recent elapsed time
            mElapsedTime += System.currentTimeMillis() - mStartTime;
            return mElapsedTime;
        }

        void reset() {
            mElapsedTime = 0;
        }
    }

    @Override
    public void onDestroy() {
        // do cleanup
        unregisterReceiver(mReciever);
        mLocationManager.removeUpdates(mLocationListener);
        super.onDestroy();
    }
}
