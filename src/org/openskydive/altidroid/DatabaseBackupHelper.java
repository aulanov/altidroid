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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class DatabaseBackupHelper implements BackupHelper {
    public interface DbAdaptor {
        byte[] Serialize(Cursor cursor);
        ContentValues Unserialise(byte[] data);
    }

    private static final String ID_COLUMN = "_id";
    private static final String[] ID_COLUMNS = new String[] { ID_COLUMN };

    private final Context mContext;
    private final Uri mUri;
    private final DbAdaptor mAdaptor;

    public DatabaseBackupHelper(Context context, Uri contentUri, DbAdaptor adaptor) {
        mContext = context;
        mUri = contentUri;
        mAdaptor = adaptor;
    }

    @Override
    public void performBackup(ParcelFileDescriptor oldState,
            BackupDataOutput data, ParcelFileDescriptor newState) {
        ContentResolver contentResolver = mContext.getContentResolver();

        int[] dbIds = readDbIds(true);
        Arrays.sort(dbIds);
        int[] oldIds = readOldIds(oldState);
        Arrays.sort(oldIds);

        StringBuilder toBackUp = new StringBuilder();
        int dbPos = 0;
        int oldPos = 0;
        while (dbPos < dbIds.length || oldPos < oldIds.length) {
            int db = (dbPos < dbIds.length) ? dbIds[dbPos] : Integer.MAX_VALUE;
            int old = (oldPos < oldIds.length) ? oldIds[oldPos] : Integer.MAX_VALUE;
            if (db < old) {
                // The row is in the db but not backed up: back up now.
                if (toBackUp.length() > 0) toBackUp.append(',');
                toBackUp.append(db);
                dbPos++;
            } else if (old < db) {
                try {
                    data.writeEntityHeader("row_" + old, -1);
                } catch (IOException e) {
                    // WTF?
                }
                oldPos++;
            } else {
                dbPos++;
                oldPos++;
            }
        }

        if (toBackUp.length() > 0) {
            Cursor cursor = contentResolver.query(
                    mUri, null, "_id IN (" + toBackUp.toString() + ")", null, null);
            backupCursor(data, contentResolver, cursor);
        }


        {
            // Backup rows with backed_up == null (i.e. those which have been backed up
            // before but have changed).
            Cursor cursor = contentResolver.query(
                    mUri, null, "backed_up is null", null, null);

            backupCursor(data, contentResolver, cursor);
        }

        writeNewStateDescription(dbIds, newState);
    }

    private void backupCursor(BackupDataOutput data,
            ContentResolver contentResolver, Cursor cursor) {
        StringBuilder backedUp = new StringBuilder();
        int idColumn = cursor.getColumnIndex(ID_COLUMN);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(idColumn);
            byte[] serialized = mAdaptor.Serialize(cursor);
            try {
                data.writeEntityHeader("row_" + id, serialized.length);
                data.writeEntityData(serialized, serialized.length);
                if (backedUp.length() != 0) backedUp.append(',');
                backedUp.append(id);
            } catch (IOException e) {
                Log.e("AltidroidBackup", "cannot backup: " + id);
                e.printStackTrace();
            }
        }

        // Mark all entries that were just backed up as such
        if (backedUp.length() > 0) {
            ContentValues values = new ContentValues();
            values.put("backed_up", true);
            contentResolver.update(mUri, values, "_id IN (" + backedUp.toString() + ")", null);
        }
    }

    public List<Integer> asList(final int[] is)
    {
        return new AbstractList<Integer>() {
            @Override
            public Integer get(int i) { return is[i]; }
            @Override
            public int size() { return is.length; }
        };
    }

    @Override
    public void restoreEntity(BackupDataInputStream data) {
        byte[] buf = new byte[data.size()];
        try {
            data.read(buf);
            ContentValues values = mAdaptor.Unserialise(buf);
            mContext.getContentResolver().insert(mUri, values);
        } catch (IOException e) {
            Log.e("AltidroidBackup", "Unable to read backup value: " + data.getKey());
            e.printStackTrace();
        }
    }

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState) {
        writeNewStateDescription(readDbIds(true), newState);
    }

    public void writeNewStateDescription(int[] backedUpIds, ParcelFileDescriptor newState) {
        FileOutputStream outstream = new FileOutputStream(newState.getFileDescriptor());
        DataOutputStream out = new DataOutputStream(outstream);

        try {
            out.writeInt(backedUpIds.length);
            for (int id : backedUpIds) {
                out.writeInt(id);
            }
        } catch (IOException e) {
            Log.e("AltidroidBackup", "Unable to write backup state.");
            e.printStackTrace();
        }
    }

    private int[] readOldIds(ParcelFileDescriptor oldState) {
        if (oldState == null) {
            return new int[0];
        }
        FileInputStream instream = new FileInputStream(oldState.getFileDescriptor());
        DataInputStream in = new DataInputStream(instream);

        try {
            int size = in.readInt();
            int[] result = new int[size];
            for (int i = 0; i < size; i++) {
                result[i] = in.readInt();
            }
            return result;
        } catch (IOException e) {
            Log.e("AltidroidBackup", "Unable to read previous backup state.");
            e.printStackTrace();
            return new int[0];
        }
    }

    private int[] readDbIds(boolean backedupOnly) {
        Cursor cursor = mContext.getContentResolver().query(mUri, ID_COLUMNS,
                backedupOnly ? "backed_up is not null" : null, null, null);
        if (cursor == null) {
            return new int[0];
        }

        int idColumn = cursor.getColumnIndex(ID_COLUMN);
        int[] result = new int[cursor.getCount()];
        int pos = 0;
        while(cursor.moveToNext()) {
            result[pos++] = cursor.getInt(idColumn);
        }
        cursor.close();
        return result;
    }
}
