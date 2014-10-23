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

import org.openskydive.altidroid.Preferences;
import org.openskydive.altidroid.R;
import org.openskydive.altidroid.skydive.Alarm;
import org.openskydive.altidroid.skydive.Alarm.Type;
import org.openskydive.altidroid.util.Units;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class AlarmsFragment
        extends ExpandableListFrament
        implements OnSharedPreferenceChangeListener {
    private LayoutInflater mLayoutInflater;
    private AlarmsListAdapter mAdapter;
    private Group mGroups[];

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
        Alarm alarm = (Alarm) mAdapter.getChild(groupPosition, childPosition);

        Intent i = new Intent(getActivity(), AlarmEdit.class);
        i.putExtra(AlarmEdit.ALARM_INTENT_EXTRA, alarm);
        startActivity(i);

        return true;
    }

    protected void addAlarm(Group group) {
        Intent i = new Intent(getActivity(), AlarmEdit.class);
        i.putExtra(AlarmEdit.NEW_ALARM_INTENT_EXTRA, group.mType.ordinal());
        startActivity(i);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLayoutInflater =
                (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mGroups = new Group[3];
        mGroups[0] = new Group(getString(R.string.freefall_group_title),
                Alarm.Type.FREEFALL,
                Alarm.Columns.IS_FREEFALL);
        mGroups[1] = new Group(getString(R.string.timer_group_title),
                Alarm.Type.FREEFALL_DELAY,
                Alarm.Columns.IS_FREEFALL_DELAY);
        mGroups[2] = new Group(getString(R.string.canopy_group_title),
                Alarm.Type.CANOPY,
                Alarm.Columns.IS_CANOPY);

        mAdapter = new AlarmsListAdapter();
        setListAdapter(mAdapter);

        ExpandableListView list = getExpandableListView();
        for (int i = 0; i < mGroups.length; i++) {
            list.expandGroup(i);
        }

        Preferences.getPrefs(getActivity()).registerOnSharedPreferenceChangeListener(this);
    }

    private class Group implements LoaderManager.LoaderCallbacks<Cursor> {
        private final String mName;
        private Cursor mCursor;
        private final String mFilter;

        private Alarm.Reader mReader;
        private final Type mType;

        public Group(String name, Type type, String filter) {
            mName = name;
            mType = type;
            mFilter = filter;

            getLoaderManager().initLoader(type.ordinal(), null, this);
        }

        public Object getChild(int position) {
            mCursor.moveToPosition(position);
            return mReader.read();
        }

        public long getChildId(int position) {
            mCursor.moveToPosition(position);
            return mReader.getRowId();
        }

        public View getChildView(int childPosition, View convertView, ViewGroup parent) {
            View layout;
            if (convertView == null) {
                layout = mLayoutInflater.inflate(R.layout.alarm_list_item, parent, false);
            } else {
                layout = convertView;
            }
            bindView(childPosition, layout);
            return layout;
        }

        private void bindView(int position, View layout) {
            mCursor.moveToPosition(position);
            Alarm alarm = mReader.read();

            TextView text = (TextView) layout.findViewById(R.id.text);
            text.setText(alarm.getName());

            TextView at = (TextView) layout.findViewById(R.id.at);
            if (alarm.getType() != Alarm.Type.FREEFALL_DELAY) {
                at.setText(Units.getInstance(getActivity()).formatAltitude(alarm.getValue()));
            } else {
                at.setText(alarm.getValue() + " " + getString(R.string.units_s_short));
            }

            TextView ringtone = (TextView) layout.findViewById(R.id.ringtone);
            ringtone.setText(alarm.getRingtoneName());
        }

        public int getChildrenCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }

        public String getName() {
            return mName;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
            return new CursorLoader(getActivity(), Alarm.Columns.getContentUri(getActivity()),
                    Alarm.Columns.QUERY_COLUMNS,
                    mFilter, null,
                    Alarm.Columns.VALUE + " DESC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            mCursor = cursor;
            mReader = new Alarm.Reader(cursor);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mCursor = null;
            mReader = null;
            mAdapter.notifyDataSetInvalidated();
        }
    }

    private class AlarmsListAdapter extends BaseExpandableListAdapter {
        public AlarmsListAdapter() {
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            if (groupPosition < 0 || groupPosition >= mGroups.length) {
                return null;
            }
            return mGroups[groupPosition].getChild(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            if (groupPosition < 0 || groupPosition >= mGroups.length) {
                return -1;
            }
            return mGroups[groupPosition].getChildId(childPosition);
        }

        @Override
        public View getChildView(int groupPosition, int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            if (groupPosition < 0 || groupPosition >= mGroups.length) {
                return null;
            }
            return mGroups[groupPosition].getChildView(childPosition, convertView, parent);
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if (groupPosition < 0 || groupPosition >= mGroups.length) {
                return 0;
            }
            return mGroups[groupPosition].getChildrenCount();
        }

        @Override
        public Object getGroup(int groupPosition) {
            if (groupPosition < 0 || groupPosition >= mGroups.length) {
                return null;
            }
            return mGroups[groupPosition];
        }

        @Override
        public int getGroupCount() {
            return mGroups.length;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(final int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            View layout;
            if (convertView == null) {
                layout = mLayoutInflater.inflate(R.layout.alarm_group_title, parent, false);
            } else {
                layout = convertView;
            }
            layout.findViewById(R.id.add_button).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    addAlarm(mGroups[groupPosition]);
                }
            });
            TextView title = (TextView) layout.findViewById(R.id.text);
            title.setText(mGroups[groupPosition].getName());
            return layout;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mAdapter.notifyDataSetChanged();
    }
}
