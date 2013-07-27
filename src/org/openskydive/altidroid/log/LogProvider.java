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

import android.app.backup.BackupManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class LogProvider extends ContentProvider {
    public static final Uri COMPLETIONS_URI =
            Uri.parse("content://org.openskydive.altidroid.log/completions");

    private static final int LOG = 1;
    private static final int LOG_JUMP_ID = 2;
    private static final int LOG_LAST = 3;
    private static final int COMPLETIONS = 4;

    private static final UriMatcher sURLMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        sURLMatcher.addURI("org.openskydive.altidroid.log", "log", LOG);
        sURLMatcher.addURI("org.openskydive.altidroid.log", "log/#", LOG_JUMP_ID);
        sURLMatcher.addURI("org.openskydive.altidroid.log", "log/last", LOG_LAST);
        sURLMatcher.addURI("org.openskydive.altidroid.log", "completions", COMPLETIONS);
    }

    private LogDatabaseHelper mOpenHelper;
    private BackupManager mBackupManager;

    @Override
    public boolean onCreate() {
        mOpenHelper = new LogDatabaseHelper(getContext());
        mBackupManager = new BackupManager(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("log");

        int match = sURLMatcher.match(uri);
        String groupBy = null;
        String limit = null;
        switch (match) {
        case LOG:
            break;
        case LOG_JUMP_ID:
            qb.appendWhere("_id=");
            qb.appendWhere(uri.getPathSegments().get(1));
            break;
        case LOG_LAST:
            limit = "1";
            break;
        case COMPLETIONS:
            if (projection.length > 0) {
                groupBy = projection[projection.length - 1];
            }
            break;
        default:
            throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor ret = qb.query(db, projection, selection, selectionArgs,
                groupBy, null, sortOrder, limit);
        if (ret != null) {
            ret.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return ret;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURLMatcher.match(uri);
        switch (match) {
        case LOG:
            return "vnd.android.cursor.dir/skydive_log";
        case LOG_JUMP_ID:
            return "vnd.android.cursor.item/skydive_log";
        default:
            throw new IllegalArgumentException("Unrecognised URI: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sURLMatcher.match(uri)) {
        case LOG:
            count = db.delete("log", where, whereArgs);
            break;
        case LOG_JUMP_ID:
            String segment = uri.getPathSegments().get(1);
            if (TextUtils.isEmpty(where)) {
                where = "_id=" + segment;
            } else {
                where = "_id=" + segment + " AND (" + where + ")";
            }
            count = db.delete("log", where, whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Cannot delete from URI: " + uri);
        }

        mBackupManager.dataChanged();
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (sURLMatcher.match(uri) != LOG) {
            throw new IllegalArgumentException("Cannot insert into URI: " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert("log", null, values);
        Uri newUri = ContentUris.withAppendedId(LogEntry.Columns.CONTENT_URI, rowId);

        mBackupManager.dataChanged();
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
        case LOG_JUMP_ID: {
            String segment = uri.getPathSegments().get(1);
            rowId = Long.parseLong(segment);
            count = db.update("log", values, "_id=" + rowId, null);
            break;
        }
        case LOG:
            count = db.update("log", values, selection, selectionArgs);
            break;
        default:
            throw new UnsupportedOperationException(
                    "Cannot update URI: " + uri);
        }

        mBackupManager.dataChanged();
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
