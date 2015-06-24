package com.droid.mooresoft.x_tracker;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
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
        init();
    }

    private ListView mListView;

    private void init() {
        mListView = (ListView) findViewById(R.id.main_list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                long routeId = view.getId();
                // TODO: want to start the route details activity
                Intent details = new Intent(MainActivity.this, DetailsActivity.class);
                details.putExtra("id", routeId);
                startActivity(details);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // load all data
        DataSource dataSrc = new DataSource(this);
        try {
            dataSrc.open();
            // get all route data
            Cursor routesCursor = dataSrc.fetchAllRoutes();
            // populate the list view with data
            if (mAdapter == null) {
                mAdapter = new RouteArrayAdapter(this, routesCursor, 0);
                mListView.setAdapter(mAdapter);
            } else {
                mAdapter.swapCursor(routesCursor);
            }
        } catch (SQLiteException sqle) {
            sqle.printStackTrace();
        } finally {
            if (dataSrc != null) dataSrc.close();
        }
    }

    public void performClick(View view) {
        if (view.getId() == R.id.main_begin_exercise) {
            Intent intent = new Intent(this, ControlsActivity.class);
            startActivity(intent);
        }
    }
}
