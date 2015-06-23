package com.droid.mooresoft.x_tracker;

import android.util.Log;

import java.text.DecimalFormat;

/**
 * Created by Ed on 6/21/15.
 */
public class Utils {

    public static float metersToMiles(float meters) {
        return meters / 1609.34f;
    }

    public static float metersToKilometers(float meters) {
        return meters / 1000;
    }

    public static String millisToFormatedTime(long millis) {
        int oneSecond = 1000, oneMinute = oneSecond * 60, oneHour = oneMinute * 60;
        // break time into hours, minutes, and seconds
        int hours = (int) (millis / oneHour);
        millis -= hours * oneHour;
        int minutes = (int) (millis / oneMinute);
        millis -= minutes * oneMinute;
        int seconds = (int) (millis / oneSecond);

        DecimalFormat decimalFormat = new DecimalFormat("00");
        return decimalFormat.format(hours) + ":" + decimalFormat.format(minutes) + ":" +
                decimalFormat.format(seconds);
    }

    public static String toFormatedDistance(float distance) {
        return new DecimalFormat("0.0").format(distance);
    }
}
