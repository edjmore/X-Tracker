package com.droid.mooresoft.x_tracker;

import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import java.util.ArrayList;

/**
 * Created by Ed on 6/10/15.
 */
public class GeoTrackingService extends Service {

    public ArrayList<Location> getLocationHistory() {
        return mLocationHistory;
    }

    public class CustomBinder extends Binder {

        public GeoTrackingService getService() {
            return GeoTrackingService.this;
        }
    }

    private final IBinder mBinder = new CustomBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private LocationManager mLocationManager;
    // TODO: use custom location object to save memory?
    private final ArrayList<Location> mLocationHistory = new ArrayList<>();

    @Override
    public void onCreate() {
        init();
    }

    private void init() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerListener();
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
            long minTime = 30 * 1000; // milliseconds
            float minDistance = 50; // meters
            mLocationManager.requestLocationUpdates(bestProvider, minTime, minDistance,
                    mLocationListener);
        }
    }

    @Override
    public void onDestroy() {
        unregisterListener();
    }

    private void unregisterListener() {
        mLocationManager.removeUpdates(mLocationListener);
    }
}
