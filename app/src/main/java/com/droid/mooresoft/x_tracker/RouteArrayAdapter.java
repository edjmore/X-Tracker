package com.droid.mooresoft.x_tracker;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

/**
 * Created by Ed on 6/15/15.
 */
public class RouteArrayAdapter extends CursorAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private int mIdIndex, mDistanceIndex, mDateIndex;

    public RouteArrayAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // get cursor column indices
        mIdIndex = cursor.getColumnIndex(DatabaseHelper.ROUTE_ID);
        mDistanceIndex = cursor.getColumnIndex(DatabaseHelper.ROUTE_DISTANCE);
        mDateIndex = cursor.getColumnIndex(DatabaseHelper.ROUTE_DATE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.route_item, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // route distance
        TextView distanceView = (TextView) view.findViewById(R.id.route_item_distance),
                unitsView = (TextView) view.findViewById(R.id.route_item_distance_units);
        float distance = cursor.getFloat(mDistanceIndex);
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
        distanceView.setText(decimalFormat.format(distance));

        // date of exercise
        TextView dateView = (TextView) view.findViewById(R.id.route_item_date);
        long dateMillis = cursor.getLong(mDateIndex);
        Date date = new Date(dateMillis);
        DateFormat dateFormat = DateFormat.getInstance();
        dateView.setText(dateFormat.format(date));

        // view tag
        int id = cursor.getInt(0);
        view.setId(id);
    }
}
