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

import org.openskydive.altidroid.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

public class SeekBarPreference extends DialogPreference {
    protected static SeekBar getSeekBar(View dialogView) {
        return (SeekBar) dialogView.findViewById(R.id.seekbar);
    }

    private final Drawable mMyIcon;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.seekbar_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        // Steal the XML dialogIcon attribute's value
        mMyIcon = getDialogIcon();
        setDialogIcon(null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        final ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        if (mMyIcon != null) {
            iconView.setImageDrawable(mMyIcon);
        } else {
            iconView.setVisibility(View.GONE);
        }
    }
}
