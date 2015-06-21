package com.droid.mooresoft.x_tracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Ed on 6/14/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // database
    private static final String DATABASE_NAME = "x_tracker.db";
    private static final int DATABASE_VERSION = 1;

    // tables
    public static final String ROUTES_TABLE = "routes";
    public static final String LOCATION_DATA_TABLE = "location_data";

    // columns
    public static final String ROUTE_ID = "_id",
            ROUTE_DISTANCE = "distance",
            ROUTE_ELAPSED_TIME = "elapsed_time",
            ROUTE_DATE = "date";
    public static final String LOCATION_DATA_ID = "_id",
            LOCATION_DATA_ROUTE_ID = "route_id",
            LOCATION_DATA_LATITUDE = "latitude",
            LOCATION_DATA_LONGITUDE = "longitude",
            LOCATION_DATA_SPEED = "speed";

    private static final String CREATE_ROUTES_TABLE = "create table " + ROUTES_TABLE + "(" +
            ROUTE_ID + " integer primary key autoincrement, " +
            ROUTE_DISTANCE + " float, " +
            ROUTE_ELAPSED_TIME + " long, " +
            ROUTE_DATE + " long);";

    private static final String CREATE_LOCATION_DATA_TABLE = "create table " + LOCATION_DATA_TABLE + "(" +
            LOCATION_DATA_ID + " integer primary key autoincrement, " +
            LOCATION_DATA_ROUTE_ID + " integer, " +
            LOCATION_DATA_LATITUDE + " double, " +
            LOCATION_DATA_LONGITUDE + " double, " +
            LOCATION_DATA_SPEED + " float);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_ROUTES_TABLE);
        db.execSQL(CREATE_LOCATION_DATA_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ROUTES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LOCATION_DATA_TABLE);
        onCreate(db);
    }
}
