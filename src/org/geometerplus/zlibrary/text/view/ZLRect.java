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

package org.geometerplus.zlibrary.text.view;

public class ZLRect {
	public int Left;
	public int Right;
	public int Top;
	public int Bottom;

	ZLRect(int left, int right, int top, int bottom) {
		Left = left;
		Right = right;
		Top = top;
		Bottom = bottom;
	}

	ZLRect(ZLRect orig) {
		Left = orig.Left;
		Right = orig.Right;
		Top = orig.Top;
		Bottom = orig.Bottom;
	}

	public void merge(ZLRect rect) {
		if (rect == null) {
			return;
		}
		if (rect.Left < Left) {
			Left = rect.Left;
		}
		if (rect.Right > Right) {
			Right = rect.Right;
		}
		if (rect.Top < Top) {
			Top = rect.Top;
		}
		if (rect.Bottom > Bottom) {
			Bottom = rect.Bottom;
		}
	}

	@Override
	public String toString() {
		return "ZLRect[(" + Left + "," + Top + "),(" + Right + "," + Bottom + ")]";
	}
}
