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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;

public class RotatedStringDrawable extends Drawable {

	public static Drawable create(String string, int angle, int fontSize) {
		final Drawable disabled = new RotatedStringDrawable(string, angle, fontSize, Color.rgb(0x55, 0x55, 0x55));
		final Drawable normal = new RotatedStringDrawable(string, angle, fontSize, Color.rgb(0xF0, 0xF0, 0xF0));
		final Drawable pressed = new RotatedStringDrawable(string, angle, fontSize, Color.rgb(0xF0, 0x68, 0));
		android.graphics.drawable.StateListDrawable image = new StateListDrawable();
		image.addState(new int[]{-android.R.attr.state_enabled}, disabled);
		image.addState(new int[]{android.R.attr.state_pressed}, pressed);
		image.addState(new int[]{}, normal);
		return image;
	}

	private final Paint myTextPaint = new Paint();
	private final String myString;
	private final int myAngle;

	private RotatedStringDrawable(String string, int angle, int fontSize, int color) {
		myTextPaint.setLinearText(false);
		myTextPaint.setAntiAlias(true);
		myTextPaint.setSubpixelText(false);
		myTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
		myTextPaint.setColor(color);
		myTextPaint.setTextAlign(Paint.Align.LEFT);
		myTextPaint.setTextSize(fontSize);
		myTextPaint.setShadowLayer(2.0f, 1.0f, 1.0f, Color.rgb(0x70, 0x70, 0x70));
		myTextPaint.measureText("M");

		myString = string;
		myAngle = angle;
	}

	private int getStringWidth() {
		return (int)(myTextPaint.measureText(myString) + 0.5f);
	}

	private int getStringHeight() {
		return (int)(myTextPaint.getTextSize() + 0.5f);
	}

	@Override
	public void draw(Canvas canvas) {
		final int w = getStringWidth();
		final int h = getStringHeight();
		canvas.save();
		switch (myAngle) {
		case ZLAndroidApplication.ROTATE_90:
			canvas.rotate(90.0f);
			canvas.translate(0.0f, -h);
			break;
		case ZLAndroidApplication.ROTATE_180:
			canvas.rotate(180.0f, w / 2.0f, h / 2.0f);
			break;
		case ZLAndroidApplication.ROTATE_270:
			canvas.rotate(-90.0f);
			canvas.translate(-w, 0.0f);
			break;
		}
		canvas.drawText(
			myString,
			0.0f,
			myTextPaint.getTextSize() - myTextPaint.descent(),
			myTextPaint
		);
		canvas.restore();
	}

	@Override
	public int getIntrinsicWidth() {
		if (myAngle == ZLAndroidApplication.ROTATE_90 ||
				myAngle == ZLAndroidApplication.ROTATE_270) {
			return getStringHeight();
		}
		return getStringWidth();
	}

	@Override
	public int getIntrinsicHeight() {
		if (myAngle == ZLAndroidApplication.ROTATE_90 ||
				myAngle == ZLAndroidApplication.ROTATE_270) {
			return getStringWidth();
		}
		return getStringHeight();
	}

	@Override
	public int getOpacity() {
		return android.graphics.PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha(int alpha) {
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
	}
}
