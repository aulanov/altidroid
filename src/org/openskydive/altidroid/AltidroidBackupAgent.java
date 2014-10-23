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

import org.openskydive.altidroid.log.LogEntry;
import org.openskydive.altidroid.log.LogProtos;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

public class AltidroidBackupAgent extends BackupAgentHelper {
    private final DatabaseBackupHelper.DbAdaptor mLogAdapter = new DatabaseBackupHelper.DbAdaptor() {
        @Override
        public ContentValues Unserialise(byte[] data) {
            LogProtos.Entry proto;
            try {
                proto = LogProtos.Entry.parseFrom(data);
            } catch (InvalidProtocolBufferException e) {
                Log.w("AltidroidBackup", "Cannot unserialize", e);
                throw new RuntimeException(e);
            }

            return LogEntry.createContentValues(proto);
        }

        @Override
        public byte[] Serialize(Cursor cursor) {
            return new LogEntry(cursor).getProto().toByteArray();
        }
    };

    @Override
    public void onCreate() {
        addHelper("prefs",
                new SharedPreferencesBackupHelper(this, Preferences.PREFS_NAME));
        addHelper("log",
                new DatabaseBackupHelper(this, LogEntry.Columns.getContentUri(this), mLogAdapter));

        super.onCreate();
    }
}
