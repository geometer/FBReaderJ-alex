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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geometerplus.fbreader.fbreader.ActionCode;


public class ButtonsCollection {
	private static ButtonsCollection ourInstance;

	private ButtonsCollection() {
	}

	public static ButtonsCollection Instance() {
		if (ourInstance == null) {
			ourInstance = new ButtonsCollection();
		}
		return ourInstance;
	}


	private LinkedList<AbstractButton> myButtons;

	private void createButtons() {
		if (myButtons != null) {
			return;
		}
		myButtons = new LinkedList<AbstractButton>();
		myButtons.add(new FBActionButton("page", ActionCode.GOTO_PAGE));
		myButtons.add(new FBActionButton("search", ActionCode.SEARCH));
		myButtons.add(new FBActionButton("bookmarks", ActionCode.SHOW_BOOKMARKS));
		myButtons.add(new FBActionButton("table_of_contents", ActionCode.SHOW_CONTENTS));
		myButtons.add(new FBActionButton("screen_rotations", ActionCode.ROTATE));
		myButtons.add(new FBActionButton("dictionary", ActionCode.SHOW_DICTIONARY_DIALOG));
		myButtons.add(new FBActionButton("hyperlinks", ActionCode.SHOW_HYPERLINKS_DIALOG));
		myButtons.add(new FBActionButton("settings_fbreader", ActionCode.SHOW_PREFERENCES));
		myButtons.add(new FBActionButton("fonts", ActionCode.FONT_SIZE));
		myButtons.add(new FBActionButton("lib", ActionCode.SHOW_LIBRARY));
		myButtons.add(new FBActionButton("lib_network", ActionCode.SHOW_NETWORK_LIBRARY));
		myButtons.add(new FBActionDecorator("day_night", ActionCode.SWITCH_TO_NIGHT_PROFILE,
				new FBActionButton("day_night", ActionCode.SWITCH_TO_DAY_PROFILE)));
		myButtons.add(new FBActionButton("info", ActionCode.SHOW_BOOK_INFO));
	}

	private void collectDefaultButtons(List<AbstractButton> buttons) {
		createButtons();
		buttons.addAll(myButtons);
	}

	public void loadAllButtons(List<AbstractButton> buttons) {
		collectDefaultButtons(buttons);
	}

	public void loadButtons(List<AbstractButton> buttons) {
		SQLiteButtonsDatabase.Instance().loadButtons(buttons);
		if (buttons.isEmpty()) {
			collectDefaultButtons(buttons);
			return;
		}
		createButtons();
		final Iterator<AbstractButton> it = buttons.iterator();
		while (it.hasNext()) {
			final AbstractButton b = it.next();
			if (!myButtons.contains(b)) {
				it.remove();
			}
		}
	}

	public void saveButtons(List<AbstractButton> buttons) {
		SQLiteButtonsDatabase.Instance().saveButtons(buttons);
	}
}
