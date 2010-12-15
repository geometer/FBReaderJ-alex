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

import java.util.LinkedList;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.View;
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

	private final Activity myActivity;

	private class ViewHandler extends Handler {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				updateEpdDisplay();
				break;
			case -1:
				myActivity.finish();
				break;
			}
		};
	};
	private final Handler myHandler = new ViewHandler();


	public EPDView(Activity activity) {
		myActivity = activity;
	}

	public final Activity getActivity() {
		return myActivity;
	}

	public void notifyApplicationChanges(boolean singleChange) {
		myHandler.sendEmptyMessage(0);
	}

	public void finishActivity() {
		myHandler.sendEmptyMessage(-1);
	}


	public interface EventsListener {
		void onPageScrolling();
		void onEpdRepaintFinished();
	}

	private LinkedList<EventsListener> myListeners = new LinkedList<EventsListener>();

	public void addEventsListener(EventsListener listener) {
		myListeners.add(listener);
	}

	public void removeEventsListener(EventsListener listener) {
		myListeners.remove(listener);
	}

	protected final void onPageScrolling() {
		for (EventsListener listener: myListeners) {
			listener.onPageScrolling();
		}
	}

	public final void onEpdRepaintFinished() {
		for (EventsListener listener: myListeners) {
			listener.onEpdRepaintFinished();
		}
	}


	public void onResume() {
		final LinearLayout view = (LinearLayout) myActivity.findViewById(R.id.epd_layout);
		if (view == null) {
			throw new RuntimeException("EPDView's activity must be layed out with \"epd_layout\" layout.");
		}
		setVdsActive(true);
		if (getLayout() != view) {
			bindLayout(view);
		}
		notifyApplicationChanges(true);
	}

	public void onPause() {
		setVdsActive(false);
	}

	@Override
	public boolean onPageUp(int arg1, int arg2) {
		final int angle = ZLAndroidApplication.Instance().RotationFlag;
		scrollPage(angle == ZLAndroidApplication.ROTATE_90 || angle == ZLAndroidApplication.ROTATE_180);
		return true;
	}

	@Override
	public boolean onPageDown(int arg1, int arg2) {
		final int angle = ZLAndroidApplication.Instance().RotationFlag;
		scrollPage(angle == ZLAndroidApplication.ROTATE_0 || angle == ZLAndroidApplication.ROTATE_270);
		return true;
	}

	public final void scrollPage(boolean forward) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view instanceof ZLTextView) {
			onPageScrolling();
			((ZLTextView) view).scrollPage(forward, ZLTextView.ScrollingMode.NO_OVERLAPPING, 0);
			ZLApplication.Instance().repaintView();
		}
	}

	public void updateEpdDisplay() {
		updateEpdStatusbar();
		updateEpdView();

		final View bar = myActivity.findViewById(R.id.statusbar_layout);

		if (bar.getWidth() != 0 && bar.getHeight() != 0) {
			final int left = bar.getLeft();
			final int top = bar.getTop();
			final int right = left + bar.getWidth();
			final int bottom = top + bar.getHeight();
			final Rect rect = new Rect(left + bar.getPaddingLeft(), top + bar.getPaddingTop(),
				right + bar.getPaddingRight(), bottom + bar.getPaddingBottom());
			Log.w("FBREADER", "rect: " + rect + " / " + rect.width() + " x " + rect.height());
			partialUpdateEpdView(rect);
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
}
