// This file is derived from android source code.

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openskydive.altidroid.ui;

import org.openskydive.altidroid.AlarmPlayer;
import org.openskydive.altidroid.Preferences;
import org.openskydive.altidroid.R;
import org.openskydive.altidroid.util.ToneUri;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class VolumePreference extends SeekBarPreference implements
        View.OnKeyListener {
    private static final String TAG = "VolumePreference";

    /** May be null if the dialog isn't visible. */
    private SeekBarVolumizer mSeekBarVolumizer;

    private float mVolume;

    private AlarmPlayer mAlarmPlayer;

    private static class SavedState extends BaseSavedState {
        VolumeStore mVolumeStore = new VolumeStore();

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        public SavedState(Parcel source) {
            super(source);
            mVolumeStore.volume = source.readInt();
            mVolumeStore.originalVolume = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        VolumeStore getVolumeStore() {
            return mVolumeStore;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mVolumeStore.volume);
            dest.writeInt(mVolumeStore.originalVolume);
        }
    }

    /**
     * Turns a {@link SeekBar} into a volume control.
     */
    private class SeekBarVolumizer implements OnSeekBarChangeListener, Runnable {
        private final Context mContext;
        private final Handler mHandler = new Handler();

        private final AudioManager mAudioManager;
        private int mOriginalStreamVolume;

        private int mMaxVolume;
        private int mLastVolume = -1;
        private final SeekBar mSeekBar;
        private final Uri mSampleUri;

        private final ContentObserver mVolumeObserver = new ContentObserver(
                mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                if (mSeekBar != null) {
                    int volume = System.getInt(mContext.getContentResolver(),
                            System.VOLUME_SETTINGS[AlarmPlayer.SAMPLE_STREAM], -1);
                    // Works around an atomicity problem with volume updates
                    // TODO: Fix the actual issue, probably in AudioService
                    if (volume >= 0) {
                        mSeekBar.setProgress(volume);
                    }
                }
            }
        };

        public SeekBarVolumizer(Context context, SeekBar seekBar,
                float initialValue) {
            mContext = context;
            mAudioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            mSeekBar = seekBar;
            mVolume = initialValue;

            // TODO: pull this from alarm settings
            mSampleUri = ToneUri.getBuiltInUri(0);

            initSeekBar(seekBar);
        }

        public void changeVolumeBy(int amount) {
            mSeekBar.incrementProgressBy(amount);
            sample();
            postSetVolume(mSeekBar.getProgress());
        }

        private void initSeekBar(SeekBar seekBar) {
            mMaxVolume = mAudioManager.getStreamMaxVolume(AlarmPlayer.SAMPLE_STREAM);
            seekBar.setMax(mMaxVolume);
            mOriginalStreamVolume = mAudioManager.getStreamVolume(AlarmPlayer.SAMPLE_STREAM);
            seekBar.setProgress(Math.round(mVolume * mMaxVolume));
            seekBar.setOnSeekBarChangeListener(this);

            mContext.getContentResolver().registerContentObserver(
                    System.getUriFor(System.VOLUME_SETTINGS[AlarmPlayer.SAMPLE_STREAM]),
                    false, mVolumeObserver);

            postSetVolume(seekBar.getProgress());
            sample();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromTouch) {
            if (!fromTouch) {
                return;
            }

            postSetVolume(progress);

            stopSample();
            sample();
        }

        public void onRestoreInstanceState(VolumeStore volumeStore) {
            if (volumeStore.volume != -1) {
                mOriginalStreamVolume = volumeStore.originalVolume;
                mLastVolume = volumeStore.volume;
                postSetVolume(mLastVolume);
            }
        }

        public void onSaveInstanceState(VolumeStore volumeStore) {
            if (mLastVolume >= 0) {
                volumeStore.volume = mLastVolume;
                volumeStore.originalVolume = mOriginalStreamVolume;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (!mAlarmPlayer.isPlaying()) {
                sample();
            }
        }

        void postSetVolume(int progress) {
            // Do the volume changing separately to give responsive UI
            mLastVolume = progress;
            mHandler.removeCallbacks(this);
            mHandler.post(this);
        }

        public void revertVolume() {
            mAudioManager.setStreamVolume(AlarmPlayer.SAMPLE_STREAM, mOriginalStreamVolume, 0);
        }

        @Override
        public void run() {
            mAudioManager.setStreamVolume(AlarmPlayer.SAMPLE_STREAM, mLastVolume, 0);
        }

        private void sample() {
            if (mSampleUri != null) {
                mAlarmPlayer.playSample(mSampleUri);
            }
        }

        public void stop() {
            stopSample();
            mContext.getContentResolver().unregisterContentObserver(
                    mVolumeObserver);
            mSeekBar.setOnSeekBarChangeListener(null);
        }

        public void stopSample() {
            mAlarmPlayer.stop();
        }
    }

    public static class VolumeStore {
        public int volume = -1;
        public int originalVolume = -1;
    }

    public VolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (context instanceof Preferences) {
            mAlarmPlayer = ((Preferences)context).getAlarmPlayer();
        }
    }

    /**
     * Do clean up. This can be called multiple times!
     */
    private void cleanup() {
        if (mSeekBarVolumizer != null) {
            Dialog dialog = getDialog();
            if (dialog != null && dialog.isShowing()) {
                View view = dialog.getWindow().getDecorView()
                        .findViewById(R.id.seekbar);
                if (view != null) {
                    view.setOnKeyListener(null);
                }
                // Stopped while dialog was showing, revert changes
                mSeekBarVolumizer.revertVolume();
            }
            mSeekBarVolumizer.stop();
            mSeekBarVolumizer = null;
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekbar);
        mSeekBarVolumizer = new SeekBarVolumizer(getContext(), seekBar,
                mVolume);

        // grab focus and key events so that pressing the volume buttons in the
        // dialog doesn't also show the normal volume adjust toast.
        view.setOnKeyListener(this);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (mSeekBarVolumizer != null && positiveResult) {
            mVolume = (float) mSeekBarVolumizer.mLastVolume / mSeekBarVolumizer.mMaxVolume;
            Log.d(TAG, "Persisting volume as " + mVolume);
            persistFloat(mVolume);
        }

        mSeekBarVolumizer.revertVolume();
        cleanup();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // If key arrives immediately after the activity has been cleaned up.
        if (mSeekBarVolumizer == null) {
            return true;
        }
        boolean isdown = (event.getAction() == KeyEvent.ACTION_DOWN);
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if (isdown) {
                mSeekBarVolumizer.changeVolumeBy(-1);
            }
            return true;
        case KeyEvent.KEYCODE_VOLUME_UP:
            if (isdown) {
                mSeekBarVolumizer.changeVolumeBy(1);
            }
            return true;
        default:
            return false;
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getFloat(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
            Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);

        float def = -1;
        if (defaultValue != null && defaultValue.getClass().equals(Float.class)) {
            def = (Float) defaultValue;
        }
        mVolume = getPersistedFloat(def);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (mSeekBarVolumizer != null) {
            mSeekBarVolumizer.onRestoreInstanceState(myState.getVolumeStore());
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        if (mSeekBarVolumizer != null) {
            mSeekBarVolumizer.onSaveInstanceState(myState.getVolumeStore());
        }
        return myState;
    }
}