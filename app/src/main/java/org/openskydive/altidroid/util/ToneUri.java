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
package org.openskydive.altidroid.util;

import org.openskydive.altidroid.R;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class ToneUri {
    public static final int TYPE_BUILT_IN = 0;
    public static final int TYPE_TTS = 1;
    public static final int TYPE_SYSTEM = 2;
    public static final int TYPE_CUSTOM = 3;

    public static final Uri BUILT_IN_URI =
            Uri.parse("content://org.openskydive.altidroid/tone");
    private static final String TTS_MESSAGE_PARAMETER = "message";

    public static int getType(Uri uri) {
        String authority = uri.getAuthority();
        if (authority.equals("org.openskydive.altidroid")) {
            String path = uri.getPathSegments().get(0);
            if (path.equals("tone")) {
                return TYPE_BUILT_IN;
            } else if (path.equals("tts")) {
                return TYPE_TTS;
            }
        } else if (authority.equals("media")) {
            String path = uri.getPathSegments().get(0);
            if (path.equals("internal")) {
                return TYPE_SYSTEM;
            } else if (path.equals("external")) {
                return TYPE_CUSTOM;
            }
        }
        return -1;
    }

    public static Uri getBuiltInUri(int id) {
        return ContentUris.withAppendedId(BUILT_IN_URI, id);
    }

    public static String getBuiltInUriName(Context context, int position) {
        return context.getResources().getTextArray(R.array.alarm_sounds)[position].toString();
    }

    public static int getBuiltIn(Uri uri) {
        int id = getBuiltInId(uri);
        switch(id) {
        case 0:
            return R.raw.beep_50_50;
        case 1:
            return R.raw.beep_100_90;
        case 2:
            return R.raw.beep_constant;
        }
        return R.raw.beep_50_50;
    }

    public static int getBuiltInId(Uri uri) {
        try {
            return Integer.parseInt(uri.getPathSegments().get(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String getTtsMessage(Uri uri) {
        return uri.getQueryParameter(TTS_MESSAGE_PARAMETER);
    }

    public static Uri createTtsUri(String message) {
        Uri result = Uri.parse(String.format("content://org.openskydive.altidroid/tts?%s=%s",
                TTS_MESSAGE_PARAMETER, Uri.encode(message)));
        Log.i("Altidroid", result.toString());
        return result;
    }
}
