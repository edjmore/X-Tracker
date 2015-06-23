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
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Ed on 6/20/15.
 */
public class XTrackingService extends Service {

    public static boolean IS_RUNNING = false;

    public class XBinder extends Binder {
        XTrackingService getService() {
            return XTrackingService.this;
        }
    }

    private final XBinder mBinder = new XBinder();

    public ArrayList<Location> getLocationHistory() {
        return mLocationHistory;
    }

    public float getDistance() {
        return mDistance;
    }

    public long getElapsedTime() {
        return mStopwatch.check();
    }

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
        // all control actions
        filter.addAction(ACTION_RESUME);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_END);
        registerReceiver(mReciever, filter);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    private static final String PACKAGE = "com.droid.mooresoft.x_tracker";

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
        Toast.makeText(this, R.string.tracking_resumed, Toast.LENGTH_SHORT).show();

        // resume location updates
        String bestProvider = chooseLocationProvider();
        beginLocationTracking(bestProvider);
        // unpause the timer
        mStopwatch.start();
    }

    private void pauseTracking() {
        Toast.makeText(this, R.string.tracking_paused, Toast.LENGTH_SHORT).show();

        // stop the location updates
        mLocationManager.removeUpdates(mLocationListener);
        // pause the timer
        mStopwatch.stop();
    }

    private void endTracking() {
        Toast.makeText(this, R.string.tracking_ended, Toast.LENGTH_SHORT).show();

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
                long date = System.currentTimeMillis();

                try {
                    dataSrc.open();
                    // add the new route
                    dataSrc.addRoute(mLocationHistory, mDistance, elapsed, date);
                } catch (SQLiteException sqle) {
                    sqle.printStackTrace();
                } finally {
                    if (dataSrc != null) dataSrc.close(); // avoid database leak
                }
            }
        }.start();

        stopSelf(); // service is no longer needed
    }

    // for tracking elapsed time
    private final Stopwatch mStopwatch = new Stopwatch();
    // location data
    private final ArrayList<Location> mLocationHistory = new ArrayList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, R.string.tracking_started, Toast.LENGTH_SHORT).show();
        IS_RUNNING = true;

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

    private float mDistance = 0; // meters

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (!mLocationHistory.isEmpty()) { // update total travel distance
                int lastIndex = mLocationHistory.size() - 1;
                mDistance += mLocationHistory.get(lastIndex).distanceTo(location);
            }
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
    private class Stopwatch {
        long mStartTime = -1, mElapsedTime = 0;

        void start() {
            mStartTime = System.currentTimeMillis();
        }

        long stop() {
            if (mStartTime != -1) { // add the most recent elapsed time
                mElapsedTime += System.currentTimeMillis() - mStartTime;
            }
            mStartTime = -1;
            return mElapsedTime;
        }

        long check() {
            if (mStartTime == -1) {
                return mElapsedTime;
            } else {
                return mElapsedTime + System.currentTimeMillis() - mStartTime;
            }
        }
    }

    @Override
    public void onDestroy() {
        IS_RUNNING = false;
        // do cleanup
        unregisterReceiver(mReciever);
        mLocationManager.removeUpdates(mLocationListener);
        super.onDestroy();
    }
}
