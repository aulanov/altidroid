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
package org.openskydive.altidroid.log;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LogDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "log.db";
    private static final int DATABASE_VERSION = 2;

    public static String getDbPath(Context context) {
        return context.getDatabasePath(DATABASE_NAME).getPath();
    }

    public LogDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE log(" +
                "_id INTEGER PRIMARY KEY," +
                "number INTEGER," +
                "date INTEGER," +
                "exit_altitude INTEGER," +
                "deploy_altitude INTEGER," +
                "freefall_time INTEGER," +
                "comments TEXT," +
                "dropzone TEXT," +
                "aircraft TEXT," +
                "jumptype TEXT," +
                "equipment TEXT," +
                "backed_up BOOL);");
        db.execSQL(
                "CREATE TRIGGER log_reset_backed_up BEFORE UPDATE ON log BEGIN " +
                "  UPDATE log SET backed_up=null WHERE _id=new._id;" +
                "END;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            db.execSQL("ALTER TABLE log ADD backed_up BOOL;");
            db.execSQL(
                    "CREATE TRIGGER log_reset_backed_up BEFORE UPDATE ON log BEGIN " +
                    "  UPDATE log SET backed_up=null WHERE _id=new._id;" +
                    "END;");
        }
    }
}
