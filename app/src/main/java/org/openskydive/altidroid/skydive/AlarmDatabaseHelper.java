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

import org.openskydive.altidroid.R;
import org.openskydive.altidroid.util.ToneUri;
import org.openskydive.altidroid.util.Units;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Locale;

public class AlarmDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "alarms.db";
    private static final int DATABASE_VERSION = 1;
    private final Context mContext;

    private static class DefaultAlarm {
        public DefaultAlarm(int name_id, Alarm.Type type, int altitude, int tone_id) {
            this.name_id = name_id;
            this.type = type;
            this.altitude = altitude;
            this.tone_id = tone_id;
        }
        int name_id;
        Alarm.Type type;
        int altitude;
        int tone_id;

        String genInsertSql(Context context) {
            return String.format(Locale.US, "INSERT INTO alarms " +
                    "(name, type, value, ringtone, ringtone_name) " +
                    "VALUES (\"%s\", %d, %d, \"%s\", \"%s\")",
                    context.getString(name_id),
                    type.ordinal(), altitude,
                    ToneUri.getBuiltInUri(tone_id),
                    ToneUri.getBuiltInUriName(context, tone_id));

        }
    }

    private static DefaultAlarm DEFAULT_ALARMS_IMPERIAL[] = {
        new DefaultAlarm(R.string.break_off_name, Alarm.Type.FREEFALL, Units.fromFoot(4500), 0),
        new DefaultAlarm(R.string.deploy_name, Alarm.Type.FREEFALL, Units.fromFoot(3500), 1),
        new DefaultAlarm(R.string.danger_name, Alarm.Type.FREEFALL, Units.fromFoot(1500), 2),

        new DefaultAlarm(R.string.downwind_leg_name, Alarm.Type.CANOPY, Units.fromFoot(900), 0),
        new DefaultAlarm(R.string.base_leg_name, Alarm.Type.CANOPY, Units.fromFoot(600), 0),
        new DefaultAlarm(R.string.final_approach_name, Alarm.Type.CANOPY, Units.fromFoot(300), 0),
    };

    private static DefaultAlarm DEFAULT_ALARMS_METRIC[] = {
        new DefaultAlarm(R.string.break_off_name, Alarm.Type.FREEFALL, Units.fromMeters(1300), 0),
        new DefaultAlarm(R.string.deploy_name, Alarm.Type.FREEFALL, Units.fromMeters(1000), 1),
        new DefaultAlarm(R.string.danger_name, Alarm.Type.FREEFALL, Units.fromMeters(500), 2),

        new DefaultAlarm(R.string.downwind_leg_name, Alarm.Type.CANOPY, Units.fromMeters(300), 0),
        new DefaultAlarm(R.string.base_leg_name, Alarm.Type.CANOPY, Units.fromMeters(200), 0),
        new DefaultAlarm(R.string.final_approach_name, Alarm.Type.CANOPY, Units.fromMeters(100), 0),
    };

    public AlarmDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE alarms(" +
                "_id INTEGER PRIMARY KEY," +
                "name TEXT," +
                "type TEXT," +
                "value INTEGER," +
                "ringtone TEXT," +
                "ringtone_name TEXT);");

        DefaultAlarm defaultAlarms[];
        if (Units.getInstance(mContext).getPreferred() == Units.Type.IMPERIAL) {
            defaultAlarms = DEFAULT_ALARMS_IMPERIAL;
        } else {
            defaultAlarms = DEFAULT_ALARMS_METRIC;
        }
        for (DefaultAlarm alarm : defaultAlarms) {
            db.execSQL(alarm.genInsertSql(mContext));
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}