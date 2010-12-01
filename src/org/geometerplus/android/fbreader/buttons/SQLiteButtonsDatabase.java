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

package org.geometerplus.android.fbreader.buttons;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.database.SQLException;
import android.database.Cursor;


import org.geometerplus.android.util.UIUtil;

final class SQLiteButtonsDatabase {
	private final SQLiteDatabase myDatabase;

	public SQLiteButtonsDatabase(Context context) {
		myDatabase = context.openOrCreateDatabase("buttons.db", Context.MODE_PRIVATE, null);
		migrate(context);
	}

	protected void executeAsATransaction(Runnable actions) {
		myDatabase.beginTransaction();
		try {
			actions.run();
			myDatabase.setTransactionSuccessful();
		} finally {
			myDatabase.endTransaction();
		}
	}

	private void migrate(Context context) {
		final int version = myDatabase.getVersion();
		final int currentVersion = 1;
		if (version >= currentVersion) {
			return;
		}
		UIUtil.wait((version == 0) ? "creatingButtonsDatabase" : "updatingButtonsDatabase", new Runnable() {
			public void run() {
				myDatabase.beginTransaction();

				switch (version) {
					case 0:
						createTables();
				}
				myDatabase.setTransactionSuccessful();
				myDatabase.endTransaction();

				myDatabase.execSQL("VACUUM");
				myDatabase.setVersion(currentVersion);
			}
		}, context);
	}

	private static void bindString(SQLiteStatement statement, int index, String value) {
		if (value != null) {
			statement.bindString(index, value);
		} else {
			statement.bindNull(index);
		}
	}



	private void createTables() {
		myDatabase.execSQL("CREATE TABLE Buttons (btn_id TEXT, btn_data TEXT)");
	}
}
