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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;


public class SynchronousActivity extends Activity {
	static SynchronousActivity Instance;

	public static final String ROTATE_KEY = "org.geometerplus.android.fbreader.RotateFlag";

	private ProgressDialog myProgressDialog;
	private ZLAndroidWidget myWidget;

	private boolean myRotateFlag;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (FBReaderActivity.Instance == null) {
			finish();
			return;
		}

		if (savedInstanceState != null) {
			myRotateFlag = savedInstanceState.getBoolean(ROTATE_KEY, false);
		} else {
			final Intent intent = getIntent();
			myRotateFlag = intent.getBooleanExtra(ROTATE_KEY, false);
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.synchronous_view);

		myWidget = (ZLAndroidWidget) FBReaderActivity.Instance.findViewById(R.id.main_view_epd);
		final SynchronousView view = (SynchronousView) findViewById(R.id.synchronous_view);
		view.setWidget(myWidget);
		view.setRotated(myRotateFlag);

		myProgressDialog = new ProgressDialog(this);
		myProgressDialog.setIndeterminate(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateImage();
		Instance = this;
	}

	@Override
	protected void onPause() {
		myProgressDialog.cancel();
		Instance = null;
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(ROTATE_KEY, myRotateFlag);
	}

	void updateImage() {
		SynchronousView view = (SynchronousView) findViewById(R.id.synchronous_view);
		view.resetScroll();
		view.invalidate();
		myProgressDialog.cancel();
	}

	void showPageProgress() {
		SynchronousView view = (SynchronousView) findViewById(R.id.synchronous_view);
		view.invalidateScroll();
		showProgress(ZLResource.resource("fbreader").getResource("pageChange").getValue());
	}

	private void showProgress(String message) {
		myProgressDialog.setMessage(message);
		if (!myProgressDialog.isShowing()) {
			myProgressDialog.show();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return ((myWidget != null) && myWidget.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return ((myWidget != null) && myWidget.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
	}
}
