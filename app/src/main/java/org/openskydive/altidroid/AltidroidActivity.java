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

import org.openskydive.altidroid.ui.AlarmsFragment;
import org.openskydive.altidroid.ui.LogFragment;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;

public class AltidroidActivity extends FragmentActivity implements TabListener {
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {
        Context mContext;

        @IntDef({FRAGMENT_STATUS, FRAGMENT_ALARMS, FRAGMENT_LOG})
        public @interface FragmentId {}
        private static final int FRAGMENT_STATUS = 0;
        private static final int FRAGMENT_ALARMS = 1;
        private static final int FRAGMENT_LOG = 2;

        public AppSectionsPagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case FRAGMENT_STATUS:
                    return new StatusFragment();
                case FRAGMENT_ALARMS:
                    return new AlarmsFragment();
                case FRAGMENT_LOG:
                    return new LogFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
            case FRAGMENT_STATUS:
                return mContext.getString(R.string.status_title);
            case FRAGMENT_ALARMS:
                return mContext.getString(R.string.alarms_title);
            case FRAGMENT_LOG:
                return mContext.getString(R.string.log_title);
            }
            return null;
        }
    }

    AppSectionsPagerAdapter mAppSectionsPagerAdapter;
    ViewPager mViewPager;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mAppSectionsPagerAdapter =
                new AppSectionsPagerAdapter(this, getSupportFragmentManager());

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(actionBar.newTab()
                    .setText(mAppSectionsPagerAdapter.getPageTitle(i))
                    .setTabListener(this));
        }

        if (!checkPressureSensor()) {
            return;
        }

        SharedPreferences prefs = Preferences.getPrefs(this);
        if (prefs.getBoolean(Preferences.SHOW_WARNING, true)) {
            showWarning();
        }
    }

    private void showWarning() {
        final CheckBox checkBox = new CheckBox(this);
        checkBox.setText(R.string.first_time_warning_show_next_time);
        checkBox.setChecked(true);
        new AlertDialog.Builder(this)
        .setIconAttribute(android.R.attr.alertDialogIcon)
        .setTitle(R.string.first_time_warning_title)
        .setMessage(R.string.first_time_warning)
        .setView(checkBox)
        .setCancelable(false)
        .setPositiveButton(R.string.accept, (dialog, whichButton) -> {
            if (!checkBox.isChecked()) {
                Editor editor = Preferences.getPrefs(AltidroidActivity.this).edit();
                editor.putBoolean(Preferences.SHOW_WARNING, false);
                editor.apply();
            }
        })
        .setNegativeButton(R.string.reject, (dialog, whichButton) -> finish())
        .show();
    }

    private boolean checkPressureSensor() {
        if (BuildConfig.ALLOW_NO_SENSOR) {
            return true;
        }
        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor pressureSensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor == null) {
            showNoSensorWarning();
            return false;
        }
        return true;
    }

    protected void showNoSensorWarning() {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setMessage(R.string.no_pressure_sensor_warning)
            .setPositiveButton(R.string.ok, null)
            .create();

        dialog.setOnDismissListener(dialog1 -> finish());
        dialog.show();
  }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.preferences) {
            showPreferencesActivity();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected void showPreferencesActivity() {
        Intent intent = new Intent(this, Preferences.class);
        startActivity(intent);
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }
}
