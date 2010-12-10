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

package org.geometerplus.fbreader.library;

import org.geometerplus.zlibrary.core.image.ZLImage;

import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.formats.FormatPlugin;

public class BookTree extends LibraryTree {
	public final Book Book;
	private final boolean myShowAuthors;

	BookTree(LibraryTree parent, Book book, boolean showAuthors) {
		super(parent);
		Book = book;
		myShowAuthors = showAuthors;
	}

	@Override
	public String getName() {
		return Book.getTitle();
	}

	@Override
	public String getSummary() {
		if (!myShowAuthors) {
			return super.getSummary();
		}
		StringBuilder builder = new StringBuilder();
		int count = 0;
		for (Author author : Book.authors()) {
			if (count++ > 0) {
				builder.append(",  ");
			}
			builder.append(author.DisplayName);
			if (count == 5) {
				break;
			}
		}
		return builder.toString();
	}

	@Override
	protected ZLImage createCover() {
		final FormatPlugin plugin = PluginCollection.Instance().getPlugin(Book.File);
		if (plugin != null) {
			return plugin.readCover(Book);
		}

		return null;
	}
}
