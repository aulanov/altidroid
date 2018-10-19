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

import org.openskydive.altidroid.sensor.AltitudeListener.Update;
import org.openskydive.altidroid.util.Units;

public class SkydiveState {
    public enum Type {
        UNKNOWN, GROUND, CLIMB, FREEFALL, CANOPY
    }

    private final Update mUpdate;
    private final Type mType;

    /**
     * Current vertical speed in m/s = mm/ms.
     */
    private final float mSpeed;

    private final JumpInfo mJumpInfo;

    private final float mAcceleration;

    public SkydiveState(Update update, Type type, float speed, JumpInfo jumpInfo, float acceleration) {
        mUpdate = update;
        mType = type;
        mSpeed = speed;
        mJumpInfo = jumpInfo;
        mAcceleration = acceleration;
    }

    public Type getType() {
        return mType;
    }

    /**
     * @return current vertical speed in m/s = mm/ms.
     */
    public float getSpeed() {
        return mSpeed;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(mType.name());
        result.append(" altitude: ");
        result.append(Units.toFoot(mUpdate.getAltitude()));
        result.append(" speed: ");
        result.append(mSpeed);
        result.append(" timestamp: ");
        result.append(mUpdate.getTimestamp());
        result.append(" acceleration: ");
        result.append(mAcceleration);
        result.append(" jumpState: ");
        result.append(mJumpInfo.toString());
        return result.toString();
    }

    public int getRelativeAltitude() {
        return mUpdate.getAltitude() - mJumpInfo.getGroundLevel();
    }

    /**
     * @return time since take off in milliseconds
     */
    public int getTimeInFlight() {
        return (int) (mUpdate.getTimestamp() - mJumpInfo.getTakeOffTIme());
    }

    public int getFreefallTime() {
        return (int) (mUpdate.getTimestamp() - mJumpInfo.getExitTime());
    }

    public Update getAltimeterUpdate() {
        return mUpdate;
    }

    public String toLogString() {
        StringBuilder result = new StringBuilder(mUpdate.toString());
        result.append(" ");
        result.append(mType.name());
        result.append(" ");
        result.append(mAcceleration);
        return result.toString();
    }
}
