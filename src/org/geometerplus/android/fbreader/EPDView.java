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

package org.geometerplus.android.fbreader;

import java.util.List;

import android.widget.EpdRender;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.options.ZLStringOption;
import org.geometerplus.zlibrary.core.view.ZLView;

import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.view.style.ZLTextStyleCollection;


class EPDView extends EpdRender {

	private static EPDView ourInstance;

	public static EPDView Instance() {
		if (ourInstance == null) {
			ourInstance = new EPDView();
		}
		return ourInstance;
	}


	private EPDView() {
	}

	@Override
	public boolean onPageUp(int arg1, int arg2) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view instanceof ZLTextView) {
			((ZLTextView) view).scrollPage(false, ZLTextView.ScrollingMode.NO_OVERLAPPING, 0);
		}
		ZLApplication.Instance().repaintView();
		return true;
	}

	@Override
	public boolean onPageDown(int arg1, int arg2) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view instanceof ZLTextView) {
			((ZLTextView) view).scrollPage(true, ZLTextView.ScrollingMode.NO_OVERLAPPING, 0);
		}
		ZLApplication.Instance().repaintView();
		return true;
	}

	@Override
	public boolean onTogglePressed(int arg1, int arg2) {
		final List<String> families = ZLibrary.Instance().getPaintContext().fontFamilies();
		if (families.size() == 0) {
			return true;
		}
		final ZLStringOption option = ZLTextStyleCollection.Instance().getBaseStyle().FontFamilyOption;
		final int index = (families.indexOf(option.getValue()) + 1) % families.size();
		System.err.println("family: " + index + "(" + families.size() + ") -- " + families.get(index));
		option.setValue(families.get(index));
		((org.geometerplus.fbreader.fbreader.FBReader)ZLApplication.Instance()).clearTextCaches();
		ZLApplication.Instance().repaintView();
		return true;
	}
}