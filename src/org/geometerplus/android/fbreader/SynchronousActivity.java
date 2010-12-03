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
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;


public class SynchronousActivity extends Activity {

	private ProgressDialog myProgressDialog;

	private static class SyncEPDView extends EPDView {

		public SyncEPDView(SynchronousActivity activity) {
			super(activity);
		}

		@Override
		public boolean onTogglePressed(int arg1, int arg2) {
			Log.w("FBREADER", "SyncEPDView: onTogglePressed");
			finishActivity();
			return true;
		}

		@Override
		protected void onPageScrolling() {
			((SynchronousActivity) getActivity()).showPageProgress();
		}

		public void onEpdRepaintFinished() {
			Log.w("FBREADER", "SyncEPDView: onEpdRepaintFinished");
			((SynchronousActivity) getActivity()).updateImage();
		}
	}
	private EPDView myEPDView = new SyncEPDView(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.synchronous_view);

		((ZLAndroidLibrary)ZLAndroidLibrary.Instance()).setEventsListener(myEPDView);
		((ZLAndroidLibrary)ZLAndroidLibrary.Instance()).setActivity(this);

		final SynchronousView view = (SynchronousView) findViewById(R.id.synchronous_view);
		view.setEPDView(myEPDView);

		myProgressDialog = new ProgressDialog(this);
		myProgressDialog.setIndeterminate(true);
	}

	@Override
	protected void onStart() {
		super.onStart();
		((ZLAndroidLibrary)ZLAndroidLibrary.Instance()).setEventsListener(myEPDView);
		((ZLAndroidLibrary)ZLAndroidLibrary.Instance()).setActivity(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		myEPDView.onResume();
		updateImage();
	}

	@Override
	protected void onPause() {
		myProgressDialog.cancel();
		myEPDView.onPause();
		super.onPause();
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
		View view = findViewById(R.id.main_view_epd);
		return ((view != null) && view.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		View view = findViewById(R.id.main_view_epd);
		return ((view != null) && view.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
	}
}
