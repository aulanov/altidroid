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
package org.openskydive.altidroid.util;

import org.openskydive.altidroid.Preferences;
import org.openskydive.altidroid.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class Units implements OnSharedPreferenceChangeListener {
    // User units:
    //   - meters in metric mode
    //   - feet in imperial mode
    // System units:
    //   - millimeters regardless of the user-selected units

    private static Units mInstance;

    private Type mPreferred;

    private final SharedPreferences mPreferences;

    public enum Type {
        METRIC,
        IMPERIAL,
    };

    public Units(Context context) {
        mPreferences = Preferences.getPrefs(context.getApplicationContext());
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        readPrefs();
    }

    private void readPrefs() {
        // Default value is actually set in resources.
        if (mPreferences.getBoolean(Preferences.USE_IMPERIAL_UNITS, false)) {
            mPreferred = Type.IMPERIAL;
        } else {
            mPreferred = Type.METRIC;
        }
    }

    public static Units getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        } else {
            synchronized (Units.class) {
                if (mInstance == null) {
                    mInstance = new Units(context);
                }
            }
            return mInstance;
        }
    }

    public Type getPreferred() {
        return mPreferred;
    }

    public int toUserUnits(int distance) {
        if (mPreferred == Type.METRIC) {
            return toMeters(distance);
        } else {
            return toFoot(distance);
        }
    }

    public int fromUserUnits(int distance) {
        if (mPreferred == Type.METRIC) {
            return fromMeters(distance);
        } else {
            return fromFoot(distance);
        }
    }

    public String getUnitsNameShort(Context context) {
        int resId;
        if (mPreferred == Type.METRIC) {
            resId = R.string.units_m_short;
        } else {
            resId = R.string.units_ft_short;
        }
        return context.getString(resId);
    }

    public CharSequence formatAltitude(Context context, int altitude) {
        return toUserUnits(altitude) + " " + getUnitsNameShort(context);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(Preferences.USE_IMPERIAL_UNITS)) {
            readPrefs();
        }
    }

    public static int fromFoot(int foot) {
        return Math.round(foot * 304.8f);
    }

    public static int toFoot(int dist) {
        return Math.round(dist / 304.8f);
    }

    public static int fromMeters(int meters) {
        return Math.round(meters * 1000f);
    }

    public static int toMeters(int dist) {
        return Math.round(dist / 1000f);
    }

    /**
     * @param speed vertical speed in meters per second
     * @return formatted climb rate string, e.g. "500 ft/min"
     */
    public String formatClimbRate(Context context, float speed) {
        int s = Math.round(speed * 60000); // mm/min.
        int resId;
        if (mPreferred == Type.METRIC) {
            resId = R.string.climb_rate_meters_per_min;
        } else {
            resId = R.string.climb_rate_ft_per_min;
        }
        // TODO: round to nearest hundreds
        return context.getString(resId, toUserUnits(s));
    }
}
