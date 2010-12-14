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

package org.geometerplus.zlibrary.text.view;

import java.util.*;

import org.geometerplus.zlibrary.core.view.ZLPaintContext;

class ZLTextHorizontalConvexHull {
	private final LinkedList<ZLRect> myRectangles = new LinkedList<ZLRect>();

	ZLTextHorizontalConvexHull(List<ZLTextElementArea> textAreas) {
		for (ZLTextElementArea area : textAreas) {
			addArea(area);
		}
		normalize();
	}

	private void addArea(ZLTextElementArea area) {
		if (myRectangles.isEmpty()) {
			myRectangles.add(new ZLRect(area.XStart, area.XEnd, area.YStart, area.YEnd));
			return;
		}
		final int top = area.YStart;
		final int bottom = area.YEnd;
		for (ListIterator<ZLRect> iter = myRectangles.listIterator(); iter.hasNext(); ) {
			ZLRect r = iter.next();
			if (r.Bottom <= top) {
				continue;
			}
			if (r.Top >= bottom) {
				break;
			}
			if (r.Top < top) {
				final ZLRect before = new ZLRect(r);
				before.Bottom = top;
				r.Top = top;
				iter.previous();
				iter.add(before);
				iter.next();
			}
			if (r.Bottom > bottom) {
				final ZLRect after = new ZLRect(r);
				after.Top = bottom;
				r.Bottom = bottom;
				iter.add(after);
			}
			r.Left = Math.min(r.Left, area.XStart);
			r.Right = Math.max(r.Right, area.XEnd);
		}

		final ZLRect first = myRectangles.getFirst();
		if (top < first.Top) {
			myRectangles.add(0, new ZLRect(area.XStart, area.XEnd, top, Math.min(bottom, first.Top)));
		}

		final ZLRect last = myRectangles.getLast();
		if (bottom > last.Bottom) {
			myRectangles.add(new ZLRect(area.XStart, area.XEnd, Math.max(top, last.Bottom), bottom));
		}
	}

	private void normalize() {
		ZLRect previous = null;
		for (ListIterator<ZLRect> iter = myRectangles.listIterator(); iter.hasNext(); ) {
			final ZLRect current = iter.next();
			if (previous != null) {
				if ((previous.Left == current.Left) && (previous.Right == current.Right)) {
					previous.Bottom = current.Bottom;
					iter.remove();
					continue;
				}
				if ((previous.Bottom != current.Top) &&
					(current.Left <= previous.Right) &&
					(previous.Left <= current.Right)) {
					iter.previous();
					iter.add(new ZLRect(
						Math.max(previous.Left, current.Left),
						Math.min(previous.Right, current.Right),
						previous.Bottom,
						current.Top
					));
					iter.next();
				}
			}
			previous = current;
		}
	}

	int distanceTo(int x, int y) {
		int distance = Integer.MAX_VALUE;
		for (ZLRect r : myRectangles) {
			final int xd = (r.Left > x) ? r.Left - x : ((r.Right < x) ? x - r.Right : 0);
			final int yd = (r.Top > y) ? r.Top - y : ((r.Bottom < y) ? y - r.Bottom : 0);
			distance = Math.min(distance, Math.max(xd, yd));
			if (distance == 0) {
				break;
			}
		}
		return distance;
	}

	ZLRect getRect() {
		if (myRectangles.isEmpty()) {
			return null;
		}
		int left = Integer.MAX_VALUE;
		int right = Integer.MIN_VALUE;
		int top = Integer.MAX_VALUE;
		int bottom = Integer.MIN_VALUE;
		for (ZLRect r : myRectangles) {
			if (r.Left < left) {
				left = r.Left;
			}
			if (r.Right > right) {
				right = r.Right;
			}
			if (r.Top < top) {
				top = r.Top;
			}
			if (r.Bottom > bottom) {
				bottom = r.Bottom;
			}
		}
		return new ZLRect(left - 3, right + 5, top - 3, bottom + 5);
	}

	void draw(ZLPaintContext context) {
		final LinkedList<ZLRect> rectangles = new LinkedList<ZLRect>(myRectangles);
		while (!rectangles.isEmpty()) {
			final LinkedList<ZLRect> connected = new LinkedList<ZLRect>();
			ZLRect previous = null;
			for (final Iterator<ZLRect> iter = rectangles.iterator(); iter.hasNext(); ) {
				final ZLRect current = iter.next();
				if ((previous != null) &&
					((previous.Left > current.Right) || (current.Left > previous.Right))) {
					break;
				}
				iter.remove();
				connected.add(current);
				previous = current;
			}

			final LinkedList<Integer> xList = new LinkedList<Integer>();
			final LinkedList<Integer> yList = new LinkedList<Integer>();
			int x = 0, xPrev = 0;

			final ListIterator<ZLRect> iter = connected.listIterator();
			ZLRect r = iter.next();
			x = r.Right + 2;
			xList.add(x); yList.add(r.Top);
			while (iter.hasNext()) {
				xPrev = x;
				r = iter.next();
				x = r.Right + 2;
				if (x != xPrev) {
					final int y = (x < xPrev) ? r.Top + 2 : r.Top;
					xList.add(xPrev); yList.add(y);
					xList.add(x); yList.add(y);
				}
			}
			xList.add(x); yList.add(r.Bottom + 2);

			r = iter.previous();
			x = r.Left - 2;
			xList.add(x); yList.add(r.Bottom + 2);
			while (iter.hasPrevious()) {
				xPrev = x;
				r = iter.previous();
				x = r.Left - 2;
				if (x != xPrev) {
					final int y = (x > xPrev) ? r.Bottom : r.Bottom + 2;
					xList.add(xPrev); yList.add(y);
					xList.add(x); yList.add(y);
				}
			}
			xList.add(x); yList.add(r.Top);

			final int xs[] = new int[xList.size()];
			final int ys[] = new int[yList.size()];
			int count = 0;
			for (int xx : xList) {
				xs[count++] = xx;
			}
			count = 0;
			for (int yy : yList) {
				ys[count++] = yy;
			}
			context.drawOutline(xs, ys);
		}
	}
}
