/*
 * Copyright (C) 2009-2010 Geometer Plus <contact@geometerplus.com>
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
import android.os.Handler;
import android.util.Log;
import android.widget.EpdRender;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.view.ZLView;

import org.geometerplus.zlibrary.text.view.ZLTextView;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;


abstract class EPDView extends EpdRender implements ZLAndroidLibrary.EventsListener {

	private static EPDView ourActiveEPDView;

	private final Activity myActivity;

	public EPDView(Activity activity) {
		myActivity = activity;
	}

	public final Activity getActivity() {
		return myActivity;
	}

	@Override
	public void setVdsActive(boolean active) {
		if (active) {
			if (ourActiveEPDView == this) {
				return;
			}
			if (ourActiveEPDView != null) {
				ourActiveEPDView.setVdsActive(false);
			}
			ourActiveEPDView = this;
		}
		Log.w("FBREADER", "EPDView: " + (active ? "Activate" : "Deactivate") +
				" VDS for (" + myActivity.getClass().getSimpleName() + ")");
		super.setVdsActive(active);
	}


	public void onStart() {
		final LinearLayout view = (LinearLayout) myActivity.findViewById(R.id.epd_layout);
		if (view == null) {
			throw new RuntimeException("EPDView's activity must be layed out with \"epd_layout\" layout.");
		}
		setVdsActive(true);

		Log.w("FBREADER", "EPDView: bind layout for (" + myActivity.getClass().getSimpleName() + ")");
		bindLayout(view);
		updateEpdViewDelay(200);
	}


	@Override
	public boolean onPageUp(int arg1, int arg2) {
		Log.w("FBREADER", "EPDView: onPageUp (" + myActivity.getClass().getSimpleName() + ")");
		scrollPage(ZLAndroidApplication.Instance().RotatedFlag);
		return true;
	}

	@Override
	public boolean onPageDown(int arg1, int arg2) {
		Log.w("FBREADER", "EPDView: onPageDown (" + myActivity.getClass().getSimpleName() + ")");
		scrollPage(!ZLAndroidApplication.Instance().RotatedFlag);
		return true;
	}

	public final void scrollPage(boolean forward) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view instanceof ZLTextView) {
			((ZLTextView) view).scrollPage(forward, ZLTextView.ScrollingMode.NO_OVERLAPPING, 0);
			onPageScrolling();
			ZLApplication.Instance().repaintView();
		}
	}

	protected void onPageScrolling() {
	}

	public void updateEpdView(int delay) {
		Log.w("FBREADER", "EPDView: updateEPDView(" + delay + ") for (" + myActivity.getClass().getSimpleName() + ")");
		updateEpdStatusbar();
		if (delay <= 0) {
			updateEpdView();
		} else {
			updateEpdViewDelay(delay);
		}
	}

	private void updateEpdStatusbar() {
		final TextView statusPositionText = (TextView) myActivity.findViewById(R.id.statusbar_position_text);
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view instanceof ZLTextView
				&& ((ZLTextView) view).getModel() != null
				&& ((ZLTextView) view).getModel().getParagraphsNumber() != 0) {
			ZLTextView textView = (ZLTextView) view;
			final int page = textView.computeCurrentPage();
			final int pagesNumber = textView.computePageNumber();
			statusPositionText.setText(makePositionText(page, pagesNumber));
		} else {
			statusPositionText.setText("");
		}
	}

	public static String makePositionText(int page, int pagesNumber) {
		return "" + page + " / " + pagesNumber;
	}

	private class NotificationHandler extends Handler {
		@Override
		public void handleMessage(android.os.Message msg) {
			/*final boolean singleChange = msg.what == 1;
			updateEpdView(singleChange ? 0 : 10);*/
			updateEpdView(0);
		};
	};
	private final Handler myNotifyApplicationHandler = new NotificationHandler();


	public void notifyApplicationChanges(boolean singleChange) {
		myNotifyApplicationHandler.sendEmptyMessage(singleChange ? 1 : 0);
	}
}
