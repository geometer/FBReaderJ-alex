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

package org.geometerplus.fbreader.formats.plucker;

import java.io.IOException;
import java.io.InputStream;

import org.geometerplus.zlibrary.core.image.ZLSingleImage;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;

public class PluckerFileImage extends ZLSingleImage {
	private final ZLFile myFile;
	private final int myOffset;
	private final int mySize;

	public PluckerFileImage(String mimeType, final ZLFile file, final int offset, final int size) {
		super(mimeType);
		myFile = file;
		myOffset = offset;
		mySize = size;
	}

	public byte[] byteData() {
		try {
			final InputStream stream = myFile.getInputStream();
			if (stream == null) {
				return new byte[0];
			}

			stream.skip(myOffset);
			byte [] buffer = new byte[mySize];
			stream.read(buffer, 0, mySize);
			return buffer;
		} catch (IOException e) {}
		
		return new byte[0];
	}
}
