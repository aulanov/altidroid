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
package org.openskydive.altidroid.skydive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

/**
 * Keeps a list of audible and visual notifications.
 */
public class AlarmList {
    public class MyDataSetObserver extends ContentObserver {
        public MyDataSetObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.i("Altidroid", "Alarm list changed. Reloading...");
            reload();
        }
    }

    public class AltitudeComparator implements Comparator<Alarm> {
        @Override
        public int compare(Alarm a, Alarm b) {
            return b.getValue() - a.getValue();
        }
    }
    public class TimerComparator implements Comparator<Alarm> {
        @Override
        public int compare(Alarm a, Alarm b) {
            return a.getValue() - b.getValue();
        }
    }

    private List<Alarm> mFreefallAlarms = new ArrayList<Alarm>();
    private List<Alarm> mTimerAlarms = new ArrayList<Alarm>();
    private List<Alarm> mCanopyAlarms = new ArrayList<Alarm>();
    private final Context mContext;
    private Cursor mCursor;
    private final MyDataSetObserver mDataSetObserver = new MyDataSetObserver();

    public AlarmList(Context context) {
        mContext = context;

        reload();
    }

    private synchronized void reload() {
        mCursor = mContext.getContentResolver().query(
                Alarm.Columns.CONTENT_URI,
                Alarm.Columns.QUERY_COLUMNS,
                null, null, null);
        mCursor.setNotificationUri(mContext.getContentResolver(), Alarm.Columns.CONTENT_URI);
        mFreefallAlarms = new ArrayList<Alarm>();
        mTimerAlarms = new ArrayList<Alarm>();
        mCanopyAlarms = new ArrayList<Alarm>();

        Alarm.Reader reader = new Alarm.Reader(mCursor);
        while(mCursor.moveToNext()) {
            Alarm a = reader.read();
            switch (a.getType()) {
            case FREEFALL:
                mFreefallAlarms.add(a);
                break;
            case FREEFALL_DELAY:
                mTimerAlarms.add(a);
                break;
            case CANOPY:
                mCanopyAlarms.add(a);
                break;
            }
        }
        Collections.sort(mFreefallAlarms, new AltitudeComparator());
        Collections.sort(mCanopyAlarms, new AltitudeComparator());
        Collections.sort(mTimerAlarms, new TimerComparator());

        mCursor.registerContentObserver(mDataSetObserver);
    }

    public synchronized Alarm getAlarm(SkydiveState prevState, SkydiveState thisState, int mLowestAlarmAltitude) {
        if (thisState.getType() == SkydiveState.Type.FREEFALL) {
            int prevAltitude = Math.min(prevState.getRelativeAltitude(), mLowestAlarmAltitude);
            Alarm alarm = findAlarm(mFreefallAlarms,
                    prevAltitude,
                    thisState.getRelativeAltitude());
            if (alarm != null) {
                return alarm;
            }
            return findAlarm(mTimerAlarms,
                    thisState.getFreefallTime() / 1000,
                    prevState.getFreefallTime() / 1000);
        } else if (thisState.getType() == SkydiveState.Type.CANOPY) {
            int prevAltitude = Math.min(prevState.getRelativeAltitude(), mLowestAlarmAltitude);
            return findAlarm(mCanopyAlarms,
                    prevAltitude,
                    thisState.getRelativeAltitude());
        }
        return null;
    }

    private static Alarm findAlarm(List<Alarm> alarms, int highValue, int lowValue) {
        if (highValue < lowValue) {
            return null;
        }
        for (Alarm a : alarms) {
            if (highValue > a.getValue() && a.getValue() >= lowValue) {
                return a;
            }
        }
        return null;
    }
}
