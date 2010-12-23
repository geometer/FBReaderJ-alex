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

package org.geometerplus.android.fbreader.network;

import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.ContextMenu;

import org.geometerplus.fbreader.network.NetworkTree;

import org.geometerplus.android.fbreader.network.browser.BrowserActivity;


class BrowserItemActions extends NetworkTreeActions {

	public static final int OPEN_BROWSER_ITEM_ID = 0;


	@Override
	public boolean canHandleTree(NetworkTree tree) {
		return tree instanceof BrowserItemTree;
	}

	@Override
	public void buildContextMenu(NetworkBaseActivity activity, ContextMenu menu, NetworkTree tree) {
	}

	@Override
	public int getDefaultActionCode(NetworkBaseActivity activity, NetworkTree tree) {
		return OPEN_BROWSER_ITEM_ID;
	}

	@Override
	public String getConfirmText(NetworkTree tree, int actionCode) {
		return null;
	}

	@Override
	public boolean createOptionsMenu(Menu menu, NetworkTree tree) {
		return false;
	}

	@Override
	public boolean prepareOptionsMenu(NetworkBaseActivity activity, Menu menu, NetworkTree tree) {
		return false;
	}

	@Override
	public boolean runAction(NetworkBaseActivity activity, NetworkTree tree, int actionCode) {
		switch (actionCode) {
			case OPEN_BROWSER_ITEM_ID:
				doOpenBrowser(activity);
				return true;
		}
		return false;
	}

	private void doOpenBrowser(Context context) {
		context.startActivity(new Intent(context.getApplicationContext(), BrowserActivity.class));
	}
}