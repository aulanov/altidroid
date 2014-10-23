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

import org.openskydive.altidroid.R;
import org.openskydive.altidroid.log.LogEntry;
import org.openskydive.altidroid.log.LogProtos.Entry;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LogFragment extends ListFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

    private LayoutInflater mLayoutInflater;
    private LogCursorAdapter mAdapter;

    private Typeface mNumberTypeface;

    public class LogCursorAdapter extends CursorAdapter {
        private final DateFormat mDateFormat =
                android.text.format.DateFormat.getMediumDateFormat(getActivity());

        public LogCursorAdapter() {
            super(getActivity(), null, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View layout = mLayoutInflater.inflate(R.layout.log_list_item, parent, false);
            bindView(layout, context, cursor);
            return layout;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            LogEntry entry = new LogEntry(cursor);

            TextView number = (TextView) view.findViewById(R.id.number);
            TextView firstLine = (TextView) view.findViewById(R.id.first_line);
            TextView firstLineRight = (TextView) view.findViewById(R.id.first_line_right);
            TextView secondLine = (TextView) view.findViewById(R.id.second_line);
            TextView secondLineRight = (TextView) view.findViewById(R.id.second_line_right);

            Entry proto = entry.getProto();

            number.setTypeface(mNumberTypeface);
            number.setText(Integer.toString(proto.getNumber()));
            firstLine.setText(mDateFormat.format(new Date(proto.getTimestamp())));
            firstLineRight.setText(proto.getDropzone());
            secondLine.setText(proto.getComments());
            secondLineRight.setText(proto.getJumpType());
        }

        @Override
        public Object getItem(int position) {
            return new LogEntry((Cursor) super.getItem(position));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.log_list, container, false);

        mNumberTypeface = Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/MedulaOne-Regular.ttf");

        mLayoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mAdapter = new LogCursorAdapter();
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);

        return rootView;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Intent i = new Intent(getActivity(), JumpInfoActivity.class);
        i.putExtra(JumpInfoActivity.POSITION_INTENT_EXTRA, position);
        startActivity(i);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), LogEntry.Columns.getContentUri(getActivity()),
                LogEntry.Columns.QUERY_COLUMNS,
                null, null, LogEntry.Columns.NUMBER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> data) {
        mAdapter.swapCursor(null);
    }
}
