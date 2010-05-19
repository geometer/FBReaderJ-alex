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

import java.util.List;

import android.app.AlertDialog;
import android.os.Message;
import android.os.Handler;
import android.view.Menu;
import android.view.ContextMenu;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.util.ZLBoolean3;

import org.geometerplus.zlibrary.ui.android.dialogs.ZLAndroidDialogManager;

import org.geometerplus.fbreader.network.*;
import org.geometerplus.fbreader.network.tree.NetworkTreeFactory;
import org.geometerplus.fbreader.network.tree.NetworkCatalogTree;
import org.geometerplus.fbreader.network.tree.NetworkCatalogRootTree;
import org.geometerplus.fbreader.network.authentication.*;


class NetworkCatalogActions extends NetworkTreeActions {

	public static final int OPEN_CATALOG_ITEM_ID = 0;
	public static final int OPEN_IN_BROWSER_ITEM_ID = 1;
	public static final int RELOAD_ITEM_ID = 2;
	public static final int SIGNIN_ITEM_ID = 4;
	public static final int SIGNOUT_ITEM_ID = 5;
	public static final int REFILL_ACCOUNT_ITEM_ID = 6;


	@Override
	public boolean canHandleTree(NetworkTree tree) {
		return tree instanceof NetworkCatalogTree;
	}

	@Override
	public String getTreeTitle(NetworkTree tree) {
		if (tree instanceof NetworkCatalogRootTree) {
			return tree.getName();
		}
		return tree.getName() + " - " + ((NetworkCatalogTree) tree).Item.Link.SiteName;
	}

	@Override
	public void buildContextMenu(NetworkBaseActivity activity, ContextMenu menu, NetworkTree tree) {
		final NetworkCatalogTree catalogTree = (NetworkCatalogTree) tree;
		final NetworkCatalogItem item = catalogTree.Item;
		menu.setHeaderTitle(tree.getName());

		final boolean isVisible = item.getVisibility() == ZLBoolean3.B3_TRUE;
		boolean hasItems = false;

		final String catalogUrl = item.URLByType.get(NetworkCatalogItem.URL_CATALOG);
		if (catalogUrl != null) {
			addMenuItem(menu, OPEN_CATALOG_ITEM_ID, "openCatalog");
			hasItems = true;
		}

		if (tree instanceof NetworkCatalogRootTree) {
			if (isVisible) {
				final NetworkAuthenticationManager mgr = item.Link.authenticationManager();
				if (mgr != null) {
					final boolean maybeSignedIn = mgr.isAuthorised(false).Status != ZLBoolean3.B3_FALSE;
					if (maybeSignedIn) {
						addMenuItem(menu, SIGNOUT_ITEM_ID, "signOut", mgr.currentUserName());
						if (mgr.refillAccountLink() != null) {
							final String account = mgr.currentAccount();
							if (account != null) {
								addMenuItem(menu, REFILL_ACCOUNT_ITEM_ID, "refillAccount", account);
							}
						}
					} else {
						addMenuItem(menu, SIGNIN_ITEM_ID, "signIn");
						//if (mgr.passwordRecoverySupported()) {
						//	registerAction(new PasswordRecoveryAction(mgr), true);
						//}
					}
				}
			}
		} else {
			if (item.URLByType.get(NetworkCatalogItem.URL_HTML_PAGE) != null) {
				addMenuItem(menu, OPEN_IN_BROWSER_ITEM_ID, "openInBrowser");
				hasItems = true;
			}
		}

		if (!isVisible && !hasItems) {
			switch (item.Visibility) {
			case NetworkCatalogItem.VISIBLE_LOGGED_USER:
				if (item.Link.authenticationManager() != null) {
					addMenuItem(menu, SIGNIN_ITEM_ID, "signIn");
				}
				break;
			}
			return;
		}
	}

	@Override
	public int getDefaultActionCode(NetworkTree tree) {
		final NetworkCatalogTree catalogTree = (NetworkCatalogTree) tree;
		final NetworkCatalogItem item = catalogTree.Item;
		if (item.URLByType.get(NetworkCatalogItem.URL_CATALOG) != null) {
			return OPEN_CATALOG_ITEM_ID;
		}
		if (item.URLByType.get(NetworkCatalogItem.URL_HTML_PAGE) != null) {
			return OPEN_IN_BROWSER_ITEM_ID;
		}
		if (item.getVisibility() != ZLBoolean3.B3_TRUE) {
			switch (item.Visibility) {
			case NetworkCatalogItem.VISIBLE_LOGGED_USER:
				if (item.Link.authenticationManager() != null) {
					return SIGNIN_ITEM_ID;
				}
				break;
			}
			return TREE_NO_ACTION;
		}
		return TREE_NO_ACTION;
	}

	@Override
	public String getConfirmText(NetworkTree tree, int actionCode) {
		if (actionCode == OPEN_IN_BROWSER_ITEM_ID) {
			return getConfirmValue("openInBrowser");
		}
		return null;
	}

	@Override
	public boolean createOptionsMenu(Menu menu, NetworkTree tree) {
		addOptionsItem(menu, RELOAD_ITEM_ID, "reload");
		addOptionsItem(menu, SIGNIN_ITEM_ID, "signIn");
		addOptionsItem(menu, SIGNOUT_ITEM_ID, "signOut", "");
		addOptionsItem(menu, REFILL_ACCOUNT_ITEM_ID, "refillAccount");
		return true;
	}

	@Override
	public boolean prepareOptionsMenu(Menu menu, NetworkTree tree) {
		final NetworkCatalogTree catalogTree = (NetworkCatalogTree) tree;
		final NetworkCatalogItem item = catalogTree.Item;

		final String catalogUrl = item.URLByType.get(NetworkCatalogItem.URL_CATALOG);
		final boolean isLoading = (catalogUrl != null) ?
			NetworkView.Instance().containsItemsLoadingRunnable(catalogUrl) : false;

		prepareOptionsItem(menu, RELOAD_ITEM_ID, catalogUrl != null && !isLoading);

		boolean signIn = false;
		boolean signOut = false;
		boolean refill = false;
		String userName = null;
		String account = null;
		NetworkAuthenticationManager mgr = item.Link.authenticationManager();
		if (mgr != null) {
			if (mgr.isAuthorised(false).Status != ZLBoolean3.B3_FALSE) {
				userName = mgr.currentUserName();
				signOut = true;
				account = mgr.currentAccount();
				if (mgr.refillAccountLink() != null && account != null) {
					refill = true;
				}
			} else {
				signIn = true;
				//if (mgr.passwordRecoverySupported()) {
				//	registerAction(new PasswordRecoveryAction(mgr), true);
				//}
			}
		}
		prepareOptionsItem(menu, SIGNIN_ITEM_ID, signIn);
		prepareOptionsItem(menu, SIGNOUT_ITEM_ID, signOut, "signOut", userName);
		prepareOptionsItem(menu, REFILL_ACCOUNT_ITEM_ID, refill);
		return true;
	}

	private boolean consumeByVisibility(final NetworkBaseActivity activity, final NetworkTree tree, final int actionCode) {
		final NetworkCatalogTree catalogTree = (NetworkCatalogTree) tree;
		if (catalogTree.Item.getVisibility() == ZLBoolean3.B3_TRUE) {
			return false;
		}
		switch (catalogTree.Item.Visibility) {
		case NetworkCatalogItem.VISIBLE_LOGGED_USER:
			NetworkDialog.show(activity, NetworkDialog.DIALOG_AUTHENTICATION, ((NetworkCatalogTree)tree).Item.Link, new Runnable() {
				public void run() {
					if (catalogTree.Item.getVisibility() != ZLBoolean3.B3_TRUE) {
						return;
					}
					if (actionCode != SIGNIN_ITEM_ID) {
						runAction(activity, tree, actionCode);
					}
				}
			});
			break;
		}
		return true;
	}

	@Override
	public boolean runAction(NetworkBaseActivity activity, NetworkTree tree, int actionCode) {
		if (consumeByVisibility(activity, tree, actionCode)) {
			return true;
		}
		switch (actionCode) {
			case OPEN_CATALOG_ITEM_ID:
				doExpandCatalog(activity, (NetworkCatalogTree)tree);
				return true;
			case OPEN_IN_BROWSER_ITEM_ID:
				NetworkView.Instance().openInBrowser(
					activity,
					((NetworkCatalogTree)tree).Item.URLByType.get(NetworkCatalogItem.URL_HTML_PAGE)
				);
				return true;
			case RELOAD_ITEM_ID:
				doReloadCatalog(activity, (NetworkCatalogTree)tree);
				return true;
			case SIGNIN_ITEM_ID:
				NetworkDialog.show(activity, NetworkDialog.DIALOG_AUTHENTICATION, ((NetworkCatalogTree)tree).Item.Link, null);
				return true;
			case SIGNOUT_ITEM_ID:
				doSignOut(activity, (NetworkCatalogTree)tree);
				return true;
			case REFILL_ACCOUNT_ITEM_ID:
				NetworkView.Instance().openInBrowser(
					activity,
					((NetworkCatalogTree)tree).Item.Link.authenticationManager().refillAccountLink()
				);
				return true;
		}
		return false;
	}


	private static class ExpandCatalogHandler extends ItemsLoadingHandler {

		private final String myKey;
		private final NetworkCatalogTree myTree;

		ExpandCatalogHandler(NetworkCatalogTree tree, String key) {
			myTree = tree;
			myKey = key;
		}

		public void onUpdateItems(List<NetworkLibraryItem> items) {
			for (NetworkLibraryItem item: items) {
				myTree.ChildrenItems.add(item);
				NetworkTreeFactory.createNetworkTree(myTree, item);
			}
		}

		public void afterUpdateItems() {
			if (NetworkView.Instance().isInitialized()) {
				NetworkView.Instance().fireModelChanged();
			}
		}

		public void onFinish(String errorMessage, boolean interrupted) {
			if (interrupted) {
				myTree.ChildrenItems.clear();
				myTree.clear();
			} else {
				myTree.updateLoadedTime();
				afterUpdateCatalog(errorMessage, myTree.ChildrenItems.size() == 0);
				final NetworkLibrary library = NetworkLibrary.Instance();
				library.invalidateVisibility();
				library.synchronize();
			}
			if (NetworkView.Instance().isInitialized()) {
				NetworkView.Instance().fireModelChanged();
			}
		}

		private void afterUpdateCatalog(String errorMessage, boolean childrenEmpty) {
			final ZLResource dialogResource = ZLResource.resource("dialog");
			ZLResource boxResource = null;
			String msg = null;
			if (errorMessage != null) {
				boxResource = dialogResource.getResource("networkError");
				msg = errorMessage;
			} else if (childrenEmpty) {
				// TODO: make ListView's empty view instead
				boxResource = dialogResource.getResource("emptyCatalogBox");
				msg = boxResource.getResource("message").getValue();
			}
			if (msg != null) {
				if (NetworkView.Instance().isInitialized()) {
					final NetworkCatalogActivity activity = NetworkView.Instance().getOpenedActivity(myKey);
					if (activity != null) {
						final ZLResource buttonResource = dialogResource.getResource("button");
						new AlertDialog.Builder(activity)
							.setTitle(boxResource.getResource("title").getValue())
							.setMessage(msg)
							.setIcon(0)
							.setPositiveButton(buttonResource.getResource("ok").getValue(), null)
							.create().show();
					}
				}
			}
		}
	}

	private static class ExpandCatalogRunnable extends ItemsLoadingRunnable {

		private final NetworkCatalogTree myTree;
		private final boolean myCheckAuthentication;

		public ExpandCatalogRunnable(ItemsLoadingHandler handler, NetworkCatalogTree tree, boolean checkAuthentication) {
			super(handler);
			myTree = tree;
			myCheckAuthentication = checkAuthentication;
		}

		public String getResourceKey() {
			return "downloadingCatalogs";
		}

		public String doBefore() {
			/*if (!NetworkOperationRunnable::tryConnect()) {
				return;
			}*/
			final NetworkLink link = myTree.Item.Link;
			if (myCheckAuthentication && link.authenticationManager() != null) {
				NetworkAuthenticationManager mgr = link.authenticationManager();
				AuthenticationStatus auth = mgr.isAuthorised(true);
				if (auth.Message != null) {
					return auth.Message;
				}
				if (auth.Status == ZLBoolean3.B3_TRUE && mgr.needsInitialization()) {
					final String err = mgr.initialize();
					if (err != null) {
						mgr.logOut();
					}
				}
			}
			return null;
		}

		public String doLoading(NetworkOperationData.OnNewItemListener doWithListener) {
			return myTree.Item.loadChildren(doWithListener);
		}
	}

	public void doExpandCatalog(final NetworkBaseActivity activity, final NetworkCatalogTree tree) {
		final String url = tree.Item.URLByType.get(NetworkCatalogItem.URL_CATALOG);
		if (url == null) {
			throw new RuntimeException("That's impossible!!!");
		}
		NetworkView.Instance().tryResumeLoading(activity, tree, url, new Runnable() {
			public void run() {
				if (tree.hasChildren()) {
					if (tree.isContentValid()) {
						NetworkView.Instance().openTree(activity, tree, url);
						return;
					} else {
						tree.ChildrenItems.clear();
						tree.clear();
						NetworkView.Instance().fireModelChanged();
					}
				}
				final ExpandCatalogHandler handler = new ExpandCatalogHandler(tree, url);
				NetworkView.Instance().startItemsLoading(
					activity,
					url,
					new ExpandCatalogRunnable(handler, tree, true)
				);
				NetworkView.Instance().openTree(activity, tree, url);
			}
		});
	}

	public void doReloadCatalog(NetworkBaseActivity activity, final NetworkCatalogTree tree) {
		final String url = tree.Item.URLByType.get(NetworkCatalogItem.URL_CATALOG);
		if (url == null) {
			throw new RuntimeException("That's impossible!!!");
		}
		if (NetworkView.Instance().containsItemsLoadingRunnable(url)) {
			return;
		}
		tree.ChildrenItems.clear();
		tree.clear();
		NetworkView.Instance().fireModelChanged();
		final ExpandCatalogHandler handler = new ExpandCatalogHandler(tree, url);
		NetworkView.Instance().startItemsLoading(
			activity,
			url,
			new ExpandCatalogRunnable(handler, tree, false)
		);
	}

	private void doSignOut(NetworkBaseActivity activity, NetworkCatalogTree tree) {
		final Handler handler = new Handler() {
			public void handleMessage(Message message) {
				final NetworkLibrary library = NetworkLibrary.Instance();
				library.invalidateVisibility();
				library.synchronize();
				if (NetworkView.Instance().isInitialized()) {
					NetworkView.Instance().fireModelChanged();
				}
			}
		};
		final NetworkAuthenticationManager mgr = tree.Item.Link.authenticationManager();
		final Runnable runnable = new Runnable() {
			public void run() {
				if (mgr.isAuthorised(false).Status != ZLBoolean3.B3_FALSE) {
					mgr.logOut();
					handler.sendEmptyMessage(0);
				}
			}
		};
		((ZLAndroidDialogManager)ZLAndroidDialogManager.Instance()).wait("signOut", runnable, activity);
	}
}
