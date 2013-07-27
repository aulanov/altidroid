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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.openskydive.altidroid.sensor.AltimeterBase;
import org.openskydive.altidroid.sensor.AltitudeListener;
import org.openskydive.altidroid.sensor.AltitudeListener.Update;
import org.openskydive.altidroid.util.Util;

import android.content.Context;
import android.os.SystemClock;

public class MockAltimeter extends AltimeterBase {
    BufferedReader mInput;
    Thread mThread;
    boolean mMockTimestamps = false;

    private class ReplayThread extends Thread {
        @Override
        public void run() {
            String line;
            try {
                long lastTimestamp = -1;
                while (!interrupted() && (line = mInput.readLine()) != null) {
                    Update update = newReading(line);
                    if (lastTimestamp > 0) {
                        sleep(update.getTimestamp() - lastTimestamp);
                    }
                    lastTimestamp = update.getTimestamp();
                    notifyListeners(new Update(null, SystemClock.elapsedRealtime(),
                            update.getAltitude(), update.getAccuracy()));
                }
            } catch (IOException e) {
                Util.fatalError(e);
            } catch (InterruptedException e) {
            }
        }
    };

    public MockAltimeter(Context context, Reader input) {
        mInput = new BufferedReader(input);
        mThread = new ReplayThread();
    }

    public Update newReading(String line) {
        String[] parts = line.split(" ");
        if (parts.length < 2) {
            Util.fatalError(new IOException("Bad line: " + line));
            return null; // Not reachable.
        } else {
            long timestamp = Long.parseLong(parts[0]);
            int altitude = Integer.parseInt(parts[1]);
            return new AltitudeListener.Update(null, timestamp, altitude, 1);
        }
    }

    public boolean supported() {
        return true;
    }

    public void start() {
        mThread.start();
    }

    public void stop() {
        mThread.interrupt();
    }

    public void replayAll() {
        String line;
        try {
            while ((line = mInput.readLine()) != null) {
                Update update = newReading(line);
                notifyListeners(update);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
