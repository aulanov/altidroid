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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openskydive.altidroid.skydive.SkydiveState;

public class LogWriter {
    private static final String PATH_BASE = "/sdcard/altidroid";

    private Writer mWriter;

    public LogWriter() {
        File path = new File(PATH_BASE);
        path.mkdir();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MMdd-HH:mm:ss");
        path = new File(path, format.format(new Date()) + ".log");
        try {
            mWriter = new FileWriter(path, true);
        } catch (IOException e) {
            e.printStackTrace();
            mWriter = null;
        }
    }


    public void close() {
    	if (mWriter == null) return;
        try {
            mWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(SkydiveState state) {
    	if (mWriter == null) return;
        try {
            mWriter.append(state.toLogString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
