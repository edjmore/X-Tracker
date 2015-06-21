package com.droid.mooresoft.x_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

/**
 * Created by Ed on 6/21/15.
 */
public class MainActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // TODO: list view
    }

    public void performClick(View view) {
        if (view.getId() == R.id.main_begin_exercise) {
            Intent intent = new Intent(this, ControlsActivity.class);
            startActivity(intent);
        }
    }
}
