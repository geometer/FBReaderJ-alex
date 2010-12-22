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

package org.geometerplus.android.fbreader;

import android.content.Intent;

import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

class ShowBookInfoAction extends FBAction {
	private final FBReader myBaseActivity;

	ShowBookInfoAction(FBReader baseActivity, FBReaderApp fbreader) {
		super(fbreader);
		myBaseActivity = baseActivity;
	}

	public boolean isVisible() {
		return Reader.Model != null;
	}

	public void run() {
		myBaseActivity.startActivityForResult(
			new Intent(myBaseActivity.getApplicationContext(), BookInfoActivity.class)
				.putExtra(BookInfoActivity.CURRENT_BOOK_PATH_KEY, Reader.Model.Book.File.getPath())
				.putExtra(BookInfoActivity.HIDE_OPEN_BUTTON_KEY, true),
			FBReader.REPAINT_CODE
		);
	}
}
