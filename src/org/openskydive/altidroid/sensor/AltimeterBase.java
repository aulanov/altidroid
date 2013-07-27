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

import java.util.Iterator;
import java.util.Set;

import org.openskydive.altidroid.util.WeakSet;

public abstract class AltimeterBase implements Altimeter {

    private final Set<AltitudeListener> mListeners = new WeakSet<AltitudeListener>();

    public AltimeterBase() {
    }

    public void registerListener(AltitudeListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(AltitudeListener listener) {
        mListeners.remove(listener);
    }

    public void notifyListeners(Object base, int altitude, int accuracy) {
        notifyListeners(new AltitudeListener.Update(base, altitude, accuracy));
    }

    public void notifyListeners(AltitudeListener.Update update) {
        Iterator<AltitudeListener> i = mListeners.iterator();
        while (i.hasNext()) {
            i.next().update(this, update);
        }
    }
}
