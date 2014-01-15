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

import java.util.Set;

import org.openskydive.altidroid.Preferences;
import org.openskydive.altidroid.sensor.Altimeter;
import org.openskydive.altidroid.sensor.AltitudeHistory;
import org.openskydive.altidroid.sensor.AltitudeListener;
import org.openskydive.altidroid.sensor.BarometricAltimeter;
import org.openskydive.altidroid.sensor.GPSAltimeter;
import org.openskydive.altidroid.sensor.LogWriter;
import org.openskydive.altidroid.sensor.VerticalAccelerationListener;
import org.openskydive.altidroid.skydive.SkydiveState.Type;
import org.openskydive.altidroid.util.WeakSet;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;

public class SkydiveController implements AltitudeListener, VerticalAccelerationListener {
    private static final float M_PER_S = 1f;
    private static final int SEC = 1000;
    private static final int METERS = 1000;
    private static final float SPEED_FILTER_K = 0.1f;

    private final Context mContext;

    private JumpInfo mJumpInfo = new JumpInfo();
    private SkydiveState mState = new SkydiveState(
            new Update(null, 0, 0), SkydiveState.Type.UNKNOWN, 0, mJumpInfo, 0);
    private final AlarmList mAlarms;

    private Altimeter mMainAltimeter = null;
    private BarometricAltimeter mBarometricAltimeter;
    private GPSAltimeter mGpsAltimeter;
    //private final VerticalAccelerationSensor mAccelerationSensor;

    private LogWriter mLog;

    private final Set<SkydiveListener> mListeners = new WeakSet<SkydiveListener>();

    private final AltitudeHistory mHistory10 = new AltitudeHistory(10 * SEC, 0.5f);
    private final AltitudeHistory mHistory2 = new AltitudeHistory(2 * SEC, 0.3f);
    private float mSpeed = 0;
    private int mLowestAlarmAltitude = Integer.MAX_VALUE;
    private float mLastAcceleration;
    private final SharedPreferences mPrefs;

    public SkydiveController(Context context) {
        mContext = context;

        mAlarms = new AlarmList(context);

        SensorManager sm = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        mBarometricAltimeter = new BarometricAltimeter(sm);
        if (mBarometricAltimeter.supported()) {
            mBarometricAltimeter.start();
            mMainAltimeter = mBarometricAltimeter;
        } else {
            mGpsAltimeter = new GPSAltimeter(context);
            mGpsAltimeter.start();
            mMainAltimeter = mGpsAltimeter;
        }

        if (mMainAltimeter != null) {
            mMainAltimeter.registerListener(this);
        }

        mPrefs = Preferences.getPrefs(mContext);

        // TODO: uncomment when we know how to use this.
        //mAccelerationSensor = new VerticalAccelerationSensor(sm, this);
    }

    public void setAltimeter(Altimeter altimerter) {
        stop();

        mMainAltimeter = altimerter;
        mMainAltimeter.registerListener(this);

        mState = new SkydiveState(
                new Update(null, 0, 0), SkydiveState.Type.UNKNOWN, 0, mJumpInfo, 0);
    }

    public SkydiveState getState() {
        return mState;
    }

    public void stop() {
        stopLog();
        if (mBarometricAltimeter != null) {
            mBarometricAltimeter.stop();
            mBarometricAltimeter = null;
        }
        if (mGpsAltimeter != null) {
            mGpsAltimeter.stop();
            mGpsAltimeter = null;
        }
        if (mMainAltimeter != null) {
            mMainAltimeter.stop();
            mMainAltimeter = null;
        }
    }

    public void registerListener(SkydiveListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(SkydiveListener listener) {
        mListeners.remove(listener);
    }

    public void startLog() {
        if (mLog == null) {
            mLog = new LogWriter();
        }
    }

    public void stopLog() {
        if (mLog != null) {
            mLog.close();
            mLog = null;
        }
    }

    /* AltitudeListener */
    @Override
    public void update(Altimeter altimeter, Update update) {
        mHistory10.add(update, false);
        mHistory2.add(update, false);

        SkydiveState newState = determineNewState(mState, update);
        Alarm alarm = mAlarms.getAlarm(mState, newState, mLowestAlarmAltitude);
        mState = newState;

        if (mLog != null) {
            mLog.write(newState);
        }

        if (alarm != null) {
            for (SkydiveListener listener : mListeners) {
                listener.alarm(alarm);
            }
            if (alarm.getType() != Alarm.Type.FREEFALL_DELAY) {
                mLowestAlarmAltitude = alarm.getValue() - 1;
            }
        }
        for (SkydiveListener listener : mListeners) {
            listener.update(mState);
        }
    }

    private SkydiveState determineNewState(SkydiveState curState, Update update) {
        mSpeed = mHistory2.speed() * SPEED_FILTER_K + (1-SPEED_FILTER_K) * mSpeed;

        // UNKNOWN -> GROUND
        // UNKNOWN -> CLIMB // v.2, must use GPS?
        // GROUND -> CLIMB
        // CLIMB -> FREEFALL
        // CLIMB -> GROUND // v.2
        // FREEFALL -> CANOPY
        // CANOPY -> GROUND

        SkydiveState newState;

        SkydiveState.Type newType = curState.getType();
        Type curType = curState.getType();
        if ((curType == Type.UNKNOWN || curType == Type.GROUND) &&
                mHistory10.sampleTime() > 5 * SEC && mHistory10.maxVariation() < 2 * METERS) {
            // Reset ground level.
            newType = SkydiveState.Type.GROUND;
            mJumpInfo.setGroundLevel(mHistory10.averageAltitude());
        } else if (curState.getType() == SkydiveState.Type.GROUND &&
                mHistory10.speed() > 1 * M_PER_S) {
            // Switch to climb.
            mJumpInfo.setTakeOffTime(update.getTimestamp());
            newType = SkydiveState.Type.CLIMB;
        } else if (curType == Type.CLIMB && mSpeed < -25 * M_PER_S) {
            // Assume it takes 3 seconds to reach free-fall threshold speed (-25 m/s)
            mJumpInfo.setExitTime(update.getTimestamp() - 3000);
            mJumpInfo.setExitAltitude(mHistory10.maxAltitude());
            newType = SkydiveState.Type.FREEFALL;
        } else if ((curType == Type.FREEFALL || curType == Type.CANOPY) &&
                mSpeed < -25 * M_PER_S) {
            newType = SkydiveState.Type.FREEFALL;
        } else if (curType == Type.FREEFALL || curType == Type.CANOPY) {
            if (mHistory10.maxVariation() < 2 * METERS) {
                mLowestAlarmAltitude = Integer.MAX_VALUE;
                mJumpInfo = new JumpInfo();
                mJumpInfo.setGroundLevel(mHistory2.averageAltitude());
                newType = SkydiveState.Type.GROUND;
            } else if (mSpeed > -15 * M_PER_S) {
                if (curType == Type.FREEFALL) {
                    logCurrentJump(update);
                }
                newType = SkydiveState.Type.CANOPY;
            }
        }

        newState = new SkydiveState(update, newType, mSpeed, mJumpInfo, mLastAcceleration);

        return newState;
    }

    private void logCurrentJump(Update update) {
        mJumpInfo.setDeployAltitude(update.getAltitude());
        mJumpInfo.setDeployTime(update.getTimestamp());
        mJumpInfo.writeToLog(mContext,
                mPrefs.getInt(Preferences.NEXT_JUMP_NUMBER, -1),
                mPrefs.getBoolean(Preferences.LOG_AUTO_FILL, true));
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.remove(Preferences.NEXT_JUMP_NUMBER);
        editor.apply();
    }

    @Override
    public void update(float acceleration) {
        mLastAcceleration = acceleration;
    }
}
