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

import java.text.DateFormat;
import java.util.Date;

import org.openskydive.altidroid.AltidroidActivity;
import org.openskydive.altidroid.R;
import org.openskydive.altidroid.log.LogEntry;
import org.openskydive.altidroid.log.LogProtos.Entry;
import org.openskydive.altidroid.util.Units;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class JumpInfoActivity extends FragmentActivity {
    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            reload();
        }
    }

    public static final String POSITION_INTENT_EXTRA = "intent.extra.log_entry";

    private JumpInfoPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private final MyContentObserver mContentObserver = new MyContentObserver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jump_info_activity);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(
                actionBar.getDisplayOptions() | ActionBar.DISPLAY_HOME_AS_UP);

        mPagerAdapter = new JumpInfoPagerAdapter(this, getFragmentManager(), mContentObserver);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);

        processIntent(getIntent());
    }

    public void reload() {
        int curItem = mViewPager.getCurrentItem();
        mPagerAdapter = new JumpInfoPagerAdapter(this, getFragmentManager(), mContentObserver);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(curItem);
    }

    private void processIntent(Intent intent) {
        if (intent != null) {
            mViewPager.setCurrentItem(intent.getIntExtra(POSITION_INTENT_EXTRA, 0));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent upIntent = new Intent(this, AltidroidActivity.class);
            if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                TaskStackBuilder.create(this).addNextIntent(upIntent).startActivities();
                finish();
            } else {
                NavUtils.navigateUpTo(this, upIntent);
            }
            return true;
        } else if (item.getItemId() == R.id.delete) {
            deleteCurJump();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteCurJump() {
        final int jumpId = mPagerAdapter.getJumpId(mViewPager.getCurrentItem());
        new AlertDialog.Builder(this)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setTitle(R.string.alert_dialog_delete_jump_confirmation)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    getContentResolver().delete(LogEntry.Columns.getContentUri(JumpInfoActivity.this),
                                                LogEntry.Columns.ID + "=?",
                                                new String[] { Integer.toString(jumpId) });
                }
            })
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    /* User clicked Cancel so do some stuff */
                }
            })
            .show();

    }

    private static class JumpInfoPagerAdapter extends FragmentStatePagerAdapter {
        Cursor mCursor;

        public JumpInfoPagerAdapter(Context context, FragmentManager fragmentManager,
                MyContentObserver contentObserver) {
            super(fragmentManager);

            mCursor = context.getContentResolver().query(LogEntry.Columns.getContentUri(context),
                    LogEntry.Columns.QUERY_COLUMNS,
                    null, null, LogEntry.Columns.NUMBER);
            mCursor.registerContentObserver(contentObserver);
        }

        public int getJumpId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getInt(mCursor.getColumnIndex(LogEntry.Columns.ID));
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            mCursor.moveToPosition(position);

            int colId = mCursor.getColumnIndex(LogEntry.Columns.NUMBER);
            return "JUMP " + mCursor.getInt(colId);
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            Fragment fragment = new JumpInfoFragment();
            Bundle args = new Bundle();
            args.putParcelable(JumpInfoFragment.ARG_ENTRY, new LogEntry(mCursor));
            fragment.setArguments(args);
            return fragment;
        }
    }

    public static class JumpInfoFragment extends Fragment {
        public static final String ARG_ENTRY = "entry";

        LogEntry mLogEntry;

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);

            inflater.inflate(R.menu.jump_info_activity, menu);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            setHasOptionsMenu(true);

            View rootView = inflater.inflate(R.layout.jump_info_fragment, container, false);
            Bundle args = getArguments();
            mLogEntry = args.getParcelable(ARG_ENTRY);

            Entry proto = mLogEntry.getProto();

            Typeface face = Typeface.createFromAsset(getActivity().getAssets(),
                    "fonts/MedulaOne-Regular.ttf");
            ((TextView) rootView.findViewById(R.id.number)).setTypeface(face);

            Units units = Units.getInstance(getActivity());
            DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(getActivity());
            DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());
            ((TextView) rootView.findViewById(R.id.number)).setText(
                    Integer.toString(proto.getNumber()));
            ((TextView) rootView.findViewById(R.id.date)).setText(
                    dateFormat.format(new Date(proto.getTimestamp())));
            ((TextView) rootView.findViewById(R.id.time)).setText(
                    timeFormat.format(new Date(proto.getTimestamp())));
            ((TextView) rootView.findViewById(R.id.jump_type)).setText(
                    proto.getJumpType());
            ((TextView) rootView.findViewById(R.id.drop_zone)).setText(
                    proto.getDropzone());
            ((TextView) rootView.findViewById(R.id.equipment)).setText(
                    proto.getEquipment());
            ((TextView) rootView.findViewById(R.id.aircraft)).setText(
                    proto.getAircraft());
            ((TextView) rootView.findViewById(R.id.exit_at)).setText(
                    Integer.toString(units.toUserUnits(proto.getExitAltitude())));
            ((TextView) rootView.findViewById(R.id.exit_units)).setText(
                    units.getUnitsNameShort());
            ((TextView) rootView.findViewById(R.id.deploy_at)).setText(
                    Integer.toString(units.toUserUnits(proto.getDeployAltitude())));
            ((TextView) rootView.findViewById(R.id.deploy_units)).setText(
                    units.getUnitsNameShort());
            ((TextView) rootView.findViewById(R.id.delay)).setText(
                    Integer.toString(proto.getFreefallTime() / 1000));
            ((TextView) rootView.findViewById(R.id.comments)).setText(
                    proto.getComments());

            return rootView;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == R.id.edit) {
                Intent i = new Intent(getActivity(), JumpInfoEdit.class);
                i.putExtra(JumpInfoEdit.ENTRY_INTENT_EXTRA, mLogEntry);
                startActivity(i);
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

}
