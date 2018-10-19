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

import org.openskydive.altidroid.log.LogProtos.Entry.Builder;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

public class LogEntry implements Parcelable {
    final LogProtos.Entry mProto;

    public static class Columns {
        public static final Uri getContentUri(Context context) {
            return Uri.parse("content://" + context.getPackageName() + ".log/log");
        }

        public static final String ID = "_id";
        public static final String NUMBER = "number";
        public static final String DATE = "date";
        public static final String EXIT_ALTITUDE = "exit_altitude";
        public static final String DEPLOY_ALTITUDE = "deploy_altitude";
        public static final String FREEFALL_TIME = "freefall_time";
        public static final String COMMENTS = "comments";
        public static final String DROPZONE = "dropzone";
        public static final String AIRCRAFT = "aircraft";
        public static final String JUMPTYPE = "jumptype";
        public static final String EQUIPMENT = "equipment";

        public static final String[] QUERY_COLUMNS = null;
    }

    public static final Parcelable.Creator<LogEntry> CREATOR
    = new Parcelable.Creator<LogEntry>() {
        public LogEntry createFromParcel(Parcel p) {
            try {
                return new LogEntry(p);
            } catch (InvalidProtocolBufferException e) {
                Log.e("Altidroid", "Unable to parse log entry");
                return new LogEntry(LogProtos.Entry.newBuilder().build());
            }
        }
        public LogEntry[] newArray(int size) {
            return new LogEntry[size];
        }
    };


    public static LogEntry readLastJump(Context context) {
        Cursor cursor = context.getContentResolver().query(
                Uri.withAppendedPath(LogEntry.Columns.getContentUri(context), "last"),
                LogEntry.Columns.QUERY_COLUMNS, null, null,
                LogEntry.Columns.NUMBER + " DESC");
        if (cursor != null) {
            if (cursor.moveToNext()) {
                return new LogEntry(cursor);
            }
            cursor.close();
        }
        return null;
    }

    public LogEntry(Cursor cursor) {
        Builder builder = LogProtos.Entry.newBuilder();
        builder.setId(cursor.getInt(cursor.getColumnIndex(Columns.ID)));
        builder.setNumber(cursor.getInt(cursor.getColumnIndex(Columns.NUMBER)));
        builder.setTimestamp(cursor.getLong(cursor.getColumnIndex(Columns.DATE)));
        builder.setExitAltitude(cursor.getInt(cursor.getColumnIndex(Columns.EXIT_ALTITUDE)));
        builder.setDeployAltitude(cursor.getInt(cursor.getColumnIndex(Columns.DEPLOY_ALTITUDE)));
        builder.setFreefallTime(cursor.getInt(cursor.getColumnIndex(Columns.FREEFALL_TIME)));

        int id = cursor.getColumnIndex(Columns.COMMENTS);
        if (!cursor.isNull(id) && !cursor.getString(id).isEmpty()) {
            builder.setComments(cursor.getString(id));
        }
        id = cursor.getColumnIndex(Columns.DROPZONE);
        if (!cursor.isNull(id) && !cursor.getString(id).isEmpty()) {
            builder.setDropzone(cursor.getString(id));
        }
        id = cursor.getColumnIndex(Columns.AIRCRAFT);
        if (!cursor.isNull(id) && !cursor.getString(id).isEmpty()) {
            builder.setAircraft(cursor.getString(id));
        }
        id = cursor.getColumnIndex(Columns.JUMPTYPE);
        if (!cursor.isNull(id) && !cursor.getString(id).isEmpty()) {
            builder.setJumpType(cursor.getString(id));
        }
        id = cursor.getColumnIndex(Columns.EQUIPMENT);
        if (!cursor.isNull(id) && !cursor.getString(id).isEmpty()) {
            builder.setEquipment(cursor.getString(id));
        }

        mProto = builder.build();
    }

    public LogEntry(Parcel parcel) throws InvalidProtocolBufferException {
        int len = parcel.readInt();
        byte[] bytes = new byte[len];
        parcel.readByteArray(bytes);
        mProto = LogProtos.Entry.parseFrom(bytes);
    }

    public LogEntry(LogProtos.Entry proto) {
        mProto = proto;
    }

    public static ContentValues createContentValues(LogProtos.Entry proto) {
        ContentValues values = new ContentValues();

        if (proto.hasId()) {
            values.put(Columns.ID, proto.getId());
        }
        if (proto.hasNumber()) {
            values.put(Columns.NUMBER, proto.getNumber());
        }
        if (proto.hasTimestamp()) {
            values.put(Columns.DATE, proto.getTimestamp());
        }
        if (proto.hasExitAltitude()) {
            values.put(Columns.EXIT_ALTITUDE, proto.getExitAltitude());
        }
        if (proto.hasDeployAltitude()) {
            values.put(Columns.DEPLOY_ALTITUDE, proto.getDeployAltitude());
        }
        if (proto.hasFreefallTime()) {
            values.put(Columns.FREEFALL_TIME, proto.getFreefallTime());
        }
        if (proto.hasComments()) {
            values.put(Columns.COMMENTS, proto.getComments());
        }
        if (proto.hasDropzone()) {
            values.put(Columns.DROPZONE, proto.getDropzone());
        }
        if (proto.hasAircraft()) {
            values.put(Columns.AIRCRAFT, proto.getAircraft());
        }
        if (proto.hasJumpType()) {
            values.put(Columns.JUMPTYPE, proto.getJumpType());
        }
        if (proto.hasEquipment()) {
            values.put(Columns.EQUIPMENT, proto.getEquipment());
        }
        return values;
    }

    public ContentValues createContentValues() {
        return createContentValues(mProto);
    }

    public static Uri getUri(Context context, int id) {
        return ContentUris.withAppendedId(Columns.getContentUri(context), id);
    }

    public Uri getUri(Context context) {
        return getUri(context, mProto.getId());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        byte[] bytes = mProto.toByteArray();
        dest.writeInt(bytes.length);
        dest.writeByteArray(bytes);
    }

    public LogProtos.Entry getProto() {
        return mProto;
    }
}
