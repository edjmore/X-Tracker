package com.droid.mooresoft.x_tracker;

import android.app.Service;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by Ed on 6/10/15.
 */
public class GeoTrackingService extends Service {

    public class CustomBinder extends Binder {

        GeoTrackingService getService() {
            return GeoTrackingService.this;
        }
    }

    private final IBinder mBinder = new CustomBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private LocationManager mLocationManager;

    @Override
    public void onCreate() {

    }

    private void init() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }
}
