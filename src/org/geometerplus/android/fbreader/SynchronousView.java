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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;


public class SynchronousView extends View {

	private EPDView myEPDView;
	private ZLAndroidWidget myWidget;

    private int myMinimumVelocity;

	private VelocityTracker myVelocityTracker;
	private Scroller myScroller;

	private float myLastScrollX; 
	private float myLastScrollY; 

	private int myScrollX;
	private int myScrollY;
	private boolean myInvalidScroll = true;

	private int myScrollPage;
	private float myStartScrollX;
	private float myStartScrollY;

	private boolean myPendingClick;

	public SynchronousView(Context context) {
		super(context);
		init();
	}

	public SynchronousView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SynchronousView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private final void init() {
		setDrawingCacheEnabled(false);
		myScroller = new Scroller(getContext());

		final ViewConfiguration configuration = ViewConfiguration.get(getContext());
		myMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
	}

	public void setEPDView(EPDView view) {
		myEPDView = view;
		myWidget = (ZLAndroidWidget) view.getActivity().findViewById(R.id.main_view_epd);
	}

	public void invalidateScroll() {
		myInvalidScroll = true;
	}

	public void resetScroll() {
		if (myInvalidScroll) {
			switch (ZLAndroidApplication.Instance().RotationFlag) {
			case ZLAndroidApplication.ROTATE_0:
				myScrollX = myScrollY = 0;
				break;
			case ZLAndroidApplication.ROTATE_90:
				myScrollX = Integer.MAX_VALUE;
				myScrollY = 0;
				break;
			case ZLAndroidApplication.ROTATE_180:
				myScrollX = Integer.MAX_VALUE;
				myScrollY = Integer.MAX_VALUE;
				break;
			case ZLAndroidApplication.ROTATE_270:
				myScrollX = 0;
				myScrollY = Integer.MAX_VALUE;
				break;
			}
		}
		myInvalidScroll = false;
	}

	private void normalizeScroll() {
		myScrollX = Math.max(0, Math.min(myScrollX, myWidget.getWidth() - getClientWidth()));
		myScrollY = Math.max(0, Math.min(myScrollY, myWidget.getHeight() - getClientHeight()));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (myWidget == null) {
			return;
		}
		normalizeScroll();
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view instanceof ZLTextView) {
			final ZLColor color = ((ZLTextView) view).getBackgroundColor();
			//canvas.drawColor(Color.rgb(255-color.Red, 255-color.Green, 255-color.Blue));
			canvas.drawColor(Color.rgb(color.Red, color.Green, color.Blue));
		}
		canvas.save(Canvas.ALL_SAVE_FLAG);
		canvas.clipRect(getPaddingLeft(), getPaddingTop(), 
				getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(), 
				Region.Op.REPLACE);
		final Bitmap bmp = myWidget.getBitmap();
		if (bmp != null && !bmp.isRecycled()) {
			canvas.drawBitmap(bmp,
				getPaddingLeft() - myScrollX,
				getPaddingTop() - myScrollY,
				null
			);
		}
		canvas.restore();
	}

	private int getClientHeight() {
		return getHeight() - getPaddingBottom() - getPaddingTop();
	}

	private int getClientWidth() {
		return getWidth() - getPaddingLeft() - getPaddingRight();
	}

	private int getBitmapX(float viewX) {
		final int value = (int) (viewX - getPaddingLeft() + myScrollX);
		return Math.max(0, Math.min(value, myWidget.getWidth()));
	}

	private int getBitmapY(float viewY) {
		final int value = (int) (viewY - getPaddingTop() + myScrollY);
		return Math.max(0, Math.min(value, myWidget.getHeight()));
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
			return false;
		}
		if (myWidget == null || (myWidget.getHeight() <= getClientHeight() 
				&& myWidget.getWidth() <= getClientWidth())) {
			myScrollY = 0;
			myScrollX = 0;
			return false;
		}

		normalizeScroll();

        if (myVelocityTracker == null) {
        	myVelocityTracker = VelocityTracker.obtain();
        }
        myVelocityTracker.addMovement(event);

		final int action = event.getAction();
		final float x = event.getX();
		final float y = event.getY();

		final int angle = ZLAndroidApplication.Instance().RotationFlag;
		final int widthDiff = myWidget.getWidth() - getClientWidth();
		final int heightDiff = myWidget.getHeight() - getClientHeight();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (!myScroller.isFinished()) {
				myScroller.abortAnimation();
			}
			myStartScrollX = x;
			myStartScrollY = y;
			myLastScrollY = y;
			myLastScrollX = x;
			myScrollPage = 0;
			if (angle == ZLAndroidApplication.ROTATE_0 || angle == ZLAndroidApplication.ROTATE_180) {
				if (myScrollY == 0) {
					myScrollPage = -1;
				} else if (myScrollY == heightDiff) {
					myScrollPage = 1;
				}
			} else {
				if (myScrollX == widthDiff) {
					myScrollPage = -1;
				} else if (myScrollX == 0) {
					myScrollPage = 1;
				}
			}
			myPendingClick = true;
			break;

		case MotionEvent.ACTION_MOVE:
			if (myPendingClick) {
				final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
				if (Math.abs(myStartScrollX - x) > slop || Math.abs(myStartScrollY - y) > slop) {
					myPendingClick = false;
				}
			}
			if (!myPendingClick) {
				myScrollX += (int) (myLastScrollX - x);
				myScrollY += (int) (myLastScrollY - y);
				myLastScrollX = x;
				myLastScrollY = y;
			}
			break;

		case MotionEvent.ACTION_UP:
			if (myPendingClick) {
				if (x > getPaddingLeft() && x < getWidth() - getPaddingRight()
						&& y > getPaddingTop() && y < getHeight() - getPaddingBottom()) {
					int startX = -1, startY = -1, stopX = -1, stopY = -1;
					switch (angle) {
					case ZLAndroidApplication.ROTATE_0:
						stopX = getBitmapX(x);
						stopY = getBitmapY(y);
						startX = getBitmapX(myStartScrollX);
						startY = getBitmapY(myStartScrollY);
						break;
					case ZLAndroidApplication.ROTATE_90:
						stopX = getBitmapY(y);
						stopY = myWidget.getWidth() - getBitmapX(x);
						startX = getBitmapY(myStartScrollY);
						startY = myWidget.getWidth() - getBitmapX(myStartScrollX);
						break;
					case ZLAndroidApplication.ROTATE_180:
						stopX = myWidget.getWidth() - getBitmapX(x);
						stopY = myWidget.getHeight() - getBitmapY(y);
						startX = myWidget.getWidth() - getBitmapX(myStartScrollX);
						startY = myWidget.getHeight() - getBitmapY(myStartScrollY);
						break;
					case ZLAndroidApplication.ROTATE_270:
						stopX = myWidget.getHeight() - getBitmapY(y);
						stopY = getBitmapX(x);
						startX = myWidget.getHeight() - getBitmapY(myStartScrollY);
						startY = getBitmapX(myStartScrollX);
						break;
					}
					if (startX >= 0 && startY >= 0 && stopX >= 0 && stopY >= 0) {
						final ZLView view = ZLApplication.Instance().getCurrentView();
						view.onStylusPress(startX, startY);
						view.onStylusMovePressed(stopX, stopY);
						view.onStylusRelease(stopX, stopY);
						ZLApplication.Instance().repaintView();
					}
				}
			} else {
				myVelocityTracker.computeCurrentVelocity(1000);
				int velocityX = (int) myVelocityTracker.getXVelocity();
				int velocityY = (int) myVelocityTracker.getYVelocity();
				if (Math.abs(velocityX) <= myMinimumVelocity) {
					velocityX = 0;
				}
				if (Math.abs(velocityY) <= myMinimumVelocity) {
					velocityY = 0;
				}

				final boolean tryScrollX = myScrollPage * velocityX > 0
					&& Math.abs(myStartScrollX - x) > getWidth() / 4
					&& Math.abs(myStartScrollY - y) < getHeight() / 3; 
				final boolean tryScrollY = myScrollPage * velocityY < 0
					&& Math.abs(myStartScrollX - x) < getWidth() / 4
					&& Math.abs(myStartScrollY - y) > getHeight() / 3; 

				if ((angle == ZLAndroidApplication.ROTATE_90 || angle == ZLAndroidApplication.ROTATE_270) ?
						tryScrollX : tryScrollY) {
					myInvalidScroll = true;
					myEPDView.scrollPage((angle == ZLAndroidApplication.ROTATE_0
							|| angle == ZLAndroidApplication.ROTATE_90) ?
						(myScrollPage > 0) : (myScrollPage < 0)
					);
				} else {
					myScroller.fling(myScrollX, myScrollY, -velocityX, -velocityY,
							0, widthDiff, 0, heightDiff);
				}
			}
			if (myVelocityTracker != null) {
				myVelocityTracker.recycle();
				myVelocityTracker = null;
			}
			break;
		}
		invalidate();
		return true;
	}

	@Override
	public void computeScroll() {
        if (myScroller.computeScrollOffset()) {
            myScrollX = myScroller.getCurrX();
            myScrollY = myScroller.getCurrY();
            postInvalidate();
        }
	}

	@Override
	protected int computeVerticalScrollRange() {
		return (myWidget == null) ? getHeight() : myWidget.getHeight();
	}

	@Override
	protected int computeVerticalScrollOffset() {
		return myScrollY;
	}

	@Override
	protected int computeHorizontalScrollRange() {
		return (myWidget == null) ? getWidth() : myWidget.getWidth();
	}

	@Override
	protected int computeHorizontalScrollOffset() {
		return myScrollX;
	}

	private float computeFadingEdgeStrength(int offset, int length) {
		if (offset < length) {
			return offset / (float) length;
		}
		return 1.0f;
	}

	@Override
	protected float getLeftFadingEdgeStrength() {
		if (myWidget == null || myWidget.getWidth() <= getClientWidth()) {
			return 0.0f;
		}
		return computeFadingEdgeStrength(myScrollX, getHorizontalFadingEdgeLength());
	}

	@Override
	protected float getRightFadingEdgeStrength() {
		if (myWidget == null || myWidget.getWidth() <= getClientWidth()) {
			return 0.0f;
		}
		return computeFadingEdgeStrength(myWidget.getWidth() - myScrollX - getClientWidth(),
				getHorizontalFadingEdgeLength());
	}

	@Override
	protected float getTopFadingEdgeStrength() {
		if (myWidget == null || myWidget.getHeight() <= getClientHeight()) {
			return 0.0f;
		}
		return computeFadingEdgeStrength(myScrollY, getVerticalFadingEdgeLength());
	}

	@Override
	protected float getBottomFadingEdgeStrength() {
		if (myWidget == null || myWidget.getHeight() <= getClientHeight()) {
			return 0.0f;
		}
		return computeFadingEdgeStrength(myWidget.getHeight() - myScrollY - getClientHeight(),
				getVerticalFadingEdgeLength());
	}
}