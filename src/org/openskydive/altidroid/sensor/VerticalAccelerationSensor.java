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
import android.util.FloatMath;

public class VerticalAccelerationSensor implements SensorEventListener {
    private final VerticalAccelerationListener mListener;
    private final SensorManager mSensorManager;

    private final Sensor mGravity;
    private final Sensor mAcceleration;

    private final float mGravityVector[];

    public VerticalAccelerationSensor(SensorManager sm, VerticalAccelerationListener listener) {
        mListener = listener;
        mSensorManager = sm;

        mGravityVector = new float[] { 0, 0, 0 };

        mGravity = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sm.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);

        mAcceleration = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, mAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mGravity) {
            updateGravityVector(event.values);
        } else if (event.sensor == mAcceleration) {
            float v[] = event.values;
            float acceleration =
                    mGravityVector[0] * v[0] +
                    mGravityVector[1] * v[1] +
                    mGravityVector[2] * v[2];
            mListener.update(acceleration);
        }
    }

    private void updateGravityVector(float[] values) {
        float l = FloatMath.sqrt(
                values[0] * values[0] +
                values[1] * values[1] +
                values[2] * values[2]);
        mGravityVector[0] = values[0] / l;
        mGravityVector[1] = values[1] / l;
        mGravityVector[2] = values[2] / l;
    }
}
