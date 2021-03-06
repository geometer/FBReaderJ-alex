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

import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.text.view.style.ZLTextStyleCollection;

class ChangeFontSizeAction extends FBAction {
	private final int myDelta;

	ChangeFontSizeAction(FBReaderApp fbreader, int delta) {
		super(fbreader);
		myDelta = delta;
	}

	@Override
	public boolean isEnabled() {
		ZLIntegerRangeOption option =
			ZLTextStyleCollection.Instance().getBaseStyle().FontSizeOption;
		final int nextValue = option.getValue() + myDelta;
		return option.MinValue <= nextValue && nextValue <= option.MaxValue;
	}

	public void run() {
		ZLIntegerRangeOption option =
			ZLTextStyleCollection.Instance().getBaseStyle().FontSizeOption;
		option.setValue(option.getValue() + myDelta);
		Reader.clearTextCaches();
		Reader.repaintView();
	}
}
