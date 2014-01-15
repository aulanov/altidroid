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

import java.io.FileNotFoundException;
import java.io.FileReader;

import org.openskydive.altidroid.sensor.Altimeter;
import org.openskydive.altidroid.skydive.Alarm;
import org.openskydive.altidroid.skydive.MockAltimeter;
import org.openskydive.altidroid.skydive.SkydiveController;
import org.openskydive.altidroid.skydive.SkydiveListener;
import org.openskydive.altidroid.skydive.SkydiveState;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class AltidroidService extends Service implements SkydiveListener {
    private final int NOTIFICATION = R.string.notification_text;

    static final String ACTION_FOREGROUND = "com.ulanov.altidroid.FOREGROUND";
    static final String MOCKLOG_EXTRA = "mocklog";

    private SkydiveController mController;
    private AlarmPlayer mAlarmPlayer;

    private boolean mForegrounded = false;
    private final IBinder mBinder = new LocalBinder();

    private WakeLock mWakeLock;

    private String mLastStatus;

    public class LocalBinder extends Binder {
        AltidroidService getService() {
            return AltidroidService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mController = new SkydiveController(this);
        assert(mAlarmPlayer == null);
        mAlarmPlayer = new AlarmPlayer(this);
        mController.registerListener(mAlarmPlayer);
        mController.registerListener(this);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Altidroid lock");

        Log.i("AltidroidService", "Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_FOREGROUND.equals(intent.getAction())) {
            myStartForeground();
        } else {
            myStopForeground();
        }
        if (intent.hasExtra(MOCKLOG_EXTRA)) {
            String log = intent.getStringExtra(MOCKLOG_EXTRA);
            Log.i("AltidroidService", "Replaying log: " + log);
            try {
                FileReader reader = new FileReader(log);
                Altimeter altimeter = new MockAltimeter(this, reader);
                mController.setAltimeter(altimeter);
                altimeter.start();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                stopSelf();
            }
        }

        Log.i("AltidroidService", "Start command: " + intent.toString());
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.i("AltidroidService", "Stopped");

        myStopForeground();
        mController.removeListener(mAlarmPlayer);
        mController.stop();
        mAlarmPlayer.shutdown();
        mAlarmPlayer = null;

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void showNotification(String status) {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(getString(R.string.notification_text));
        builder.setSmallIcon(R.drawable.ic_launcher);
        if (status != null) {
            builder.setContentText(status);
        }
        mLastStatus = status;
        builder.setContentIntent(PendingIntent.getActivity(this, 0,
                new Intent(this, AltidroidActivity.class), 0));

        startForeground(NOTIFICATION, builder.getNotification());
    }

    public SkydiveController getController() {
        return mController;
    }

    public boolean isForegrounded() {
        return mForegrounded;
    }

    public void myStartForeground() {
        showNotification(null);
        mForegrounded = true;
        mController.startLog();
        mWakeLock.acquire();
    }

    public void myStopForeground() {
        mForegrounded = false;
        mController.stopLog();
        stopForeground(true);
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    public void update(SkydiveState state) {
        // TODO: localize the name
        if (mForegrounded && !state.getType().name().equals(mLastStatus)) {
            showNotification(state.getType().name());
        }
    }

    @Override
    public void alarm(Alarm alarm) {
    }
}
