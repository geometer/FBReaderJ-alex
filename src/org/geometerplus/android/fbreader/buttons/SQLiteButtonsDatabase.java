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

import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.geometerplus.android.util.UIUtil;

public final class SQLiteButtonsDatabase {
	private final SQLiteDatabase myDatabase;

	private static SQLiteButtonsDatabase ourInstance;

	public static SQLiteButtonsDatabase Instance() {
		return ourInstance;
	}


	public SQLiteButtonsDatabase(Context context) {
		ourInstance = this;
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


	public void loadButtons(List<AbstractButton> buttons) {
		buttons.clear();
        final Cursor c = myDatabase.rawQuery("SELECT btn_type, btn_data FROM Buttons", null);
        while (c.moveToNext()) {
        	final String type = c.getString(0);
        	final String data = c.getString(1);
        	buttons.add(AbstractButton.createButton(type, data));
        }
	}

	private SQLiteStatement mySaveButtonStatement;
	public void saveButtons(final List<AbstractButton> buttons) {
		if (mySaveButtonStatement == null) {
			mySaveButtonStatement = myDatabase.compileStatement("INSERT INTO Buttons (btn_type, btn_data) VALUES (?,?)");
		}
		executeAsATransaction(new Runnable() {
			public void run() {
				myDatabase.delete("Buttons", null, null);
				for (AbstractButton btn: buttons) {
					mySaveButtonStatement.bindString(0, btn.getType());
					bindString(mySaveButtonStatement, 1, btn.getData());
					mySaveButtonStatement.execute();
				}
			}
		});
	}


	private void createTables() {
		myDatabase.execSQL("CREATE TABLE Buttons (" +
				"btn_type TEXT NOT NULL, " +
				"btn_data TEXT," +
				"CONSTRAINT pk_Buttons UNIQUE (btn_type, btn_data))");
	}
}
