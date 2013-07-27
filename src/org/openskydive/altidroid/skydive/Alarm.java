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

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class Alarm implements Parcelable {
    public enum Type {
        FREEFALL,
        FREEFALL_DELAY,
        CANOPY,
    }

    public static class Columns {
        public static final Uri CONTENT_URI =
                Uri.parse("content://org.openskydive.altidroid/alarm");

        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String TYPE = "type";
        public static final String VALUE = "value";
        public static final String RINGTONE = "ringtone";
        public static final String RINGTONE_NAME = "ringtone_name";

        public static final String IS_FREEFALL = TYPE + " = " + Type.FREEFALL.ordinal();
        public static final String IS_FREEFALL_DELAY = TYPE + " = " + Type.FREEFALL_DELAY.ordinal();
        public static final String IS_CANOPY = TYPE + " = " + Type.CANOPY.ordinal();

        public static final String[] QUERY_COLUMNS = new String[] {
            ID,
            NAME,
            TYPE,
            VALUE,
            RINGTONE,
            RINGTONE_NAME
        };
    }

    public static class Reader {
        private final Cursor mCursor;
        private final int mRowIdColumn;
        private final int mNameColumn;
        private final int mTypeColumn;
        private final int mValueColumn;
        private final int mRingtoneColumn;
        private final int mRingtoneNameColumn;

        public Reader(Cursor cursor) {
            mCursor = cursor;

            mRowIdColumn = mCursor.getColumnIndex(Alarm.Columns.ID);
            mNameColumn = mCursor.getColumnIndex(Alarm.Columns.NAME);
            mTypeColumn = mCursor.getColumnIndex(Alarm.Columns.TYPE);
            mValueColumn = mCursor.getColumnIndex(Alarm.Columns.VALUE);
            mRingtoneColumn = mCursor.getColumnIndex(Alarm.Columns.RINGTONE);
            mRingtoneNameColumn = mCursor.getColumnIndex(Alarm.Columns.RINGTONE_NAME);
        }

        public Alarm read() {
            Type type;
            int intType = mCursor.getInt(mTypeColumn);
            if (intType == Type.FREEFALL.ordinal()) {
                type = Type.FREEFALL;
            } else if (intType == Type.FREEFALL_DELAY.ordinal()) {
                type = Type.FREEFALL_DELAY;
            } else if (intType == Type.CANOPY.ordinal()) {
                type = Type.CANOPY;
            } else {
                type = Type.FREEFALL;
            }

            return new Alarm(
                    mCursor.getInt(mRowIdColumn),
                    mCursor.getString(mNameColumn),
                    type,
                    mCursor.getInt(mValueColumn),
                    mCursor.getString(mRingtoneColumn),
                    mCursor.getString(mRingtoneNameColumn));
        }

        public int getRowId() {
            return mCursor.getInt(mRowIdColumn);
        }
    }

    public static final Parcelable.Creator<Alarm> CREATOR
    = new Parcelable.Creator<Alarm>() {
        @Override
        public Alarm createFromParcel(Parcel p) {
            return new Alarm(p);
        }

        @Override
        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    private final int mId;
    private String mName;
    private final Type mType;
    private int mValue;
    private String mRingtone;
    private String mRingtoneName;

    public Alarm(int id, String name, Type type,
            int value, String ringtone, String ringtoneName) {
        mId = id;
        mName = name;
        mType = type;
        mValue = value;
        mRingtone = ringtone;
        mRingtoneName = ringtoneName;
    }

    public Alarm(Cursor cursor) {
        mId = cursor.getInt(cursor.getColumnIndex(Columns.ID));
        mName = cursor.getString(cursor.getColumnIndex(Columns.NAME));
        mType = typeFromInt(cursor.getInt(cursor.getColumnIndex(Columns.TYPE)));
        mValue = cursor.getInt(cursor.getColumnIndex(Columns.VALUE));
        mRingtone = cursor.getString(cursor.getColumnIndex(Columns.RINGTONE));
        mRingtoneName = cursor.getString(cursor.getColumnIndex(Columns.RINGTONE_NAME));
    }

    public Alarm(Parcel p) {
        mId = p.readInt();
        mName = p.readString();
        mType = typeFromInt(p.readInt());
        mValue = p.readInt();
        mRingtone = p.readString();
        mRingtoneName = p.readString();
    }

    public Alarm(Type type) {
        mId = -1;
        mType = type;
        mValue = -1;
    }

    private static Type typeFromInt(int t) {
        if (Type.FREEFALL.ordinal() == t) {
            return Type.FREEFALL;
        } else if (Type.FREEFALL_DELAY.ordinal() == t) {
            return Type.FREEFALL_DELAY;
        } else {
            return Type.CANOPY;
        }
    }

    public String getName() {
        return mName;
    }

    public Type getType() {
        return mType;
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(int value) {
        mValue = value;
    }

    public void setName(String name) {
        mName = name;
    }

    public ContentValues createContentValues() {
        ContentValues values = new ContentValues(8);

        values.put(Columns.NAME, mName);
        values.put(Columns.TYPE, mType.ordinal());
        values.put(Columns.VALUE, mValue);
        values.put(Columns.RINGTONE, mRingtone);
        values.put(Columns.RINGTONE_NAME, mRingtoneName);

        return values;
    }

    public Uri getUri() {
        return ContentUris.withAppendedId(Columns.CONTENT_URI, mId);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("Name: ");
        out.append(mName);
        out.append(" Type: ");
        out.append(mType);
        out.append(" value: ");
        out.append(mValue);
        out.append(" ringtone: ");
        out.append(mRingtone);
        out.append(" ringtoneName: ");
        out.append(mRingtoneName);
        return out.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mName);
        dest.writeInt(mType.ordinal());
        dest.writeInt(mValue);
        dest.writeString(mRingtone);
        dest.writeString(mRingtoneName);
    }

    public String getRingtoneName() {
        return mRingtoneName;
    }

    public void setRingtoneUri(String uri) {
        mRingtone = uri;
    }

    public void setRingtoneName(String name) {
        mRingtoneName = name;
    }

    public Uri getRingtone() {
        return (mRingtone == null) ? null : Uri.parse(mRingtone);
    }

    public int getId() {
        return mId;
    }
}
