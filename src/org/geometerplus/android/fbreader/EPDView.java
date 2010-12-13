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
import android.epd.EpdInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.view.ZLView;

import org.geometerplus.zlibrary.text.view.ZLTextView;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;


abstract class EPDView implements EpdInterface.CallBackKeyEvent, ZLAndroidLibrary.EventsListener {

	private static final int EPD_NUMBER = 0;

	private final Activity myActivity;

	private EpdInterface myInterface;
	private int myEpdId = -1;


	private static final int EPD_FINISH = -1;
	private static final int EPD_UPDATE = 0;
	private static final int EPD_DO_UPDATE = 1;

	private class ViewHandler extends Handler {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case EPD_UPDATE:
				updateEpdView(0);
				break;
			case EPD_FINISH:
				if (myInterface != null) {
					myInterface.Cancel();
				}
				myActivity.finish();
				break;
			case EPD_DO_UPDATE:
				updateEpdView();
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
		myHandler.sendEmptyMessage(EPD_UPDATE);
	}

	public void finishActivity() {
		myHandler.sendEmptyMessage(EPD_FINISH);
	}


	public void onResume() {
		if (myActivity.findViewById(R.id.epd_layout) == null) {
			throw new RuntimeException("EPDView's activity must be layed out with \"epd_layout\" layout.");
		}

		Log.w("FBREADER", "onResume: " + myActivity.getClass().getSimpleName());

		myInterface = new EpdInterface();
		myInterface.setCallBack(this);

		myEpdId = myInterface.PondCreateVds(EPD_NUMBER);
		myInterface.PondSetVdsActive(EPD_NUMBER, myEpdId, 1);
		myInterface.PondSendMessage(EPD_NUMBER, myEpdId, EpdInterface.EPD_REGISTER_TOGGLEKEY, null);
		updateEpdViewDelay(200);
	}

	public void onPause() {
		Log.w("FBREADER", "onPause: " + myActivity.getClass().getSimpleName());

		myInterface.Cancel();
		myInterface.PondSetVdsActive(EPD_NUMBER, myEpdId, 0);
		myInterface.PondDeleteVds(EPD_NUMBER, myEpdId);
		myEpdId = -1;
		myInterface = null;
	}

	public void executeKeyEvent(int keycode, int arg1, int arg2) {
		switch (keycode) {
		case EpdInterface.EPD_PAGE_UP:
			Log.w("FBREADER", "onPageUp: " + myActivity.getClass().getSimpleName());
			onPageUp(arg1, arg2);
			break;
		case EpdInterface.EPD_PAGE_DOWN:
			Log.w("FBREADER", "onPageDown: " + myActivity.getClass().getSimpleName());
			onPageDown(arg1, arg2);
			break;
		case EpdInterface.EPD_TOGGLE:
			Log.w("FBREADER", "onTogglePressed: " + myActivity.getClass().getSimpleName());
			onTogglePressed(arg1, arg2);
			break;
		case EpdInterface.EPD_REPAINT:
			Log.w("FBREADER", "onRepaintEpdWindow: " + myActivity.getClass().getSimpleName());
			break;
		case EpdInterface.EPD_FONTSIZE_CHANGE:
			Log.w("FBREADER", "EPD_FONTSIZE_CHANGE: " + myActivity.getClass().getSimpleName());
			break;
		case EpdInterface.EPD_PROJECT:
			Log.w("FBREADER", "EPD_PROJECT: " + myActivity.getClass().getSimpleName());
			break;
		case EpdInterface.EPD_REGISTER_TOGGLEKEY:
			Log.w("FBREADER", "EPD_REGISTER_TOGGLEKEY: " + myActivity.getClass().getSimpleName());
			break;
		}
	}

	public void onPageUp(int arg1, int arg2) {
		final int angle = ZLAndroidApplication.Instance().RotationFlag;
		scrollPage(angle == ZLAndroidApplication.ROTATE_90 || angle == ZLAndroidApplication.ROTATE_180);
	}

	public void onPageDown(int arg1, int arg2) {
		final int angle = ZLAndroidApplication.Instance().RotationFlag;
		scrollPage(angle == ZLAndroidApplication.ROTATE_0 || angle == ZLAndroidApplication.ROTATE_270);
	}

	public void onTogglePressed(int arg1, int arg2) {
	}



	public final void scrollPage(boolean forward) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view instanceof ZLTextView) {
			onPageScrolling();
			((ZLTextView) view).scrollPage(forward, ZLTextView.ScrollingMode.NO_OVERLAPPING, 0);
			ZLApplication.Instance().repaintView();
		}
	}

	protected void onPageScrolling() {
	}

	public void updateEpdView(int delay) {
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


	// --- methods from EpdRender ---

	public void updateEpdViewDelay(int delay) {
		myHandler.sendMessageDelayed(myHandler.obtainMessage(EPD_DO_UPDATE), delay);
	}


	private Bitmap myBitmap;

	public void updateEpdView() {
		if (myInterface == null) {
			return;
		}
		final int width = myInterface.PondGetDisplayCaps(0, EpdInterface.PONDDC_XPIXEL);
		final int height = myInterface.PondGetDisplayCaps(0, EpdInterface.PONDDC_YPIXEL);
		if (myBitmap != null && (myBitmap.getWidth() != width || myBitmap.getHeight() != height)) {
			myBitmap.recycle();
			myBitmap = null;
		}
		if (myBitmap == null) {
			myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565); 
		}
		final Canvas canvas = new Canvas(myBitmap);
		final View view = myActivity.findViewById(R.id.epd_layout);
		view.draw(canvas);
		myInterface.Cancel();
		myInterface.PondDrawBitmap565(EPD_NUMBER, myEpdId, myBitmap, 0);
	}
}
