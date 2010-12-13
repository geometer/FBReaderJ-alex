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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

import org.geometerplus.zlibrary.core.resources.ZLResource;


public class SynchronousDialog extends Dialog {

	public static Dialog Instance;

	private ProgressDialog myProgressDialog;
	private ZLAndroidWidget myWidget;

	public SynchronousDialog(Context context, final EPDView epd) {
		super(context, android.R.style.Theme_Translucent_NoTitleBar);

		final EPDView.EventsListener listener = new EPDView.EventsListener() {
			public void onPageScrolling() {
				SynchronousDialog.this.showPageProgress();
			}
			public void onEpdRepaintFinished() {
				SynchronousDialog.this.updateImage();
			}
		};
		epd.addEventsListener(listener);

		setContentView(R.layout.synchronous_view);

		final SynchronousView view = (SynchronousView) findViewById(R.id.synchronous_view);
		view.setEPDView(epd);

		myWidget = (ZLAndroidWidget)epd.getActivity().findViewById(R.id.main_view_epd);

		myProgressDialog = new ProgressDialog(context);
		myProgressDialog.setIndeterminate(true);

		setOnDismissListener(new OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				myProgressDialog.cancel();
				epd.removeEventsListener(listener);
				Instance = null;
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		Instance = this;
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
