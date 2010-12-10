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

package org.geometerplus.android.fbreader.library;

import java.util.LinkedList;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.Toast;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.tree.FBTree;
import org.geometerplus.fbreader.library.Library;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.android.fbreader.SQLiteBooksDatabase;
import org.geometerplus.android.fbreader.tree.ZLAndroidTree;

public class LibraryTopLevelActivity extends LibraryBaseActivity {
	private LinkedList<FBTree> myItems;
	private TopLevelTree mySearchResultsItem;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		if (SQLiteBooksDatabase.Instance() == null) {
			new SQLiteBooksDatabase(this, "LIBRARY_NG");
		}
		if (Library == null) {
			Library = new Library();
			startService(new Intent(getApplicationContext(), InitializationService.class));
		}

		myItems = new LinkedList<FBTree>();
		myItems.add(new TopLevelTree(
			myResource.getResource(PATH_FAVORITES),
			R.drawable.ic_list_library_favorites,
			new OpenTreeRunnable(PATH_FAVORITES, mySelectedBookPath)
		));
		myItems.add(new TopLevelTree(
			myResource.getResource(PATH_RECENT),
			R.drawable.ic_list_library_recent,
			new OpenTreeRunnable(PATH_RECENT, mySelectedBookPath)
		));
		myItems.add(new TopLevelTree(
			myResource.getResource(PATH_BY_AUTHOR),
			R.drawable.ic_list_library_authors,
			new OpenTreeRunnable(PATH_BY_AUTHOR, mySelectedBookPath)
		));
		myItems.add(new TopLevelTree(
			myResource.getResource(PATH_BY_TAG),
			R.drawable.ic_list_library_tags,
			new OpenTreeRunnable(PATH_BY_TAG, mySelectedBookPath)
		));
		myItems.add(new TopLevelTree(
			myResource.getResource("fileTree"),
			R.drawable.ic_list_library_folder,
			new Runnable() {
				public void run() {
				}
			}
		));
		setListAdapter(new LibraryAdapter(myItems));

		onNewIntent(getIntent());
	}

	@Override
	public void onDestroy() {
		Library = null;
		super.onDestroy();
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long rowId) {
		TopLevelTree tree = (TopLevelTree)((LibraryAdapter)getListAdapter()).getItem(position);
		tree.run();
	}

	private void setSearchResults(Intent intent) {
		if (myItems.get(0) == mySearchResultsItem) {
			myItems.remove(0);
		}
		final String pattern = intent.getStringExtra(SearchManager.QUERY);
		mySearchResultsItem = new TopLevelTree(
			myResource.getResource(PATH_SEARCH_RESULTS),
			pattern,
			R.drawable.ic_list_library_books,
			new OpenTreeRunnable(PATH_SEARCH_RESULTS, pattern, mySelectedBookPath)
		);
		myItems.add(0, mySearchResultsItem);
		getListView().invalidateViews();
		mySearchResultsItem.run();
	}

	public void onNewIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			if (runSearch(intent)) {
				setSearchResults(intent);
			} else {
				showNotFoundToast();
			}
		} else if (ACTION_FOUND.equals(intent.getAction())) {
			setSearchResults(intent);
		}
	}
}

class TopLevelTree extends FBTree implements ZLAndroidTree {
	private final ZLResource myResource;
	private final String myParameter;
	private final int myCoverResourceId;
	private final Runnable myAction;

	public TopLevelTree(ZLResource resource, String parameter, int coverResourceId, Runnable action) {
		myResource = resource;
		myParameter = parameter;
		myCoverResourceId = coverResourceId;
		myAction = action;
	}

	public TopLevelTree(ZLResource resource, int coverResourceId, Runnable action) {
		this(resource, null, coverResourceId, action);
	}

	@Override
	public String getName() {
		return myResource.getValue();
	}

	@Override
	public String getSummary() {
		final String summary = myResource.getResource("summary").getValue();
		return myParameter == null ? summary : summary.replace("%s", myParameter);
	}

	public int getCoverResourceId() {
		return myCoverResourceId;
	}

	public void run() {
		myAction.run();
	}
}
