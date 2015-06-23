package com.droid.mooresoft.x_tracker;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Ed on 6/16/15.
 */
public class ControlsActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controls_activity);
        init();

        // start and bind with the tracking service
        Intent service = new Intent(this, XTrackingService.class);
        bindService(service, mConnection, BIND_AUTO_CREATE);
        if (!XTrackingService.IS_RUNNING) startService(new Intent(this, XTrackingService.class));

        // begin continuous UI updates
        mUiHandler.post(mUiRunner);
    }

    private TextView mDistanceView, mStopwatchView;
    private RoundButton mResumeButton, mPauseButton, mEndButton;

    private void init() {
        // text views
        mDistanceView = (TextView) findViewById(R.id.controls_distance);
        mStopwatchView = (TextView) findViewById(R.id.controls_stopwatch);
        // buttons
        mResumeButton = (RoundButton) findViewById(R.id.controls_resume);
        mPauseButton = (RoundButton) findViewById(R.id.controls_pause);
        mEndButton = (RoundButton) findViewById(R.id.controls_end);
    }

    private XTrackingService mBoundService;
    private boolean mIsBound = false;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundService = ((XTrackingService.XBinder) service).getService();
            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
            mIsBound = false;
        }
    };

    // for updating the UI
    private final Handler mUiHandler = new Handler();

    private final Runnable mUiRunner = new Runnable() {
        @Override
        public void run() {
            // schedule the next update
            mUiHandler.postDelayed(mUiRunner, 500); // every half second

            if (mBoundService != null) {
                // get the current values
                float distance = mBoundService.getDistance();
                long elapsed = mBoundService.getElapsedTime();

                // convert to appropriate units and format
                float miles = Utils.metersToMiles(distance);
                String distString = Utils.toFormatedDistance(miles);
                String timeString = Utils.millisToFormatedTime(elapsed);

                // update views
                mDistanceView.setText(distString);
                mStopwatchView.setText(timeString);
            }
        }
    };

    public void performClick(View view) {
        // want to control some aspect of the tracking service
        Intent controlIntent = new Intent();

        switch (view.getId()) {
            case R.id.controls_resume:
                controlIntent.setAction(XTrackingService.ACTION_RESUME);
                // enable pause and end
                mPauseButton.setEnabled(true);
                mEndButton.setEnabled(false);
                break;

            case R.id.controls_pause:
                controlIntent.setAction(XTrackingService.ACTION_PAUSE);
                // enable resume and end
                mResumeButton.setEnabled(true);
                mEndButton.setEnabled(true);
                break;

            case R.id.controls_end:
                controlIntent.setAction(XTrackingService.ACTION_END);
                mUiHandler.removeCallbacks(mUiRunner); // no longer need to update the UI
                // disable all
                mResumeButton.setEnabled(false);
                mPauseButton.setEnabled(false);
                mEndButton.setEnabled(false);
                break;
        }

        // send the intent
        sendBroadcast(controlIntent);
    }

    @Override
    public void onPause() {
        if (mIsBound) unbindService(mConnection);
        super.onPause();
    }
}
