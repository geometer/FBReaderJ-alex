/*
 * Copyright (C) 2007-2010 Geometer Plus <contact@geometerplus.com>
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

package org.geometerplus.zlibrary.ui.android.view;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.util.AttributeSet;

import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.application.ZLApplication;

import org.geometerplus.zlibrary.ui.android.util.ZLAndroidKeyUtil;

public class ZLAndroidWidget extends View {
	private final Paint myPaint = new Paint();
	private Bitmap myMainBitmap;
	private Bitmap mySecondaryBitmap;
	private boolean mySecondaryBitmapIsUpToDate;
	private boolean myScrollingInProgress;

	private int myViewPageToScroll = ZLView.PAGE_CENTRAL;

	public ZLAndroidWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setDrawingCacheEnabled(false);
	}

	public ZLAndroidWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDrawingCacheEnabled(false);
	}

	public ZLAndroidWidget(Context context) {
		super(context);
		setDrawingCacheEnabled(false);
	}

	public ZLAndroidPaintContext getPaintContext() {
		return ZLAndroidPaintContext.Instance();
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);

		final int w = getWidth();
		final int h = getHeight();

		if ((myMainBitmap != null) && ((myMainBitmap.getWidth() != w) || (myMainBitmap.getHeight() != h))) {
			myMainBitmap = null;
			mySecondaryBitmap = null;
			System.gc();
			System.gc();
			System.gc();
		}
		if (myMainBitmap == null) {
			myMainBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
			mySecondaryBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
			mySecondaryBitmapIsUpToDate = false;
			drawOnBitmap(myMainBitmap);
		}

		if (myScrollingInProgress) {
			onDrawInScrolling(canvas);
		} else {
			onDrawStatic(canvas);
			ZLApplication.Instance().onRepaintFinished();
		}
	}

	private void onDrawInScrolling(Canvas canvas) {

		canvas.drawBitmap(mySecondaryBitmap, 0, 0, myPaint);

		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (myViewPageToScroll != ZLView.PAGE_CENTRAL) {
			Bitmap swap = myMainBitmap;
			myMainBitmap = mySecondaryBitmap;
			mySecondaryBitmap = swap;
			mySecondaryBitmapIsUpToDate = false;
			view.onScrollingFinished(myViewPageToScroll);
			ZLApplication.Instance().onRepaintFinished();
		} else {
			view.onScrollingFinished(ZLView.PAGE_CENTRAL);
		}

		setPageToScroll(ZLView.PAGE_CENTRAL);
		myScrollingInProgress = false;
	}

	private void setPageToScroll(int viewPage) {
		if (myViewPageToScroll != viewPage) {
			myViewPageToScroll = viewPage;
			mySecondaryBitmapIsUpToDate = false;
		}
	}

	void startAutoScrolling(int viewPage) {
		if (myMainBitmap == null) {
			return;
		}
		myScrollingInProgress = true;
		if (viewPage != ZLView.PAGE_CENTRAL) {
			setPageToScroll(viewPage);
		}
		drawOnBitmap(mySecondaryBitmap);
		postInvalidate();
	}

	private void drawOnBitmap(Bitmap bitmap) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view == null) {
			return;
		}

		if (bitmap == myMainBitmap) {
			mySecondaryBitmapIsUpToDate = false;
		} else if (mySecondaryBitmapIsUpToDate) {
			return;
		} else {
			mySecondaryBitmapIsUpToDate = true;
		}

		final int w = getWidth();
		final int h = getHeight();
		final ZLAndroidPaintContext context = ZLAndroidPaintContext.Instance();

		Canvas canvas = new Canvas(bitmap);
		context.beginPaint(canvas);
		final int scrollbarWidth = view.showScrollbar() ? getVerticalScrollbarWidth() : 0;
		context.setSize(w, h, scrollbarWidth);
		view.paint((bitmap == myMainBitmap) ? ZLView.PAGE_CENTRAL : myViewPageToScroll);
		context.endPaint();
	}

	private void onDrawStatic(Canvas canvas) {
		drawOnBitmap(myMainBitmap);
		canvas.drawBitmap(myMainBitmap, 0, 0, myPaint);
	}

	
	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null);
		} else {
			ZLApplication.Instance().getCurrentView().onTrackballRotated((int)(10 * event.getX()), (int)(10 * event.getY()));
		}
		return true;
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_DOWN:
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_BACK:
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				return ZLApplication.Instance().doActionByKey(ZLAndroidKeyUtil.getKeyNameByCode(keyCode));
			case KeyEvent.KEYCODE_DPAD_DOWN:
				ZLApplication.Instance().getCurrentView().onTrackballRotated(0, 1);
				return true;
			case KeyEvent.KEYCODE_DPAD_UP:
				ZLApplication.Instance().getCurrentView().onTrackballRotated(0, -1);
				return true;
			default:
				return false;
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_DOWN:
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_BACK:
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				return true;
			default:
				return false;
		}
	}

	@Override
	protected int computeVerticalScrollExtent() {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.showScrollbar()) {
			return 0;
		}
		return view.getScrollbarThumbLength(ZLView.PAGE_CENTRAL);
	}

	@Override
	protected int computeVerticalScrollOffset() {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.showScrollbar()) {
			return 0;
		}
		return view.getScrollbarThumbPosition(ZLView.PAGE_CENTRAL);
	}

	@Override
	protected int computeVerticalScrollRange() {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.showScrollbar()) {
			return 0;
		}
		return view.getScrollbarFullSize();
	}
}
