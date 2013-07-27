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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class AlarmProvider extends ContentProvider {

    private static final int ALARMS = 1;
    private static final int ALARMS_ID = 2;
    private static final UriMatcher sURLMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        sURLMatcher.addURI("org.openskydive.altidroid", "alarm", ALARMS);
        sURLMatcher.addURI("org.openskydive.altidroid", "alarm/#", ALARMS_ID);
    }

    private AlarmDatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new AlarmDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("alarms");

        int match = sURLMatcher.match(uri);

        switch(match) {
        case ALARMS:
            break;
        case ALARMS_ID:
            qb.appendWhere("_id=");
            qb.appendWhere(uri.getPathSegments().get(1));
            break;
        default:
            throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        if (ret != null) {
            ret.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return ret;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURLMatcher.match(uri);
        switch (match) {
        case ALARMS:
            return "vnd.android.cursor.dir/skydive_alarm";
        case ALARMS_ID:
            return "vnd.android.cursor.item/skydive_alarm";
        default:
            throw new IllegalArgumentException("Unrecognised URI: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sURLMatcher.match(uri)) {
        case ALARMS:
            count = db.delete("alarms", where, whereArgs);
            break;
        case ALARMS_ID:
            String segment = uri.getPathSegments().get(1);
            if (TextUtils.isEmpty(where)) {
                where = "_id=" + segment;
            } else {
                where = "_id=" + segment + " AND (" + where + ")";
            }
            count = db.delete("alarms", where, whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Cannot delete from URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (sURLMatcher.match(uri) != ALARMS) {
            throw new IllegalArgumentException("Cannot insert into URI: " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert("alarms", null, values);
        Uri newUri = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, rowId);
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int count;
        long rowId = 0;
        int match = sURLMatcher.match(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (match) {
        case ALARMS_ID: {
            String segment = uri.getPathSegments().get(1);
            rowId = Long.parseLong(segment);
            count = db.update("alarms", values, "_id=" + rowId, null);
            break;
        }
        default:
            throw new UnsupportedOperationException(
                    "Cannot update URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

}
