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

package org.geometerplus.fbreader.fbreader;

import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.view.*;

import org.geometerplus.fbreader.bookmodel.FBHyperlinkType;

public final class FBView extends ZLTextView {
	private FBReaderApp myReader;

	FBView(FBReaderApp reader) {
		myReader = reader;
	}

	@Override
	public void setModel(ZLTextModel model) {
		//myIsManualScrollingActive = false;
		super.setModel(model);
	}

	/*@Override
	public void onScrollingFinished(int viewPage) {
		super.onScrollingFinished(viewPage);
	}*/

	final void doScrollPage(boolean forward) {
		scrollPage(forward, ZLTextView.ScrollingMode.NO_OVERLAPPING, 0);
		myReader.repaintView();
	}

	//private int myStartX;
	//private int myStartY;
	//private boolean myIsManualScrollingActive;

	@Override
	public boolean onStylusPress(int x, int y) {
		if (super.onStylusPress(x, y)) {
			return true;
		}

		/*if (isScrollingActive()) {
			return false;
		}*/

		final ZLTextHyperlink hyperlink = findHyperlink(x, y, 10);
		if (hyperlink != null) {
			selectHyperlink(hyperlink);
			myReader.repaintView();
			myReader.doAction(ActionCode.PROCESS_HYPERLINK);
			//followHyperlink(hyperlink);
			return true;
		}

//		final ScrollingPreferences preferences = ScrollingPreferences.Instance();
//		if (preferences.FlickOption.getValue()) {
			//myStartX = x;
			//myStartY = y;
			//setScrollingActive(true);
			//myIsManualScrollingActive = true;
//		} else {
//			if (preferences.HorizontalOption.getValue()) {
//				if (x <= myContext.getWidth() / 3) {
//					doScrollPage(false);
//				} else if (x >= myContext.getWidth() * 2 / 3) {
//					doScrollPage(true);
//				}
//			} else {
//				if (y <= myContext.getHeight() / 3) {
//					doScrollPage(false);
//				} else if (y >= myContext.getHeight() * 2 / 3) {
//					doScrollPage(true);
//				}
//			}
//		}

		//activateSelection(x, y);
		return true;
	}

	/*@Override
	public boolean onStylusMovePressed(int x, int y) {
		if (super.onStylusMovePressed(x, y)) {
			return true;
		}

		synchronized (this) {
			if (isScrollingActive() && myIsManualScrollingActive) {
				//final boolean horizontal = ScrollingPreferences.Instance().HorizontalOption.getValue();
				//final int diff = horizontal ? x - myStartX : y - myStartY;
				final int diff = x - myStartX;
				if (diff > 0) {
					ZLTextWordCursor cursor = getStartCursor();
					if (cursor == null || cursor.isNull()) {
						return false;
					}
				} else if (diff < 0) {
					ZLTextWordCursor cursor = getEndCursor();
					if (cursor == null || cursor.isNull()) {
						return false;
					}
				}
				return true;
			}
		}

		return false;
	}*/

	/*@Override
	public boolean onStylusRelease(int x, int y) {
		if (super.onStylusRelease(x, y)) {
			return true;
		}

		synchronized (this) {
			if (isScrollingActive() && myIsManualScrollingActive) {
				setScrollingActive(false);
				myIsManualScrollingActive = false;
				//final boolean horizontal = ScrollingPreferences.Instance().HorizontalOption.getValue();
				//final int diff = horizontal ? x - myStartX : y - myStartY;
				final int diff = x - myStartX;
				boolean doScroll = false;
				if (diff > 0) {
					ZLTextWordCursor cursor = getStartCursor();
					if (cursor != null && !cursor.isNull()) {
						doScroll = !cursor.isStartOfParagraph() || !cursor.getParagraphCursor().isFirst();
					}
				} else if (diff < 0) {
					ZLTextWordCursor cursor = getEndCursor();
					if (cursor != null && !cursor.isNull()) {
						doScroll = !cursor.isEndOfParagraph() || !cursor.getParagraphCursor().isLast();
					}
				}
				if (doScroll) {
					final int h = myContext.getHeight();
					final int w = myContext.getWidth();
//					final int minDiff = horizontal ?
//						((w > h) ? w / 4 : w / 3) :
//						((h > w) ? h / 4 : h / 3);
					final int minDiff = (w > h) ? w / 4 : w / 3;
					int viewPage = PAGE_CENTRAL;
					if (Math.abs(diff) > minDiff) {
						viewPage = (diff < 0) ? PAGE_RIGHT : PAGE_LEFT;
					}
					startAutoScrolling(viewPage);
				}
				return true;
			}
		}
		return false;
	}*/

	@Override
	public boolean onTrackballRotated(int diffX, int diffY) {
		if (diffX == 0 && diffY == 0) {
			return true;
		}

		final int direction = (diffY != 0) ?
			(diffY > 0 ? Direction.DOWN : Direction.UP) :
			(diffX > 0 ? Direction.RIGHT : Direction.LEFT);

		if (!moveRegionPointer(direction)) {
			if (direction == Direction.DOWN) {
				scrollPage(true, ZLTextView.ScrollingMode.SCROLL_LINES, 1);
			} else if (direction == Direction.UP) {
				scrollPage(false, ZLTextView.ScrollingMode.SCROLL_LINES, 1);
			}
		}

		myReader.repaintView();

		return true;
	}

	@Override
	public int getMode() {
		return myReader.TextViewModeOption.getValue();
	}

	@Override
	public int getLeftMargin() {
		return myReader.LeftMarginOption.getValue();
	}

	@Override
	public int getRightMargin() {
		return myReader.RightMarginOption.getValue();
	}

	@Override
	public int getTopMargin() {
		return myReader.TopMarginOption.getValue();
	}

	@Override
	public int getBottomMargin() {
		return myReader.BottomMarginOption.getValue();
	}

	@Override
	public ZLColor getBackgroundColor() {
		return myReader.getColorProfile().BackgroundOption.getValue();
	}

	@Override
	public ZLColor getSelectedBackgroundColor() {
		return myReader.getColorProfile().SelectionBackgroundOption.getValue();
	}

	@Override
	public ZLColor getTextColor(byte hyperlinkType) {
		final ColorProfile profile = myReader.getColorProfile();
		switch (hyperlinkType) {
			default:
			case FBHyperlinkType.NONE:
				return profile.RegularTextOption.getValue();
			case FBHyperlinkType.INTERNAL:
			case FBHyperlinkType.EXTERNAL:
				return profile.HyperlinkTextOption.getValue();
		}
	}

	@Override
	public ZLColor getHighlightingColor() {
		return myReader.getColorProfile().HighlightingOption.getValue();
	}

	@Override
	protected boolean isSelectionEnabled() {
		return myReader.SelectionEnabledOption.getValue();
	}

	@Override
	public int scrollbarType() {
		return SCROLLBAR_HIDE;
	}
}
