/*
 * Copyright (C) 2010 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;

import org.geometerplus.fbreader.fbreader.ActionCode;


public class DictionaryDialog extends Dialog {

	public DictionaryDialog(Context context, final EPDView epd) {
		super(context, android.R.style.Theme_Translucent_NoTitleBar);
		setContentView(R.layout.dictionary);

		final ImageButton translate = (ImageButton)findViewById(R.id.translate);
		final String text = ZLResource.resource("fbreader").getResource("translate").getValue();
		final int rotation = ZLAndroidApplication.Instance().RotationFlag;
		translate.setImageDrawable(RotatedStringDrawable.create(text, rotation, 16));
		translate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ZLApplication.Instance().doAction(ActionCode.PROCESS_HYPERLINK);
			}
		});

		((ImageButton)findViewById(R.id.turnUp)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				epd.getActivity().findViewById(R.id.main_view_epd).onKeyDown(KeyEvent.KEYCODE_DPAD_UP, null);
			}
		});
		((ImageButton)findViewById(R.id.turnDown)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				epd.getActivity().findViewById(R.id.main_view_epd).onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, null);
			}
		});
		((ImageButton)findViewById(R.id.turnLeft)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				epd.getActivity().findViewById(R.id.main_view_epd).onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
			}
		});
		((ImageButton)findViewById(R.id.turnRight)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				epd.getActivity().findViewById(R.id.main_view_epd).onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);
			}
		});

		setOnDismissListener(new OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				ZLApplication.Instance().doAction(ActionCode.SET_TEXT_VIEW_MODE_VISIT_HYPERLINKS);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		ZLApplication.Instance().doAction(ActionCode.SET_TEXT_VIEW_MODE_VISIT_ALL_WORDS);
	}
}
