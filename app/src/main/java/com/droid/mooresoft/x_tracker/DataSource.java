package com.droid.mooresoft.x_tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;

/**
 * Created by Ed on 6/14/15.
 */
public class DataSource {

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDatabase;

    public DataSource(Context context) {
        mDbHelper = new DatabaseHelper(context);
    }

    public void open() throws SQLiteException {
        mDatabase = mDbHelper.getWritableDatabase();
    }

    public void close() {
        mDatabase.close();
    }

    public long addRoute(ArrayList<Location> locations, float distance, long date) {
        // first add to the table of routes
        ContentValues routeValues = new ContentValues();
        routeValues.put(DatabaseHelper.ROUTE_DISTANCE, distance);
        routeValues.put(DatabaseHelper.ROUTE_DATE, date);
        long routeId = mDatabase.insert(DatabaseHelper.ROUTES_TABLE, null, routeValues);

        // now add location data for each point to the location data table
        final ContentValues locationDataValues = new ContentValues();
        for (Location l : locations) {
            // use the route ID to logically link the two tables
            locationDataValues.put(DatabaseHelper.LOCATION_DATA_ROUTE_ID, routeId);
            locationDataValues.put(DatabaseHelper.LOCATION_DATA_LATITUDE, l.getLatitude());
            locationDataValues.put(DatabaseHelper.LOCATION_DATA_LONGITUDE, l.getLongitude());
            locationDataValues.put(DatabaseHelper.LOCATION_DATA_SPEED, l.getSpeed());
            // each location will have its own row in the table
            mDatabase.insert(DatabaseHelper.LOCATION_DATA_TABLE, null, locationDataValues);
        }

        return routeId;
    }

    public Cursor fetchAllRoutes() {
        String[] allColumns = {
                DatabaseHelper.ROUTE_ID, DatabaseHelper.ROUTE_DISTANCE,
                DatabaseHelper.ROUTE_DATE
        };
        return mDatabase.query(DatabaseHelper.ROUTES_TABLE, allColumns, null, null, null, null, null);
    }

    public ArrayList<Location> fetchLocationData(long routeId) throws SQLiteException {
        String[] allColumns = {
                DatabaseHelper.LOCATION_DATA_LATITUDE, DatabaseHelper.LOCATION_DATA_LONGITUDE,
                DatabaseHelper.LOCATION_DATA_SPEED
        };
        String selectionClause = DatabaseHelper.LOCATION_DATA_ROUTE_ID + " = ?";
        String[] selectionArgs = {
                String.valueOf(routeId)
        };
        Cursor cursor = mDatabase.query(DatabaseHelper.LOCATION_DATA_TABLE, allColumns,
                selectionClause, selectionArgs, null, null, null); // each cursor row is a location

        if (cursor == null) {
            throw new SQLiteException();
        } else if (cursor.getCount() == 0) {
            return null; // there is no location data for this route
        } else {
            // there is at least one location for this route
            ArrayList<Location> locations = new ArrayList<>();

            int latIndex = cursor.getColumnIndex(DatabaseHelper.LOCATION_DATA_LATITUDE),
                    lngIndex = cursor.getColumnIndex(DatabaseHelper.LOCATION_DATA_LONGITUDE),
                    speedIndex = cursor.getColumnIndex(DatabaseHelper.LOCATION_DATA_SPEED);
            while (cursor.moveToNext()) {
                // build a location object from the row data
                Location l = new Location("dummy_provider");
                l.setLatitude(cursor.getDouble(latIndex));
                l.setLongitude(cursor.getDouble(lngIndex));
                l.setSpeed(cursor.getFloat(speedIndex));
                locations.add(l);
            }

            return locations;
        }
    }

    public void deleteRoute(long id) {
        // first delete the location data
        String whereClause = DatabaseHelper.LOCATION_DATA_ROUTE_ID + " = ?";
        String[] whereArgs = new String[]{
                String.valueOf(id)
        };
        mDatabase.delete(DatabaseHelper.LOCATION_DATA_TABLE, whereClause, whereArgs);

        // now delete the route
        whereClause = DatabaseHelper.ROUTE_ID + " = ?";
        // still using ID for the args
        mDatabase.delete(DatabaseHelper.ROUTES_TABLE, whereClause, whereArgs);
    }
}
