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
package org.openskydive.altidroid.sensor;

import java.util.Deque;
import java.util.LinkedList;

import org.openskydive.altidroid.sensor.AltitudeListener.Update;

import android.util.Log;

public class AltitudeHistory {

    private static class MyUpdate {
        final long mTimestamp;
        final int mAltitude;

        public MyUpdate(long timestamp, int altitude) {
            mTimestamp = timestamp;
            mAltitude = altitude;
        }
    }

    private final Deque<MyUpdate> mHistory = new LinkedList<MyUpdate>();
    private final int mTime;

    private int mSum = 0;
    private MyUpdate mMax;
    private MyUpdate mMin;
    private final float mFilterCoef;

    public AltitudeHistory(int timeMsec, float filterCoef) {
        mTime = timeMsec;
        mFilterCoef = filterCoef;
    }

    public void add(Update update, boolean log) {
        int alt;
        if (mFilterCoef >= 1 || mHistory.size() == 0) {
            alt = update.getAltitude();
        } else {
            alt = (int) (mHistory.peekLast().mAltitude * (1-mFilterCoef) +
                    update.getAltitude() * mFilterCoef);
        }
        MyUpdate myUpd = new MyUpdate(update.getTimestamp(), alt);
        mHistory.add(myUpd);
        if (mMax == null || update.getAltitude() >= mMax.mAltitude) {
            mMax = myUpd;
        }
        if (mMin == null || update.getAltitude() <= mMin.mAltitude) {
            mMin = myUpd;
        }
        mSum += update.getAltitude();

        removeOld(update.getTimestamp() - mTime);

        if (log) {
            Log.i("AltitudeHistory", "new:" + update.getAltitude() +
                    " max:" + mMax.mAltitude +
                    " min:" + mMin.mAltitude +
                    " size:" + mHistory.size());
        }
    }

    private void removeOld(long oldestLimit) {
        MyUpdate update;
        while((update = mHistory.peek()) != null) {
            if (update.mTimestamp > oldestLimit) {
                break;
            }
            mHistory.remove();
            if (update == mMax) {
                mMax = null;
                for (MyUpdate i : mHistory) {
                    if (mMax == null || i.mAltitude >= mMax.mAltitude) {
                        mMax = i;
                    }
                }
            }
            if (update == mMin) {
                mMin = null;
                for (MyUpdate i : mHistory) {
                    if (mMin == null || i.mAltitude <= mMin.mAltitude) {
                        mMin = i;
                    }
                }
            }
            mSum -= update.mAltitude;
        }
    }

    public int sampleTime() {
        if (mHistory.isEmpty()) {
            return 0;
        } else {
            MyUpdate earliest = mHistory.peekFirst();
            MyUpdate latest = mHistory.peekLast();
            return (int) (latest.mTimestamp - earliest.mTimestamp);
        }
    }

    /**
     * @return maximum difference between readings and average
     */
    public int maxVariation() {
        int average = averageAltitude();
        if (mMax != null && mMin != null) {
            return Math.max(mMax.mAltitude - average, average - mMin.mAltitude);
        }
        return 0;
    }

    public int averageAltitude() {
        return mSum / mHistory.size();
    }

    public float speed() {
        if (mHistory.size() < 2) {
            return 0;
        } else {
            MyUpdate earliest = mHistory.peekFirst();
            MyUpdate latest = mHistory.peekLast();
            return (float) (latest.mAltitude - earliest.mAltitude)
                    / (latest.mTimestamp - earliest.mTimestamp);
        }
    }

    public int maxAltitude() {
        return mMax.mAltitude;
    }


}
