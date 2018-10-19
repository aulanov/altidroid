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

import java.util.Date;

import org.openskydive.altidroid.log.LogEntry;
import org.openskydive.altidroid.log.LogProtos;
import org.openskydive.altidroid.util.Units;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class JumpInfo {

    /**
     * Ground level in millimeters. Valid in all state types except UNKNOWN.
     */
    private int mGroundLevel;

    /**
     * Take off time stamp. Only valid in CLIMP, FREEFALL and CANOPY states.
     */
    private long mTakeOffTime;

    /**
     * Exit altitude. Only valid in FREEFALL and CANOPY states.
     */
    private int mExitAltitude;

    /**
     * Exit time. Only valid in FREEFALL and CANOPY states.
     */
    private long mExitTime;

    /**
     * Deploy time.
     */
    private long mDeployTime;

    /**
     * Canopy deploy altitude. Valid only in the CANOPY state.
     */
    private int mDeployAltitude;

    private Uri mLoggedUri;
    private LogEntry mLogEntry;

    public void setGroundLevel(int altitude) {
        mGroundLevel = altitude;
    }

    public void setTakeOffTime(long timestamp) {
        mTakeOffTime = timestamp;
    }

    public void setExitAltitude(int altitude) {
        mExitAltitude = altitude;
    }

    public void setExitTime(long timestamp) {
        mExitTime = timestamp;
    }

    public void setDeployAltitude(int altitude) {
        mDeployAltitude = altitude;
    }

    public int getGroundLevel() {
        return mGroundLevel;
    }

    public long getTakeOffTIme() {
        return mTakeOffTime;
    }

    public long getExitTime() {
        return mExitTime;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(" groundLevel: ");
        result.append(Units.toFoot(mGroundLevel));
        result.append(" exitTime: ");
        result.append(mExitTime);
        result.append(" exitAltitude: ");
        result.append(mExitAltitude);
        result.append(" deployAlt: ");
        result.append(Units.toFoot(mDeployAltitude));
        return result.toString();
    }

    public int getExitAltitude() {
        return mExitAltitude;
    }

    public int getDeployAltitude() {
        return mDeployAltitude;
    }

    public void setDeployTime(long timestamp) {
        mDeployTime = timestamp;
    }

    public long getDeployTime() {
        return mDeployTime;
    }

    private void updateLogEntryFields(LogProtos.Entry.Builder builder) {
        builder.setTimestamp(new Date().getTime());
        builder.setExitAltitude(getExitAltitude() - getGroundLevel());
        builder.setDeployAltitude(getDeployAltitude() - getGroundLevel());
        builder.setFreefallTime((int) (getDeployTime() - getExitTime()));
    }

    private LogEntry buildNewLogEntry(Context context, int jumpNumber, boolean autoFill) {
        LogProtos.Entry.Builder builder;
        int minJumpNumber = 1;
        LogEntry logEntry = LogEntry.readLastJump(context);
        if (logEntry == null) {
            builder = LogProtos.Entry.newBuilder();
        } else {
            if (!autoFill) {
                builder = LogProtos.Entry.newBuilder();
            } else {
                builder = LogProtos.Entry.newBuilder(logEntry.getProto());
                builder.clearId();
                builder.clearComments();
            }
            minJumpNumber = logEntry.getProto().getNumber() + 1;
        }
        builder.setNumber(Math.max(jumpNumber, minJumpNumber));
        updateLogEntryFields(builder);
        return new LogEntry(builder.build());
    }

    private LogEntry updateLogEntry(LogEntry logEntry) {
        LogProtos.Entry.Builder builder = LogProtos.Entry.newBuilder(logEntry.getProto());
        builder.clearId();
        updateLogEntryFields(builder);
        return new LogEntry(builder.build());
    }

    public Uri writeToLog(Context context, int jumpNumber, boolean autoFill) {
        if (mLoggedUri == null) {
            mLogEntry = buildNewLogEntry(context, jumpNumber, autoFill);
            Log.i("JumpInfo", "Logging: " + mLogEntry.getProto().toString());
            mLoggedUri = context.getContentResolver().insert(
                    LogEntry.Columns.getContentUri(context), mLogEntry.createContentValues());
        } else {
            mLogEntry = updateLogEntry(mLogEntry);
            Log.i("JumpInfo", "Re-Logging: " + mLogEntry.createContentValues().toString());
            context.getContentResolver().update(
                    mLoggedUri, mLogEntry.createContentValues(), null, null);
        }
        return mLoggedUri;
    }
}
