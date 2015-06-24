package com.droid.mooresoft.x_tracker;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

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
                // store route details in the intent
                Intent details = new Intent(MainActivity.this, DetailsActivity.class);
                details.putExtra(DatabaseHelper.ROUTE_ID, routeId); // ID
                // add in route distance and time
                String distString = ((TextView) view.findViewById(R.id.route_item_distance)).getText().toString(),
                        timeString = ((TextView) view.findViewById(R.id.route_item_elapsed_time)).getText().toString();
                details.putExtra(DatabaseHelper.ROUTE_DISTANCE, distString);
                details.putExtra(DatabaseHelper.ROUTE_ELAPSED_TIME, timeString);
                // start route details activity
                startActivity(details);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent; // for starting new activities
        switch (item.getItemId()) {
            case R.id.main_begin_exercise:
                intent = new Intent(this, ControlsActivity.class);
                startActivity(intent);
                return true;

            case R.id.main_settings:
                intent = new Intent(this, PreferenceActivity.class);
                startActivity(intent);
                return true;
        }
        return false;
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
}
