// Copyright 2012-2013 Andrey Ulanov
//
// This file is part of Altidroid.
//
// Altidroid is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Altidroid is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Altidroid.  If not, see <http://www.gnu.org/licenses/>.
package org.openskydive.altidroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {
    public static final String PREFS_NAME = "altidroid_preferences";

    public static final String USE_IMPERIAL_UNITS = "use_imperial_units";
    public static final String FREEFALL_VOLUME = "freefall_volume";
    public static final String CANOPY_VOLUME = "canopy_volume";
    public static final String LOG_AUTO_FILL = "log_auto_fill";
    public static final String SHOW_WARNING = "show_warning";

    private AlarmPlayer mAlarmPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAlarmPlayer = new AlarmPlayer(this);

        super.onCreate(savedInstanceState);

        getPrefs(this);
        getPreferenceManager().setSharedPreferencesName(PREFS_NAME);

        addPreferencesFromResource(R.xml.settings);
    }

    public static SharedPreferences getPrefs(Context context) {
        PreferenceManager.setDefaultValues(context, PREFS_NAME, MODE_PRIVATE,
                R.xml.settings, false);
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mAlarmPlayer.shutdown();
    }

    public AlarmPlayer getAlarmPlayer() {
        return mAlarmPlayer;
    }
}
