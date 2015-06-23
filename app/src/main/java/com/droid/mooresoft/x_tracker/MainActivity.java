package com.droid.mooresoft.x_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ListView;

/**
 * Created by Ed on 6/21/15.
 */
public class MainActivity extends ActionBarActivity {

    private RouteArrayAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // TODO: list view

    }

    private ListView mListView;

    private void init() {
        mListView = (ListView) findViewById(R.id.main_list);
    }

    public void performClick(View view) {
        if (view.getId() == R.id.main_begin_exercise) {
            Intent intent = new Intent(this, ControlsActivity.class);
            startActivity(intent);
        }
    }
}
