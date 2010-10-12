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

package org.geometerplus.zlibrary.core.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class ZLLog {

	private static ZLLog ourInstance;

	public static ZLLog Instance() {
		if (ourInstance == null) {
			ourInstance = new ZLLog();
		}
		return ourInstance;
	}

	private Date myBase;
	private ArrayList<String> myLines = new ArrayList<String>();
	private ArrayList<Date> myDates = new ArrayList<Date>();

	private ZLLog() {
		myLines.ensureCapacity(128);
		myDates.ensureCapacity(128);
	}

	private void log0(String line) {
		myDates.add(new Date());
		myLines.add(line);
	}

	private void setBase0() {
		myBase = new Date();
	}

	private void flush0() {
		try {
			final File file = new File("/sdcard/fblog");
			final FileWriter fw = new FileWriter(file, true);
			final StringBuilder sb = new StringBuilder();
			sb.append("base time");
			if (myBase == null) {
				sb.append(" has not been set; set now");
				myBase = new Date();
			}
			sb.append("\t").append(myBase.toLocaleString()).append("\t")
				.append(myBase.getTime()).append("\n");
			for (int i = 0; i < myLines.size(); ++i) {
				sb.append(myDates.get(i).getTime() - myBase.getTime()).append("\t")
					.append(myLines.get(i)).append("\n");
			}
			fw.append(sb);
			fw.close();
		} catch (IOException e) {
		} finally {
			myDates.clear();
			myLines.clear();
		}
	}


	public static void log(String line) {
		Instance().log0(line);
	}

	public static void setBase() {
		Instance().setBase0();
	}

	public static void flush() {
		Instance().flush0();
	}
}
