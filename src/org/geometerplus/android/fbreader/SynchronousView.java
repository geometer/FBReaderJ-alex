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
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;


public class SynchronousView extends View {

	private ZLAndroidWidget myWidget;

    private int myMinimumVelocity;

	private VelocityTracker myVelocityTracker;
	private Scroller myScroller;

	private float myLastScrollX; 
	private float myLastScrollY; 

	private int myScrollX;
	private int myScrollY;
	private boolean myInvalidScroll;

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

	public void setWidget(ZLAndroidWidget widget) {
		myWidget = widget;
	}

	public void invalidateScroll() {
		myInvalidScroll = true;
	}

	public void resetScroll() {
		if (myInvalidScroll) {
			myScrollX = myScrollY = 0;
		}
		myInvalidScroll = false;
	}


	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (myWidget == null) {
			return;
		}
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
		canvas.drawBitmap(myWidget.getBitmap(),
				getPaddingLeft() - myScrollX, getPaddingTop() - myScrollY, null);
		canvas.restore();
	}

	private int getClientHeight() {
		return getHeight() - getPaddingBottom() - getPaddingTop();
	}

	private int getClientWidth() {
		return getWidth() - getPaddingLeft() - getPaddingRight();
	}

	private int getWidgetX(float viewX) {
		final int value = (int) (viewX - getPaddingLeft() + myScrollX);
		return Math.max(0, Math.min(value, myWidget.getWidth()));
	}

	private int getWidgetY(float viewY) {
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

        if (myVelocityTracker == null) {
        	myVelocityTracker = VelocityTracker.obtain();
        }
        myVelocityTracker.addMovement(event);

		final int action = event.getAction();
		final float x = event.getX();
		final float y = event.getY();

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
			if (myScrollY == 0) {
				myScrollPage = -1;
			} else if (myScrollY == myWidget.getHeight() - getClientHeight()) {
				myScrollPage = 1;
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
				final int deltaX = (int) (myLastScrollX - x);
				final int deltaY = (int) (myLastScrollY - y);
				myLastScrollX = x;
				myLastScrollY = y;
				myScrollX = Math.max(0, Math.min(myScrollX + deltaX, myWidget.getWidth() - getClientWidth()));
				myScrollY = Math.max(0, Math.min(myScrollY + deltaY, myWidget.getHeight() - getClientHeight()));
			}
			break;
			
		case MotionEvent.ACTION_UP:
			if (myPendingClick) {
				if (x > getPaddingLeft() && x < getWidth() - getPaddingRight()
						&& y > getPaddingTop() && y < getHeight() - getPaddingBottom()) {
					final ZLView view = ZLApplication.Instance().getCurrentView();
					final int stopX = getWidgetX(x);
					final int stopY = getWidgetY(y);
					view.onStylusPress(getWidgetX(myStartScrollX), getWidgetY(myStartScrollY));
					view.onStylusMovePressed(stopX, stopY);
					view.onStylusRelease(stopX, stopY);
					ZLApplication.Instance().repaintView();
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

				if (myScrollPage * velocityY < 0
						&& Math.abs(myStartScrollX - x) < getWidth() / 4
						&& Math.abs(myStartScrollY - y) > getHeight() / 3 ) {
					myInvalidScroll = true;
					EPDView.Instance().scrollPage(myScrollPage > 0);
				} else {
					final int maxX = Math.max(0, myWidget.getWidth() - getClientWidth());
					final int maxY = Math.max(0, myWidget.getHeight() - getClientHeight());
					myScroller.fling(myScrollX, myScrollY, -velocityX, -velocityY,
							0, maxX, 0, maxY);
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