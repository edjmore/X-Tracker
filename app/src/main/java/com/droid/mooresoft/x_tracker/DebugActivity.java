package com.droid.mooresoft.x_tracker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ed on 6/10/15.
 */
public class DebugActivity extends Activity implements View.OnClickListener {

    private RouteArrayAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_activity);

        initListView();

        int[] buttonIds = {
                R.id.start_tracking, R.id.pause_tracking, R.id.resume_tracking, R.id.end_tracking,
        };
        for (int id : buttonIds) {
            Button b = (Button) findViewById(id);
            b.setOnClickListener(this);
        }
    }

    private void initListView() {
        DataSource dataSrc = new DataSource(this);
        try {
            dataSrc.open();
            Cursor cursor = dataSrc.fetchAllRoutes();
            mAdapter = new RouteArrayAdapter(this, cursor, 0);
            ListView list = (ListView) findViewById(R.id.route_list);
            list.setAdapter(mAdapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent viewMap = new Intent(DebugActivity.this, MapActivity.class);
                    viewMap.putExtra("id", id);
                    startActivity(viewMap);
                }
            });
        } catch (SQLiteException sqle) {
            sqle.printStackTrace();
        } finally {
            if (dataSrc != null) dataSrc.close();
        }
    }

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
        }
    }
}
