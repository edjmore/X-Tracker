package com.droid.mooresoft.x_tracker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by Ed on 6/10/15.
 */
public class DebugActivity extends Activity implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_activity);

        IntentFilter filter = new IntentFilter();
        filter.addAction("notify");
        registerReceiver(mReceiver, filter);

        int[] buttonIds = {
                R.id.start_tracking, R.id.pause_tracking, R.id.resume_tracking, R.id.end_tracking,
                R.id.view_map
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
                Intent startTracking = new Intent(this, GeoTrackingService.class);
                startService(startTracking);
                break;

            case R.id.pause_tracking:
                Intent pauseTracking = new Intent();
                pauseTracking.setAction(GeoTrackingService.PAUSE_TRACKING);
                sendBroadcast(pauseTracking);
                break;

            case R.id.resume_tracking:
                Intent resumeTracking = new Intent();
                resumeTracking.setAction(GeoTrackingService.RESUME_TRACKING);
                sendBroadcast(resumeTracking);
                break;

            case R.id.end_tracking:
                Intent endTracking = new Intent();
                endTracking.setAction(GeoTrackingService.END_TRACKING);
                sendBroadcast(endTracking);
                break;

            case R.id.view_map:
                Intent viewMap = new Intent(this, MapActivity.class);
                viewMap.putExtra("id", mRecentRouteId);
                startActivity(viewMap);
                break;
        }
    }

    private long mRecentRouteId = -1;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mRecentRouteId = intent.getLongExtra("id", -1);
            Toast.makeText(DebugActivity.this, "Recent route ID: " + mRecentRouteId, Toast.LENGTH_SHORT);
        }
    };

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
