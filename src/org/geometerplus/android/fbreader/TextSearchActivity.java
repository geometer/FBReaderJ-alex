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

import android.app.Activity;

import org.geometerplus.fbreader.fbreader.FBReader;

public class TextSearchActivity extends SearchActivity {
	@Override
	void onSuccess() {
		FBReaderActivity.Instance.showTextSearchControls(true);
	}

	@Override
	void onFailure() {
		FBReaderActivity.Instance.showTextSearchControls(false);
	}

	@Override
	String getFailureMessageResourceKey() {
		return "textNotFound";
	}

	@Override
	String getWaitMessageResourceKey() {
		return "search";
	}

	@Override
	boolean runSearch(final String pattern) {
		final FBReader fbReader = (FBReader)FBReader.Instance();
		fbReader.TextSearchPatternOption.setValue(pattern);
		return fbReader.getTextView().search(pattern, true, false, false, false) != 0;
	}

	@Override
	Activity getParentActivity() {
		return FBReaderActivity.Instance;
	}
}
