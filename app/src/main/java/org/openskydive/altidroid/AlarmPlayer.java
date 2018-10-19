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

import java.io.IOException;
import java.util.HashMap;

import org.openskydive.altidroid.skydive.Alarm;
import org.openskydive.altidroid.skydive.SkydiveListener;
import org.openskydive.altidroid.skydive.SkydiveState;
import org.openskydive.altidroid.util.ToneUri;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;

public class AlarmPlayer implements SkydiveListener, TextToSpeech.OnInitListener,
        OnSharedPreferenceChangeListener, OnAudioFocusChangeListener {
    private static final int ALARM_STREAM = AudioManager.STREAM_ALARM;

    public static final int SAMPLE_STREAM = AudioManager.STREAM_MUSIC;

    private TextToSpeech mTts;
    private boolean mInitialized = false;
    private PlayHandler mCurHandler;
    private final Context mContext;
    private final AudioManager mAudioManager;

    private int mFreefallVolume;
    private int mCanopyVolume;
    private boolean mMuteMusic;

    private final SharedPreferences mPreferences;
    private boolean mHasAudioFocus;
    private boolean mAudioFocusRequested;

    private interface PlayHandler {
        void stop();

        boolean play(int stream, int volume);
    }

    private abstract class PlayHandlerBase implements PlayHandler {
        private int mOriginalVolume = -1;
        private int mStream;

        @Override
        public void stop() {
            if (mOriginalVolume >= 0) {
                mAudioManager.setStreamVolume(mStream, mOriginalVolume, 0);
            }
        }

        @Override
        public boolean play(int stream, int volume) {
            mStream = stream;
            mAudioManager.setStreamSolo(stream, true);
            if (volume >= 0) {
                mOriginalVolume = mAudioManager.getStreamVolume(stream);
                mAudioManager.setStreamVolume(stream, volume, 0);
            }
            boolean result = play(stream);
            mAudioManager.setStreamSolo(stream, false);
            return result;
        }

        public abstract boolean play(int stream);
    }

    private class MediaHandler extends PlayHandlerBase implements
            OnCompletionListener, OnSeekCompleteListener {
        private MediaPlayer mMediaPlayer;

        public MediaHandler(int resourceId) {
            AssetFileDescriptor afd = mContext.getResources().openRawResourceFd(resourceId);
            mMediaPlayer = new MediaPlayer();
            try {
                mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
            } catch (IllegalArgumentException e) {
                mMediaPlayer = null;
                Log.d("Altidroid", "Cannot open media", e);
            } catch (IllegalStateException e) {
                mMediaPlayer = null;
                Log.d("Altidroid", "Cannot open media", e);
            } catch (IOException e) {
                mMediaPlayer = null;
                Log.d("Altidroid", "Cannot open media", e);
            }
        }

        public MediaHandler(Uri uri) {
            mMediaPlayer = new MediaPlayer();
            try {
                mMediaPlayer.setDataSource(mContext, uri);
                mMediaPlayer.setLooping(false);
            } catch (IOException e) {
                mMediaPlayer = null;
                Log.d("Altidroid", "Cannot open media", e);
            }
        }

        @Override
        public boolean play(int stream) {
            if (mMediaPlayer == null) {
                return false;
            }
            try {
                mMediaPlayer.setAudioStreamType(stream);
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.setOnSeekCompleteListener(this);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            } catch (IllegalStateException e) {
                Log.e("Altidroid", "Cannot play media", e);
                return false;
            } catch (IOException e) {
                Log.e("Altidroid", "Cannot play media", e);
                return false;
            }
            return true;
        }

        @Override
        public void stop() {
            super.stop();
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            AlarmPlayer.this.stop();
        }

        @Override
        public void onSeekComplete(MediaPlayer mp) {
            // Some system alarms loop by default even when looping is disabled.
            // Here we stop the player when they finish.
            AlarmPlayer.this.stop();
        }
    }

    private class TTSHandler extends PlayHandlerBase implements OnUtteranceCompletedListener {
        private final String mMessage;

        public TTSHandler(String message) {
            mMessage = message;
        }

        @Override
        public void stop() {
            super.stop();
            mTts.stop();
        }

        @Override
        public boolean play(int stream) {
            if (mInitialized) {
                HashMap<String, String> params = new HashMap<String, String>();
                params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(stream));
                params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1");

                mTts.setOnUtteranceCompletedListener(this);
                mTts.speak(mMessage, TextToSpeech.QUEUE_FLUSH, params);
            } else {
                Log.w("Altidroid", "TTS is not initialized, can't play alarm: " + mMessage);
                return false;
            }
            return true;
        }

        @Override
        public void onUtteranceCompleted(String utteranceId) {
            AlarmPlayer.this.stop();
        }
    }

    public AlarmPlayer(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mTts = new TextToSpeech(context, this);

        mPreferences = Preferences.getPrefs(context);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        readPrefs();
    }

    private void readPrefs() {
        int maxVolume = mAudioManager.getStreamMaxVolume(ALARM_STREAM);

        // Defaults are set in resources by R.xml.settings.
        mFreefallVolume = Math.round(mPreferences.getFloat(Preferences.FREEFALL_VOLUME, -1) * maxVolume);
        mCanopyVolume = Math.round(mPreferences.getFloat(Preferences.CANOPY_VOLUME, -1) * maxVolume);
        mMuteMusic = mPreferences.getBoolean(Preferences.MUTE_MUSIC, true);
    }

    public void shutdown() {
        if (mHasAudioFocus) {
            mAudioManager.abandonAudioFocus(this);
            mHasAudioFocus = false;
            mAudioFocusRequested = false;
        }
        stop();
        mTts.stop();
        mTts.shutdown();
        mTts = null;
    }

    public void playSample(Uri uri) {
        mCurHandler = createHandler(uri);
        if (mCurHandler != null) {
            mCurHandler.play(SAMPLE_STREAM, -1);
        }
    }

    private PlayHandler createHandler(Uri uri) {
        int type = ToneUri.getType(uri);
        stop();
        switch (type) {
        case ToneUri.TYPE_BUILT_IN:
            return new MediaHandler(ToneUri.getBuiltIn(uri));
        case ToneUri.TYPE_TTS:
            return new TTSHandler(ToneUri.getTtsMessage(uri));
        case ToneUri.TYPE_CUSTOM:
        case ToneUri.TYPE_SYSTEM:
            return new MediaHandler(uri);
        }
        return null;
    }

    public void stop() {
        if (mCurHandler != null) {
            mCurHandler.stop();
            mCurHandler = null;
        }
    }

    private void playAlarm(Uri uri, int volume) {
        mCurHandler = createHandler(uri);
        if (mCurHandler == null || !mCurHandler.play(ALARM_STREAM, volume)) {
            mCurHandler = new MediaHandler(R.raw.beep_50_50);
            mCurHandler.play(ALARM_STREAM, volume);
        }
    }

    public boolean isPlaying() {
        return mCurHandler != null;
    }

    // SkydiveListener implementation

    @Override
    public synchronized void update(SkydiveState state) {
        if (mMuteMusic &&
                (state.getType() == SkydiveState.Type.FREEFALL ||
                        state.getType() == SkydiveState.Type.CANOPY)) {
            if (!mAudioFocusRequested) {
                mAudioFocusRequested = true;
                int result = mAudioManager.requestAudioFocus(
                        this, ALARM_STREAM, AudioManager.AUDIOFOCUS_GAIN);
                mHasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
            }
        } else if (mAudioFocusRequested) {
            mAudioManager.abandonAudioFocus(this);
            mHasAudioFocus = false;
            mAudioFocusRequested = false;
        }
    }

    @Override
    public void alarm(Alarm alarm) {
        int volume = ((alarm.getType() == Alarm.Type.CANOPY) ? mCanopyVolume : mFreefallVolume);
        playAlarm(alarm.getRingtone(), volume);
    }

    // TextToSpeech.OnInitListener

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.ERROR) {
            Log.e("Altidroid", "TTS initialization error");
        } else {
            mInitialized = true;
        }
    }

    // SharedPreferences.OnSharedPreferenceChangedListener

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        readPrefs();
    }

    // AudioManagerOnAudioFocusChangeListener

    @Override
    public synchronized void onAudioFocusChange(int focusChange) {
        mHasAudioFocus = (focusChange == AudioManager.AUDIOFOCUS_GAIN);
    }
}
