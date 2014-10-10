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
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class LogProvider extends ContentProvider {
    private static final int LOG = 1;
    private static final int LOG_JUMP_ID = 2;
    private static final int LOG_LAST = 3;
    private static final int COMPLETIONS = 4;

    private UriMatcher mUriMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    private LogDatabaseHelper mOpenHelper;
    private BackupManager mBackupManager;

    private static String getAuthority(Context context) {
        return context.getPackageName() + ".log";
    }

    public static Uri getCompletionsUri(Context context) {
        return Uri.parse("content://" + getAuthority(context) + "/completions");
    }

    @Override
    public boolean onCreate() {
        String authority = getAuthority(getContext());
        mUriMatcher.addURI(authority, "log", LOG);
        mUriMatcher.addURI(authority, "log/#", LOG_JUMP_ID);
        mUriMatcher.addURI(authority, "log/last", LOG_LAST);
        mUriMatcher.addURI(authority, "completions", COMPLETIONS);

        mOpenHelper = new LogDatabaseHelper(getContext());
        mBackupManager = new BackupManager(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("log");

        int match = mUriMatcher.match(uri);
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
        int match = mUriMatcher.match(uri);
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
        switch (mUriMatcher.match(uri)) {
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
        if (mUriMatcher.match(uri) != LOG) {
            throw new IllegalArgumentException("Cannot insert into URI: " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert("log", null, values);
        Uri newUri = ContentUris.withAppendedId(LogEntry.Columns.CONTENT_URI, rowId);

        mBackupManager.dataChanged();
        getContext().getContentResolver().notifyChange(newUri, null);
        getContext().getContentResolver().notifyChange(
                Uri.withAppendedPath(LogEntry.Columns.CONTENT_URI, "last"), null);
        return newUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int count;
        long rowId = 0;
        int match = mUriMatcher.match(uri);
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
