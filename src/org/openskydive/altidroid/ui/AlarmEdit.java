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
package org.openskydive.altidroid.ui;

import java.util.Map;
import java.util.TreeMap;

import org.openskydive.altidroid.AlarmPlayer;
import org.openskydive.altidroid.R;
import org.openskydive.altidroid.skydive.Alarm;
import org.openskydive.altidroid.ui.VerticalSeekBar.OnSeekListener;
import org.openskydive.altidroid.util.ToneUri;
import org.openskydive.altidroid.util.Units;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class AlarmEdit extends Activity implements OnSeekListener, TextWatcher,
        View.OnClickListener, RadioGroup.OnCheckedChangeListener,
        Spinner.OnItemSelectedListener {
    public static final String ALARM_INTENT_EXTRA = "intent.extra.alarm";
    public static final String NEW_ALARM_INTENT_EXTRA = "intent.extra.new_alarm";

    private class ToneType {
        int mCheckboxId;
        Uri mUri;
        View mView;
        String mName;

        ToneType(int checkbox, View view) {
            mCheckboxId = checkbox;
            mView = view;
        }

        public void check() {
            mPicker.check(mCheckboxId);
        }

        public void setVisibility(int visibility) {
            mView.setVisibility(visibility);
        }
    }

    private static final int SEEK_MAX = 1000;
    private static final double LOG_C = 450f;

    private static final int GET_CONTENT_REQUEST = 1;
    private static final int RINGTONE_PICKER_REQUEST = 2;

    private Alarm mAlarm;
    private int mMinValue;
    private int mMaxValue;
    private boolean mLogariphmic = true;
    private double mConversionCoefficient;

    private VerticalSeekBar mSeekBar;
    private EditText mValue;
    private EditText mName;
    private TextView mUnits;
    private Button mOkButton;
    private Button mDeleteButton;
    private Button mCancelButton;
    private RadioGroup mPicker;

    private Map<Integer, ToneType> mToneTypes;
    private ToneType mCurToneType;
    private Button mPlayTtsMessage;
    private Button mPickSystemAlarm;
    private Button mPickCustomSound;
    private Spinner mBuiltInSpinner;
    private AlarmPlayer mAlarmPlayer;

    private boolean mBuiltInSoundItemSelectedInvoked = false;
    private TextView mCustomSoundName;
    private TextView mSystemAlarmName;
    private EditText mTtsMessage;
    private boolean mNewAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        processIntent(getIntent());

        setContentView(R.layout.alarm_dialog);
        setTitle(R.string.alarm_dialog_title);

        mName = (EditText) findViewById(R.id.name);
        mName.setText(mAlarm.getName());

        mValue = (EditText) findViewById(R.id.value);
        mValue.addTextChangedListener(this);

        mSeekBar = (VerticalSeekBar) findViewById(R.id.seekBar);
        mSeekBar.setOnSeekListener(this);
        mSeekBar.setMax(SEEK_MAX);

        if (!mNewAlarm) {
            int value = mAlarm.getValue();
            if (mAlarm.getType() != Alarm.Type.FREEFALL_DELAY) {
                value = Units.getInstance(this).toUserUnits(value);
            }
            mValue.setText(String.valueOf(value));

            mSeekBar.setProgress(valueToSeekPos(value));
        }

        mUnits = (TextView) findViewById(R.id.units_text);
        if (mAlarm.getType() == Alarm.Type.FREEFALL_DELAY) {
            mUnits.setText(R.string.units_s_short);
        } else {
            mUnits.setText(Units.getInstance(this).getUnitsNameShort());
        }

        mOkButton = (Button) findViewById(R.id.ok);
        mOkButton.setOnClickListener(this);

        mDeleteButton = (Button) findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);
        if (mNewAlarm) {
            mDeleteButton.setVisibility(View.GONE);
        }

        mCancelButton = (Button) findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(this);

        mTtsMessage = (EditText) findViewById(R.id.tts_message);

        mPlayTtsMessage = (Button) findViewById(R.id.play_tts_message);
        mPlayTtsMessage.setOnClickListener(this);

        mPickSystemAlarm = (Button) findViewById(R.id.pick_system_alarm);
        mPickSystemAlarm.setOnClickListener(this);

        mPickCustomSound = (Button) findViewById(R.id.pick_custom_sound);
        mPickCustomSound.setOnClickListener(this);

        mCustomSoundName = (TextView) findViewById(R.id.custom_sound_name);
        mSystemAlarmName = (TextView) findViewById(R.id.system_alarm_name);

        mToneTypes = new TreeMap<Integer, ToneType>();
        mToneTypes.put(ToneUri.TYPE_BUILT_IN, new ToneType(
                R.id.builtin_sound_button, findViewById(R.id.builtin_spinner)));
        mToneTypes.put(ToneUri.TYPE_TTS, new ToneType(R.id.tts_message_button,
                findViewById(R.id.tts_message_layout)));
        mToneTypes.put(ToneUri.TYPE_SYSTEM, new ToneType(
                R.id.system_alarm_button,
                findViewById(R.id.system_alarm_layout)));
        mToneTypes.put(ToneUri.TYPE_CUSTOM, new ToneType(
                R.id.custom_sound_button,
                findViewById(R.id.custom_sound_layout)));

        mBuiltInSpinner = (Spinner) findViewById(R.id.builtin_spinner);
        mPicker = (RadioGroup) findViewById(R.id.picker);
        initToneControl();
        mPicker.setOnCheckedChangeListener(this);

        mBuiltInSpinner.setOnItemSelectedListener(this);
    }

    private void initToneControl() {
        int toneType = -1;
        if (mAlarm.getRingtone() != null) {
            toneType = ToneUri.getType(mAlarm.getRingtone());
        }
        mCurToneType = mToneTypes.get(toneType);
        if (mCurToneType == null) {
            mCurToneType = mToneTypes.get(ToneUri.TYPE_BUILT_IN);
        } else {
            mCurToneType.mUri = mAlarm.getRingtone();
            mCurToneType.mName = mAlarm.getRingtoneName();
        }
        mCurToneType.setVisibility(View.VISIBLE);
        mCurToneType.check();

        switch (toneType) {
        case ToneUri.TYPE_BUILT_IN:
            int id = ToneUri.getBuiltInId(mCurToneType.mUri);
            mBuiltInSpinner.setSelection(id);
            mCurToneType.mUri = ToneUri.getBuiltInUri(id);
            mCurToneType.mName = ToneUri.getBuiltInUriName(this, id);
            break;
        case ToneUri.TYPE_TTS:
            mTtsMessage.setText(ToneUri.getTtsMessage(mCurToneType.mUri));
            break;
        case ToneUri.TYPE_SYSTEM:
            mSystemAlarmName.setText(mCurToneType.mName);
            break;
        case ToneUri.TYPE_CUSTOM:
            mCustomSoundName.setText(mCurToneType.mName);
            break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mAlarmPlayer.stop();
        mAlarmPlayer.shutdown();
        mAlarmPlayer = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAlarmPlayer = new AlarmPlayer(this);
    }

    private void processIntent(Intent intent) {
        if (intent.hasExtra(ALARM_INTENT_EXTRA)) {
            mAlarm = intent.getParcelableExtra(ALARM_INTENT_EXTRA);
        } else {
            Alarm.Type type = Alarm.Type.FREEFALL;
            if (intent.hasExtra(NEW_ALARM_INTENT_EXTRA)) {
                int x = intent.getIntExtra(NEW_ALARM_INTENT_EXTRA, 0);
                if (x >= 0 && x < Alarm.Type.values().length) {
                    type = Alarm.Type.values()[x];
                }
            }
            mAlarm = new Alarm(type);
            mNewAlarm = true;
        }
        switch (mAlarm.getType()) {
        case FREEFALL:
            if (Units.getInstance(this).getPreferred() == Units.Type.IMPERIAL) {
                mMinValue = 1000;
                mMaxValue = 13000;
            } else {
                mMinValue = 300;
                mMaxValue = 4000;
            }
            break;
        case FREEFALL_DELAY:
            mMinValue = 5;
            mMaxValue = 90;
            mLogariphmic = false;
            break;
        case CANOPY:
            if (Units.getInstance(this).getPreferred() == Units.Type.IMPERIAL) {
                mMinValue = 100;
                mMaxValue = 4000;
            } else {
                mMinValue = 50;
                mMaxValue = 1500;
            }
            break;
        }
        if (mLogariphmic) {
            mConversionCoefficient = (mMaxValue - mMinValue)
                    / (Math.exp(SEEK_MAX / LOG_C) - 1);
        }
    }

    @Override
    public void onSeek(int progress) {
        mValue.setText(String.valueOf(seekPosToValue(progress)));
    }

    private int valueToSeekPos(int value) {
        int pos;
        if (value < mMinValue) {
            pos = 0;
        } else if (value > mMaxValue) {
            pos = SEEK_MAX;
        } else if (mLogariphmic) {
            pos = (int) (LOG_C * Math.log((value - mMinValue)
                    / mConversionCoefficient + 1));
        } else { // Linear
            pos = (value - mMinValue) * SEEK_MAX / (mMaxValue - mMinValue);
        }
        return pos;
    }

    private int seekPosToValue(int pos) {
        if (mLogariphmic) {
            int value = mMinValue
                    + (int) ((Math.exp(pos / LOG_C) - 1) * mConversionCoefficient);

            if (value < 3000) { // TODO: this is units specific, fix it!
                value = value / 100 * 100;
            } else {
                value = value / 500 * 500;
            }
            return value;
        } else {
            int value = mMinValue + pos * (mMaxValue - mMinValue) / SEEK_MAX;
            value = value / 5 * 5;
            return value;
        }
    }

    @Override
    public void afterTextChanged(Editable text) {
        try {
            int value = Integer.parseInt(mValue.getText().toString());
            int pos = valueToSeekPos(value);
            mSeekBar.setProgress(pos);
            mSeekBar.onSizeChanged(mSeekBar.getWidth(), mSeekBar.getHeight(),
                    0, 0);
        } catch (NumberFormatException e) {
        }
    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
            int arg3) {
    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

    }

    @Override
    public void onClick(View view) {
        if (view == mOkButton) {
            if (!formToAlarm()) {
                return;
            }
            ContentValues values = mAlarm.createContentValues();
            if (mNewAlarm) {
                values.remove(Alarm.Columns.ID);
                getContentResolver().insert(Alarm.Columns.getContentUri(this), values);
            } else {
                getContentResolver()
                        .update(mAlarm.getUri(this), values, null, null);
            }
            finish();
        } else if (view == mDeleteButton) {
            // TODO: ask confirmation
            getContentResolver().delete(mAlarm.getUri(this), null, null);
            finish();
        } else if (view == mCancelButton) {
            finish();
        } else if (view == mPlayTtsMessage) {
            Uri uri = ToneUri.createTtsUri(mTtsMessage.getText().toString());
            mAlarmPlayer.playSample(uri);
        } else if (view == mPickSystemAlarm) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                    RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    mCurToneType.mUri);
            startActivityForResult(intent, RINGTONE_PICKER_REQUEST);
        } else if (view == mPickCustomSound) {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("audio/*");
            Intent c = Intent.createChooser(i, "Select soundfile");
            startActivityForResult(c, GET_CONTENT_REQUEST);
        }
    }

    private boolean formToAlarm() {
        // Validate.
        int value;
        try {
            value = Integer.parseInt(mValue.getText().toString());
        } catch (NumberFormatException e) {
            showError((mAlarm.getType() == Alarm.Type.FREEFALL_DELAY) ? R.string.error_no_delay_value
                    : R.string.error_no_altitude_value);
            return false;
        }
        if (mCurToneType == null
                || (mCurToneType != mToneTypes.get(ToneUri.TYPE_TTS) && mCurToneType.mUri == null)) {
            showError(R.string.error_no_tone_selected);
            return false;
        }

        // Convert
        mAlarm.setName(mName.getText().toString());

        if (mAlarm.getType() != Alarm.Type.FREEFALL_DELAY) {
            value = Units.getInstance(this).fromUserUnits(value);
        }
        mAlarm.setValue(value);
        if (mCurToneType == mToneTypes.get(ToneUri.TYPE_TTS)) {
            String message = mTtsMessage.getText().toString();
            mAlarm.setRingtoneUri(ToneUri.createTtsUri(message).toString());
            mAlarm.setRingtoneName(message);
        } else {
            mAlarm.setRingtoneUri(mCurToneType.mUri.toString());
            mAlarm.setRingtoneName(mCurToneType.mName);
        }
        return true;
    }

    private void showError(int msgId, Object... formatArgs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(msgId, formatArgs)).setNeutralButton(
                R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (mCurToneType != null)
            mCurToneType.setVisibility(View.GONE);
        if (checkedId == R.id.builtin_sound_button) {
            mCurToneType = mToneTypes.get(ToneUri.TYPE_BUILT_IN);
        } else if (checkedId == R.id.tts_message_button) {
            mCurToneType = mToneTypes.get(ToneUri.TYPE_TTS);
        } else if (checkedId == R.id.system_alarm_button) {
            mCurToneType = mToneTypes.get(ToneUri.TYPE_SYSTEM);
        } else if (checkedId == R.id.custom_sound_button) {
            mCurToneType = mToneTypes.get(ToneUri.TYPE_CUSTOM);
        }
        mCurToneType.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0 || data == null) {
            // User pressed "Cancel". Ignore.
            return;
        }
        if (requestCode == RINGTONE_PICKER_REQUEST) {
            Uri uri = Uri.parse(data.getExtras()
                    .get(RingtoneManager.EXTRA_RINGTONE_PICKED_URI).toString());
            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) {
                ToneType tone = mToneTypes.get(ToneUri.TYPE_SYSTEM);
                tone.mUri = uri;
                tone.mName = ringtone.getTitle(this);
                mSystemAlarmName.setText(tone.mName);
            }
        } else if (requestCode == GET_CONTENT_REQUEST) {
            Uri uri = data.getData();
            if (uri != null) {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this, uri);
                String title = mmr
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                mmr.release();
                if (title == null)
                    title = uri.toString();

                ToneType tone = mToneTypes.get(ToneUri.TYPE_CUSTOM);
                tone.mUri = uri;
                tone.mName = title;
                mCustomSoundName.setText(tone.mName);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        ToneType tone = mToneTypes.get(ToneUri.TYPE_BUILT_IN);
        tone.mUri = ToneUri.getBuiltInUri(position);
        tone.mName = ToneUri.getBuiltInUriName(this, position);
        if (!mBuiltInSoundItemSelectedInvoked) {
            mBuiltInSoundItemSelectedInvoked = true;
        } else {
            mAlarmPlayer.playSample(tone.mUri);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // Should never happen
    }
}
