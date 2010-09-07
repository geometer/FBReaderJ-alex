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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.*;
import android.widget.BaseAdapter;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.zlibrary.ui.android.dialogs.ZLAndroidDialogManager;

import org.geometerplus.fbreader.network.NetworkTree;
import org.geometerplus.fbreader.network.NetworkLibrary;


public class NetworkLibraryActivity extends NetworkBaseActivity {

	private NetworkTree myTree;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
	}

	private void prepareView() {
		if (myTree == null) {
			myTree = NetworkLibrary.Instance().getTree();
			setListAdapter(new LibraryAdapter());
			getListView().invalidateViews();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		initializeNetworkView(this, "loadingNetworkLibrary", new Runnable() {
			public void run() {
				prepareView();
			}
		});
	}

	static Initializator myInitializator;

	public static void initializeNetworkView(Activity activity, String key, final Runnable doAfter) {
		final NetworkView networkView = NetworkView.Instance();
		if (!networkView.isInitialized()) {
			if (myInitializator == null) {
				myInitializator = new Initializator(activity, key, doAfter);
				myInitializator.start();
			} else {
				myInitializator.setParameters(activity, doAfter);
			}
		} else if (doAfter != null) {
			doAfter.run();
		}
	}


	private static class Initializator extends Handler {

		private Activity myActivity;
		private String myKey;
		private Runnable myDoAfter;

		public Initializator(Activity activity, String key, Runnable doAfter) {
			myActivity = activity;
			myKey = key;
			myDoAfter = doAfter;
		}

		public void setParameters(Activity activity, Runnable doAfter) {
			myActivity = activity;
		}

		final DialogInterface.OnClickListener myListener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					Initializator.this.start();
				} else {
					myActivity.finish();
				}
			}
		};

		private void runInitialization() {
			((ZLAndroidDialogManager)ZLAndroidDialogManager.Instance()).wait(myKey, new Runnable() {
				public void run() {
					final String error = NetworkView.Instance().initialize();
					Initializator.this.end(error);
				}
			}, myActivity);
		}

		private void processResults(String error) {
			final ZLResource dialogResource = ZLResource.resource("dialog");
			final ZLResource boxResource = dialogResource.getResource("networkError");
			final ZLResource buttonResource = dialogResource.getResource("button");
			new AlertDialog.Builder(myActivity)
				.setTitle(boxResource.getResource("title").getValue())
				.setMessage(error)
				.setIcon(0)
				.setPositiveButton(buttonResource.getResource("tryAgain").getValue(), myListener)
				.setNegativeButton(buttonResource.getResource("cancel").getValue(), myListener)
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						myListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
					}
				})
				.create().show();
		}

		@Override
		public void handleMessage(Message message) {
			if (message.what == 0) {
				runInitialization(); // run initialization process
			} else if (message.obj == null) {
				myActivity.startService(new Intent(myActivity.getApplicationContext(), LibraryInitializationService.class));
				if (myDoAfter != null) {
					myDoAfter.run(); // initialization is complete successfully
				}
			} else {
				processResults((String) message.obj); // handle initialization error
			}
		}

		public void start() {
			sendEmptyMessage(0);
		}

		private void end(String error) {
			sendMessage(obtainMessage(1, error));
		}
	}


	private final class LibraryAdapter extends BaseAdapter {

		public final int getCount() {
			return myTree.subTrees().size()
				+ NetworkView.Instance().getSpecialItems().size() + 1;
		}

		public final NetworkTree getItem(int position) {
			List<NetworkTree> specialItems = NetworkView.Instance().getSpecialItems();
			int size = specialItems.size();
			if (position < size) {
				return specialItems.get(position);
			}
			position -= size;
			size = myTree.subTrees().size();
			if (position < size) {
				return (NetworkTree) myTree.subTrees().get(position);
			}
			position -= size;
			if (position == 0) {
				return NetworkView.Instance().getAddCustomCatalogItemTree();
			}
			return null;
		}

		public final long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, final ViewGroup parent) {
			final NetworkTree tree = getItem(position);
			return setupNetworkTreeItemView(convertView, parent, tree);
		}
	}


	protected MenuItem addMenuItem(Menu menu, int index, String resourceKey, int iconId) {
		final String label = myResource.getResource("menu").getResource(resourceKey).getValue();
		return menu.add(0, index, Menu.NONE, label).setIcon(iconId);
	}


	private static final int MENU_SEARCH = 1;
	private static final int MENU_REFRESH = 2;
	private static final int MENU_ADD_CATALOG = 3;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		addMenuItem(menu, MENU_SEARCH, "networkSearch", R.drawable.ic_menu_networksearch);
		addMenuItem(menu, MENU_ADD_CATALOG, "addCustomCatalog", android.R.drawable.ic_menu_add);
		addMenuItem(menu, MENU_REFRESH, "refreshCatalogsList", R.drawable.ic_menu_refresh);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		final boolean searchInProgress = NetworkView.Instance().containsItemsLoadingRunnable(NetworkSearchActivity.SEARCH_RUNNABLE_KEY);
		menu.findItem(MENU_SEARCH).setEnabled(!searchInProgress);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_SEARCH:
				return onSearchRequested();
			case MENU_ADD_CATALOG:
				AddCustomCatalogItemActions.addCustomCatalog(this);
				return true;
			case MENU_REFRESH:
				refreshCatalogsList();
				return true;
			default:
				return true;
		}
	}

	@Override
	public boolean onSearchRequested() {
		if (NetworkView.Instance().containsItemsLoadingRunnable(NetworkSearchActivity.SEARCH_RUNNABLE_KEY)) {
			return false;
		}
		final NetworkLibrary library = NetworkLibrary.Instance();
		startSearch(library.NetworkSearchPatternOption.getValue(), true, null, false);
		return true;
	}

	@Override
	public void onModelChanged() {
		getListView().invalidateViews();
	}

	private void refreshCatalogsList() {
		final NetworkView view = NetworkView.Instance();

		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.obj == null) {
					view.finishBackgroundUpdate();
				} else {
					final ZLResource dialogResource = ZLResource.resource("dialog");
					final ZLResource boxResource = dialogResource.getResource("networkError");
					final ZLResource buttonResource = dialogResource.getResource("button");
					new AlertDialog.Builder(NetworkLibraryActivity.this)
						.setTitle(boxResource.getResource("title").getValue())
						.setMessage((String) msg.obj)
						.setIcon(0)
						.setPositiveButton(buttonResource.getResource("ok").getValue(), null)
						.create().show();
				}
			}
		};

		((ZLAndroidDialogManager)ZLAndroidDialogManager.Instance()).wait("updatingCatalogsList", new Runnable() {
			public void run() {
				final String result = view.runBackgroundUpdate(true);
				handler.sendMessage(handler.obtainMessage(0, result));
			}
		}, this);
	}
}
