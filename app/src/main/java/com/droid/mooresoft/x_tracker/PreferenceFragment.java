package com.droid.mooresoft.x_tracker;

import android.os.Bundle;

/**
 * Created by Ed on 6/1/15.
 */
public class PreferenceFragment extends android.preference.PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_screen);
    }
}
