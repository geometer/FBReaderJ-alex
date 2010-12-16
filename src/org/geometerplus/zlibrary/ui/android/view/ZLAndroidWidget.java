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

import org.geometerplus.zlibrary.core.view.ZLPaintContext;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.application.ZLApplication;

import org.geometerplus.zlibrary.text.view.ZLRect;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
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

	private final Handler myRepaintFinishedHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			ZLApplication.Instance().onRepaintFinished();
		}
	};

	private int myRotationAngle;

	public void setRotation(int angle) {
		myRotationAngle = angle;
		resetContext();
	}

	public void resetContext() {
		final ZLApplication app = ZLApplication.Instance();
		if (app != null) {
			final ZLView view = app.getCurrentView();
			if (view != null) {
				view.resetContext(createContext(view, new Canvas()));
			}
		}
	}

	private final ZLPaintContext createContext(ZLView view, Canvas canvas) {
		final boolean rotated = myRotationAngle == ZLAndroidApplication.ROTATE_90 ||
			myRotationAngle == ZLAndroidApplication.ROTATE_270;
		return new ZLAndroidPaintContext(
			canvas,
			rotated ? getHeight() : getWidth(),
			rotated ? getWidth() : getHeight(),
			view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0
		);
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);

		final int w = getWidth();
		final int h = getHeight();

		if ((myMainBitmap != null) && ((myMainBitmap.getWidth() != w) || (myMainBitmap.getHeight() != h))) {
			myMainBitmap.recycle();
			myMainBitmap = null;
			System.gc();
			System.gc();
			System.gc();
		}
		if (myMainBitmap == null) {
			myMainBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		}

		drawOnBitmap();
		canvas.drawBitmap(myMainBitmap, 0, 0, myPaint);

		myRepaintFinishedHandler.sendEmptyMessage(0);
	}

	private void drawOnBitmap() {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view == null) {
			return;
		}

		final Canvas canvas = new Canvas(myMainBitmap);

		canvas.save();
		final int w = myMainBitmap.getWidth();
		final int h = myMainBitmap.getHeight();
		switch (myRotationAngle) {
		case ZLAndroidApplication.ROTATE_90:
			canvas.rotate(90.0f);
			canvas.translate(0.0f, -w);
			break;
		case ZLAndroidApplication.ROTATE_180:
			canvas.rotate(180.0f, w / 2.0f, h / 2.0f);
			break;
		case ZLAndroidApplication.ROTATE_270:
			canvas.rotate(-90.0f);
			canvas.translate(-h, 0.0f);
			break;
		}

		final ZLPaintContext context = createContext(view, canvas); 
		view.paint(context, ZLView.PAGE_CENTRAL);
		canvas.restore();
	}

	public final Bitmap getBitmap() {
		return myMainBitmap;
	}

	public final Rect convertRect(ZLRect rect) {
		final int w = myMainBitmap.getWidth();
		final int h = myMainBitmap.getHeight();
		switch (myRotationAngle) {
		case ZLAndroidApplication.ROTATE_90:
			return new Rect(w - rect.Bottom, rect.Left, w - rect.Top, rect.Right);
		case ZLAndroidApplication.ROTATE_180:
			return new Rect(w - rect.Right, h - rect.Bottom, w - rect.Left, h - rect.Top);
		case ZLAndroidApplication.ROTATE_270:
			return new Rect(rect.Top, h - rect.Right, rect.Bottom, h - rect.Left);
		}
		return new Rect(rect.Left, rect.Top, rect.Right, rect.Bottom);
	}

	private int getDPadKey(int keyCode) {
		if (myRotationAngle == ZLAndroidApplication.ROTATE_0) {
			return keyCode;
		}
		int offset = 0;
		switch (myRotationAngle) {
		case ZLAndroidApplication.ROTATE_90:
			offset = 3;
			break;
		case ZLAndroidApplication.ROTATE_180:
			offset = 2;
			break;
		case ZLAndroidApplication.ROTATE_270:
			offset = 1;
			break;
		}
		final int codes[] = {KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT};
		for (int i = 0; i < codes.length; ++i) {
			if (codes[i] == keyCode) {
				return codes[(i + offset) % codes.length];
			}
		}
		return keyCode;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				return ZLApplication.Instance().doActionByKey(ZLAndroidKeyUtil.getKeyNameByCode(keyCode));
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_DPAD_UP:
				final int dpadKey = getDPadKey(keyCode);
				switch (dpadKey) {
				case KeyEvent.KEYCODE_DPAD_LEFT:
					ZLApplication.Instance().getCurrentView().onTrackballRotated(-1, 0);
					return true;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					ZLApplication.Instance().getCurrentView().onTrackballRotated(1, 0);
					return true;
				case KeyEvent.KEYCODE_DPAD_DOWN:
					ZLApplication.Instance().getCurrentView().onTrackballRotated(0, 1);
					return true;
				case KeyEvent.KEYCODE_DPAD_UP:
					ZLApplication.Instance().getCurrentView().onTrackballRotated(0, -1);
					return true;
				}
			default:
				return false;
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				return true;
			default:
				return false;
		}
	}

	@Override
	protected int computeVerticalScrollExtent() {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.isScrollbarShown()) {
			return 0;
		}
		return view.getScrollbarThumbLength(ZLView.PAGE_CENTRAL);
	}

	@Override
	protected int computeVerticalScrollOffset() {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.isScrollbarShown()) {
			return 0;
		}
		return view.getScrollbarThumbPosition(ZLView.PAGE_CENTRAL);
	}

	@Override
	protected int computeVerticalScrollRange() {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.isScrollbarShown()) {
			return 0;
		}
		return view.getScrollbarFullSize();
	}
}
