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

import android.view.Menu;
import android.view.ContextMenu;

import org.geometerplus.fbreader.network.INetworkLink;
import org.geometerplus.fbreader.network.NetworkTree;
import org.geometerplus.fbreader.network.authentication.NetworkAuthenticationManager;


class RefillAccountActions extends NetworkTreeActions {
	public static final int REFILL_VIA_SMS_ITEM_ID = 0;
	public static final int REFILL_VIA_BROWSER_ITEM_ID = 1;


	@Override
	public boolean canHandleTree(NetworkTree tree) {
		return tree instanceof RefillAccountTree;
	}

	@Override
	public void buildContextMenu(NetworkBaseActivity activity, ContextMenu menu, NetworkTree tree) {
		//final RefillAccountTree refillTree = (RefillAccountTree) tree;
		menu.setHeaderTitle(getTitleValue("refillTitle"));

		final INetworkLink link = ((RefillAccountTree)tree).Link;
		if (Util.isSmsAccountRefillingSupported(activity, link)) {
			addMenuItem(menu, REFILL_VIA_SMS_ITEM_ID, "refillViaSms");
		}
		if (Util.isBrowserAccountRefillingSupported(activity, link)) {
			addMenuItem(menu, REFILL_VIA_BROWSER_ITEM_ID, "refillViaBrowser");
		}
	}

	@Override
	public int getDefaultActionCode(NetworkBaseActivity activity, NetworkTree tree) {
		final INetworkLink link = ((RefillAccountTree)tree).Link;
		final boolean sms = Util.isSmsAccountRefillingSupported(activity, link);
		final boolean browser = Util.isBrowserAccountRefillingSupported(activity, link);

		if (sms && browser) {
			return TREE_SHOW_CONTEXT_MENU;
		} else if (sms) {
			return REFILL_VIA_SMS_ITEM_ID;
		} else /* if (browser) */ { 
			return REFILL_VIA_BROWSER_ITEM_ID;
		}
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
		final INetworkLink link = ((RefillAccountTree)tree).Link;
		Runnable refillRunnable = null;
		switch (actionCode) {
			case REFILL_VIA_SMS_ITEM_ID:
				refillRunnable = smsRefillRunnable(activity, link);
				break;
			case REFILL_VIA_BROWSER_ITEM_ID:
				refillRunnable = browserRefillRunnable(activity, link);
				break;
		}

		if (refillRunnable == null) {
			return false;
		}
		doRefill(activity, link, refillRunnable);
		return true;
	}

	private Runnable browserRefillRunnable(final NetworkBaseActivity activity, final INetworkLink link) {
		return new Runnable() {
			public void run() {
				Util.openInBrowser(
					activity,
					link.authenticationManager().refillAccountLink()
				);
			}
		};
	}

	private Runnable smsRefillRunnable(final NetworkBaseActivity activity, final INetworkLink link) {
		return new Runnable() {
			public void run() {
				// TODO: implement
			}
		};
	}

	private void doRefill(final NetworkBaseActivity activity, final INetworkLink link, final Runnable refiller) {
		final NetworkAuthenticationManager mgr = link.authenticationManager();
		if (mgr.mayBeAuthorised(false)) {
			refiller.run();
		} else {
			NetworkDialog.show(activity, NetworkDialog.DIALOG_AUTHENTICATION, link, new Runnable() {
				public void run() {
					if (mgr.mayBeAuthorised(false)) {
						refiller.run();
					}
				}
			});
		}
	}
}
