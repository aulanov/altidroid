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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.SystemClock;

public interface AltitudeListener {
    static class Update {
        private final long mTimestamp;
        private final int mAltitude;
        private final int mAccuracy;
        private final Object mRow;

        public Update(Object base, int altitude, int accuracy) {
            this(base, SystemClock.elapsedRealtime(), altitude, accuracy);
        }

        public Update(Object raw, long timestamp, int altitude, int accuracy) {
            mTimestamp = timestamp;
            mRow = raw;
            mAltitude = altitude;
            mAccuracy = accuracy;
        }

        @Override
        public String toString() {
            StringBuffer result = new StringBuffer();
            result.append(mTimestamp);
            result.append(" ");
            result.append(mAltitude);
            result.append(" ");
            result.append(mAccuracy);
            result.append(" ");
            result.append(getPressure());
            return result.toString();
        }

        public long getTimestamp() {
            return mTimestamp;
        }

        public int getAltitude() {
            return mAltitude;
        }

        public int getAccuracy() {
            return mAccuracy;
        }

        /**
         * Returns pressure sensor reading if the update is received from
         * barometric altimeter. null otherwise.
         */
        public Double getPressure() {
            if (mRow != null && mRow instanceof SensorEvent) {
                SensorEvent event = (SensorEvent) mRow;
                if (event.sensor.getType() != Sensor.TYPE_PRESSURE) {
                    return null;
                }
                return (double) event.values[0];
            }
            return null;
        }
    }

    /**
     * @param mAltitude
     *            new altitude value in meters above ocean level
     * @param mAccuracy
     *            altitude accuracy
     */
    void update(Altimeter altimeter, Update update);
}
