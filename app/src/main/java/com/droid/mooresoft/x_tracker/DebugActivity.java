package com.droid.mooresoft.x_tracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by Ed on 6/10/15.
 */
public class DebugActivity extends Activity implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_activity);

        int[] buttonIds = {
                R.id.start_tracking, R.id.stop_tracking, R.id.view_map
        };
        for (int id : buttonIds) {
            Button b = (Button) findViewById(id);
            b.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_tracking:
                Intent geoTracking = new Intent(this, GeoTrackingService.class);
                startService(geoTracking);
                break;

            case R.id.stop_tracking:
                Intent stopGeoTracking = new Intent(this, GeoTrackingService.class);
                stopService(stopGeoTracking);
                break;

            case R.id.view_map:
                Intent viewMap = new Intent(this, MapActivity.class);
                startActivity(viewMap);
                break;
        }
    }
}
