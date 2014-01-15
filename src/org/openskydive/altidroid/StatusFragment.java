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
package org.openskydive.altidroid;

import org.openskydive.altidroid.log.LogEntry;
import org.openskydive.altidroid.sensor.AltitudeListener.Update;
import org.openskydive.altidroid.skydive.Alarm;
import org.openskydive.altidroid.skydive.SkydiveListener;
import org.openskydive.altidroid.skydive.SkydiveState;
import org.openskydive.altidroid.skydive.SkydiveState.Type;
import org.openskydive.altidroid.util.Units;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.ToggleButton;

public class StatusFragment extends Fragment
        implements SkydiveListener, LoaderCallbacks<Cursor>,
                   OnSharedPreferenceChangeListener {
    private static final int NEXT_JUMP_LOADER = 0;

    private TextView mDebugText;

    private AltidroidService mBoundService;
    private ToggleButton mOnOffButton;
    private ImageView mStatusImage;
    private TextView mStatusText;
    private Button mNextJumpNumberButton;
    private SharedPreferences mPrefs;

    private int mMinNextJumpNumber = -1;
    private int mNextJumpNumber = -1;

    private Units mUnits;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            StatusFragment.this.onServiceConnected(((AltidroidService.LocalBinder) service).getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            StatusFragment.this.onServiceDisconnected();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.status_fragment, container, false);

        mDebugText = (TextView) rootView.findViewById(R.id.dbg_text);
        mOnOffButton = (ToggleButton) rootView.findViewById(R.id.onoff_button);
        mStatusImage = (ImageView) rootView.findViewById(R.id.status_image);
        mStatusText = (TextView) rootView.findViewById(R.id.status_text);
        mNextJumpNumberButton = (Button) rootView.findViewById(R.id.jump_number_button);

        mOnOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning()) {
                    stopAltidroidService();
                } else {
                    startAltidroidService();
                }
            }
        });

        mNextJumpNumberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showJumpNumberPicker();
            }
        });

        mPrefs = Preferences.getPrefs(getActivity());
        mNextJumpNumber = mPrefs.getInt(Preferences.NEXT_JUMP_NUMBER, -1);
        getLoaderManager().initLoader(NEXT_JUMP_LOADER, null, this);
        if (mNextJumpNumber > 0) {
            mNextJumpNumberButton.setText(Integer.toString(mNextJumpNumber));
        }
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        return rootView;
    }

    protected void showJumpNumberPicker() {
        LayoutInflater inflater = (LayoutInflater)
                getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.jump_number_dialog_layout, null);
        final NumberPicker numberPicker = (NumberPicker) view.findViewById(R.id.jump_number_picker);
        numberPicker.setMinValue(mMinNextJumpNumber);
        numberPicker.setMaxValue(30000);
        numberPicker.setValue(mNextJumpNumber);
        numberPicker.setWrapSelectorWheel(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.jump_number)
            .setView(view)
            .setPositiveButton(R.string.ok,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    updateNextJumpNumber(numberPicker.getValue());
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    protected void updateNextJumpNumber(int value) {
        mNextJumpNumber = value;
        mNextJumpNumberButton.setText(Integer.toString(mNextJumpNumber));

        Editor editor = mPrefs.edit();
        editor.putInt(Preferences.NEXT_JUMP_NUMBER, value);
        editor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        mUnits = Units.getInstance(getActivity());
        getActivity().bindService(new Intent(getActivity(), AltidroidService.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mBoundService != null) {
            mBoundService.getController().removeListener(this);
        }
        getActivity().unbindService(mConnection);
    }

    protected void startAltidroidService() {
        Intent intent = new Intent(AltidroidService.ACTION_FOREGROUND);
        intent.setClass(getActivity(), AltidroidService.class);
        getActivity().startService(intent);

        onServiceStateChanged(true);
    }

    protected void stopAltidroidService() {
        getActivity().stopService(new Intent(getActivity(), AltidroidService.class));
        mBoundService.myStopForeground();

        onServiceStateChanged(false);
    }

    protected void onServiceConnected(AltidroidService service) {
        mBoundService = service;
        mBoundService.getController().registerListener(StatusFragment.this);
        onServiceStateChanged(isServiceRunning());
    }

    private void onServiceStateChanged(boolean foreground) {
        if (foreground) {
            mOnOffButton.setText(R.string.on);
            mOnOffButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_on, 0, 0, 0);
        } else {
            mOnOffButton.setText(R.string.off);
            mOnOffButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_off, 0, 0, 0);
        }
    }

    protected void onServiceDisconnected() {
        mBoundService = null;
        onServiceStateChanged(false);
    }

    protected boolean isServiceRunning() {
        return mBoundService != null && mBoundService.isForegrounded();
    }

    /* SkydiveListener */
    @Override
    public void update(final SkydiveState state) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        updateStatusView(state);
                    }
                }
            });
        }
    }

    protected void updateStatusView(SkydiveState state) {
        mStatusImage.setImageResource(getStateResource(state.getType()));
        mStatusText.setText(formatStatusText(state));

        String text = state.toString();
        if (state.getType() != SkydiveState.Type.UNKNOWN) {
            text = text + "\n" + state.getRelativeAltitude();
            if (state.getType() == SkydiveState.Type.FREEFALL) {
                text = text + "\n" + state.getFreefallTime();
            }
        }
        if (Config.SHOW_DEBUG_INFO) {
            mDebugText.setText(text);
        }
    }

    private int getStateResource(Type stateType) {
        switch(stateType) {
        case  GROUND:
            return R.drawable.ic_status_ground;
        case CLIMB:
            return R.drawable.ic_status_climb;
        default:
            return R.drawable.ic_status_unknown;
        }
    }

    private CharSequence formatStatusText(SkydiveState state) {
        SpannableStringBuilder result = new SpannableStringBuilder();
        // TODO: translate
        result.append(state.getType().name());
        result.append("\n");

        Update altimeterUpdate = state.getAltimeterUpdate();

        Double pressure = altimeterUpdate.getPressure();
        if (pressure != null) {
            result.append(getString(R.string.status_pressure, pressure));
        }

        if (state.getType() == SkydiveState.Type.CLIMB) {
            result.append(getString(R.string.status_altitude,
                                    mUnits.formatAltitude(state.getRelativeAltitude())));
            result.append(getString(R.string.status_time_in_flight,
                    formatTime(state.getTimeInFlight())));

            result.append(getString(R.string.status_climb_rate,
                    mUnits.formatClimbRate(state.getSpeed())));

            // TODO: guess target altitude
            int target = Units.fromFoot(13000);
            result.append(getString(R.string.status_time_to_altitude,
                    mUnits.formatAltitude(target), guessTimeToAltitude(state, target)));
        } else {
            result.append(getString(R.string.status_pressure_altitude,
                    mUnits.formatAltitude(altimeterUpdate.getAltitude())));
        }
        return result;
    }

    private String formatTime(int time_ms) {
        int time_s = time_ms / 1000;

        if (time_s > 60) {
            int time_min = time_s / 60;
            return getString(R.string.time_interval_min, time_min);
        } else {
            return getString(R.string.time_interval_s, time_s);
        }
    }

    private String guessTimeToAltitude(SkydiveState state, int target) {
        if (state.getSpeed() > 0) {
            int time_ms = (int) ((target - state.getRelativeAltitude()) / state.getSpeed());
            if (time_ms < 1000 * 3600) {
                return formatTime(time_ms);
            }
        }
        return getString(R.string.status_time_to_altitude_unknown);
    }

    @Override
    public void alarm(Alarm alarm) {
        Log.i("Altidroid", "Alarm: " + alarm.toString());
    }

    // LoaderManager.LoaderCallbacks

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle args) {
        switch (loaderID) {
        case NEXT_JUMP_LOADER:
            return new CursorLoader(
                    getActivity(),
                    Uri.withAppendedPath(LogEntry.Columns.CONTENT_URI, "last"),
                    LogEntry.Columns.QUERY_COLUMNS,
                    null,
                    null,
                    LogEntry.Columns.NUMBER + " DESC");
        default:
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            LogEntry lastJump = new LogEntry(cursor);
            mMinNextJumpNumber = lastJump.getProto().getNumber() + 1;
        } else {
            mMinNextJumpNumber = 1;
        }
        if (mNextJumpNumber < mMinNextJumpNumber) {
            mNextJumpNumber = mMinNextJumpNumber;
            mNextJumpNumberButton.setText(Integer.toString(mNextJumpNumber));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
    }

    // OnSharedPreferenceChangeListener

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(Preferences.NEXT_JUMP_NUMBER)) {
            int newValue = mPrefs.getInt(Preferences.NEXT_JUMP_NUMBER, -1);
            if (newValue < 0) {
                getLoaderManager().initLoader(NEXT_JUMP_LOADER, null, this);
            } else {
                mNextJumpNumberButton.setText(Integer.toString(mNextJumpNumber));
            }
        }
    }
}
