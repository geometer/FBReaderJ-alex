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

public class RotatedStringDrawable extends Drawable {

	public static Drawable create(String string, int angle) {
		final Drawable normal = new RotatedStringDrawable(string, angle, false);
		final Drawable pressed = new RotatedStringDrawable(string, angle, true);
		android.graphics.drawable.StateListDrawable image = new StateListDrawable();
		image.addState(new int[]{-android.R.attr.state_pressed}, normal);
		image.addState(new int[]{android.R.attr.state_pressed}, pressed);
		return image;
	}

	private final Paint myTextPaint = new Paint();
	private final String myString;
	private final int myAngle;

	private RotatedStringDrawable(String string, int angle, boolean hilighted) {
		myTextPaint.setLinearText(false);
		myTextPaint.setAntiAlias(true);
		myTextPaint.setSubpixelText(false);
		myTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
		myTextPaint.setColor(hilighted ? Color.rgb(0xF0, 0x68, 0) : Color.rgb(0xF0, 0xF0, 0xF0));
		myTextPaint.setTextAlign(Paint.Align.LEFT);
		myTextPaint.setTextSize(28);
		myTextPaint.setShadowLayer(2.0f, 1.0f, 1.0f, Color.rgb(0x55, 0x55, 0x55));
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
		case 90:
			canvas.rotate(90.0f);
			canvas.translate(0.0f, -h);
			break;
		case 180:
			canvas.rotate(180.0f, w / 2.0f, h / 2.0f);
			break;
		case 270:
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
		if (myAngle == 90 || myAngle == 270) {
			return getStringHeight();
		}
		return getStringWidth();
	}

	@Override
	public int getIntrinsicHeight() {
		if (myAngle == 90 || myAngle == 270) {
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
