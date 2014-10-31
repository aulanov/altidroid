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
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class BarometricAltimeter extends AltimeterBase implements SensorEventListener {
    private static final double NORMAL_PRESSURE = 1013.25;
    private static final double BAROMETRIC_CONSTANT = 44330;
    private static final double EXPONENTIAL_COEFFICIENT = 1 / 5.256;
    private static final int SENSOR_DELAY_US = 150000;  // 150ms
    private final Sensor mBarometer;
    private final SensorManager mSensorManager;

    public BarometricAltimeter(SensorManager sensorManager) {
        super();
        mSensorManager = sensorManager;
        mBarometer = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    }

    public void start() {
        mSensorManager.registerListener(this, mBarometer, SENSOR_DELAY_US);
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    public boolean supported() {
        return mBarometer != null;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        double pressure = event.values[0];
        int altitude = (int) (1000 * BAROMETRIC_CONSTANT * (
                1 - Math.pow(pressure / NORMAL_PRESSURE, EXPONENTIAL_COEFFICIENT)));
        // TODO: measure noise level in the input signal and use that to determine accuracy.
        notifyListeners(event, altitude, 0);
    }

}
