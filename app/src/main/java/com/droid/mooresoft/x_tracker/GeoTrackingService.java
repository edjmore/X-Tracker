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
 * Created by Ed on 6/10/15.
 */
public class GeoTrackingService extends Service {

    public static final String PAUSE_TRACKING = "com.droid.mooresoft.x_tracker.PAUSE_TRACKING",
            RESUME_TRACKING = "com.droid.mooresoft.x_tracker.RESUME_TRACKING",
            END_TRACKING = "com.droid.mooresoft.x_tracker.END_TRACKING";

    private LocationManager mLocationManager;
    // TODO: use custom location object to save memory?
    private final ArrayList<Location> mLocationHistory = new ArrayList<>();

    @Override
    public void onCreate() {
        init();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case PAUSE_TRACKING:
                    Toast.makeText(context, R.string.tracking_paused, Toast.LENGTH_SHORT).show();
                    unregisterListener(); // stop location updates
                    break;

                case RESUME_TRACKING:
                    Toast.makeText(context, R.string.tracking_resumed, Toast.LENGTH_SHORT).show();
                    registerListener(); // begin location updates again
                    break;

                case END_TRACKING:
                    Toast.makeText(context, R.string.tracking_ended, Toast.LENGTH_SHORT).show();
                    writeBack(); // save route data
                    stopSelf(); // stop this service
                    break;
            }
        }
    };

    private void init() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        // initialize broadcast reciever
        IntentFilter filter = new IntentFilter();
        filter.addAction(PAUSE_TRACKING);
        filter.addAction(RESUME_TRACKING);
        filter.addAction(END_TRACKING);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerListener(); // begin location updates
        return START_NOT_STICKY; // don't restart service if it is killed
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
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

    private void registerListener() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(true);
        // TODO: handle case when there is a disabled provider that better matches our criteria...
        boolean enabledOnly = true;
        String bestProvider = mLocationManager.getBestProvider(criteria, enabledOnly);
        // TODO: ...or there are no enabled providers
        if (bestProvider != null) {
            long minTime = 3 * 1000; // milliseconds
            float minDistance = 20; // meters
            mLocationManager.requestLocationUpdates(bestProvider, minTime, minDistance,
                    mLocationListener);
        }
    }

    private void writeBack() {
        DataSource dataSrc = new DataSource(this);
        try {
            dataSrc.open();
            dataSrc.addRoute(mLocationHistory, 0, 0);
        } catch (SQLiteException sqle) {
            sqle.printStackTrace();
        } finally {
            if (dataSrc != null) dataSrc.close();
        }
    }

    @Override
    public void onDestroy() {
        unregisterListener(); // location listener
        unregisterReceiver(mReceiver);
    }

    private void unregisterListener() {
        mLocationManager.removeUpdates(mLocationListener);
    }

    public ArrayList<Location> getLocationHistory() {
        return mLocationHistory;
    }

    public class GeoTrackingBinder extends Binder {

        GeoTrackingService getService() {
            return GeoTrackingService.this;
        }
    }

    private final IBinder mBinder = new GeoTrackingBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
