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
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;

import org.geometerplus.zlibrary.core.util.ZLLog;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.application.ZLApplication;

import org.geometerplus.zlibrary.ui.android.util.ZLAndroidKeyUtil;

public class ZLAndroidWidget extends View {
	private final Paint myPaint = new Paint();
	private Bitmap myMainBitmap;

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

	private final Handler myRepaintFinishedHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			ZLApplication.Instance().onRepaintFinished();
		}
	};

	private boolean myRotated;
	private Bitmap myBufferBitmap;

	public void setRotated(boolean rotated) {
		myRotated = rotated;
		if (myBufferBitmap != null) {
			myBufferBitmap.recycle();
			myBufferBitmap = null;
		}
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view != null) {
			final ZLAndroidPaintContext context = ZLAndroidPaintContext.Instance();
			updatePaintContextSize(view, context);
		}
	}

	private void updatePaintContextSize(ZLView view, ZLAndroidPaintContext context) {
		final int scrollbarWidth = view.showScrollbar() ? getVerticalScrollbarWidth() : 0;
		if (myRotated) {
			context.setSize(getHeight(), getWidth(), scrollbarWidth);
		} else {
			context.setSize(getWidth(), getHeight(), scrollbarWidth);
		}
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		ZLLog.log("Start onDraw");
		super.onDraw(canvas);

		final int w = getWidth();
		final int h = getHeight();

		if ((myMainBitmap != null) && ((myMainBitmap.getWidth() != w) || (myMainBitmap.getHeight() != h))) {
			myMainBitmap.recycle();
			myMainBitmap = null;
			if (myBufferBitmap != null) {
				myBufferBitmap.recycle();
				myBufferBitmap = null;
			}
			System.gc();
			System.gc();
			System.gc();
		}
		if (myMainBitmap == null) {
			myMainBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		}

		ZLLog.log("do drawings...");
		drawOnBitmap();
		canvas.drawBitmap(myMainBitmap, 0, 0, myPaint);
		ZLLog.log("finish drawings");

		myRepaintFinishedHandler.sendEmptyMessage(0);
		ZLLog.log("Finish onDraw");
	}

	private void drawOnBitmap() {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view == null) {
			return;
		}

		final Bitmap bitmap;
		if (myRotated) {
			if (myBufferBitmap == null) {
				final int size = Math.max(myMainBitmap.getWidth(), myMainBitmap.getHeight());
				myBufferBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
			}
			bitmap = myBufferBitmap;
		} else {
			bitmap = myMainBitmap;
		}

		final ZLAndroidPaintContext context = ZLAndroidPaintContext.Instance();

		Canvas canvas = new Canvas(bitmap);
		context.beginPaint(canvas);
		updatePaintContextSize(view, context);
		view.paint(ZLView.PAGE_CENTRAL);
		context.endPaint();

		if (myRotated) {
			final int w = myMainBitmap.getWidth();
			final int h = myMainBitmap.getHeight();
			final float anchor = Math.min(w, h) / 2.0f;
			canvas = new Canvas(myMainBitmap);
			canvas.save();
			canvas.rotate(90.0f, anchor, anchor);
			// FIXME: handle situation (w > h): How to translate (along X or Y)? and when (before rotation, or after)?
			/*if (w > h) {
				canvas.translate(0, w - h);
				canvas.translate(w - h, 0);
			}*/
			canvas.drawBitmap(myBufferBitmap, 0, 0, myPaint);
			canvas.restore();
		}
	}

	public final Bitmap getBitmap() {
		return myMainBitmap;
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
