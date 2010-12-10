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

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.tree.FBTree;
import org.geometerplus.fbreader.library.Library;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.android.fbreader.SQLiteBooksDatabase;
import org.geometerplus.android.fbreader.tree.ZLAndroidTree;

public class LibraryTopLevelActivity extends LibraryBaseActivity {
	static Library Library;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		if (SQLiteBooksDatabase.Instance() == null) {
			new SQLiteBooksDatabase(this, "LIBRARY_NG");
		}
		if (Library == null) {
			Library = new Library();
		}

		final ArrayList<FBTree> items = new ArrayList<FBTree>();
		items.add(new TopLevelTree(
			myResource.getResource("searchResults"),
			R.drawable.ic_tab_library_results,
			new Runnable() {
				public void run() {
				}
			}
		));
		items.add(new TopLevelTree(
			myResource.getResource("favorites"),
			R.drawable.ic_tab_library_recent,
			new OpenTreeRunnable(LibraryTreeActivity.PATH_FAVORITES, mySelectedBookPath)
		));
		items.add(new TopLevelTree(
			myResource.getResource("recent"),
			R.drawable.ic_tab_library_recent,
			new OpenTreeRunnable(LibraryTreeActivity.PATH_RECENT, mySelectedBookPath)
		));
		items.add(new TopLevelTree(
			myResource.getResource("byAuthor"),
			R.drawable.library_by_author,
			new OpenTreeRunnable(LibraryTreeActivity.PATH_BY_AUTHOR, mySelectedBookPath)
		));
		items.add(new TopLevelTree(
			myResource.getResource("byTag"),
			R.drawable.library_by_tag,
			new OpenTreeRunnable(LibraryTreeActivity.PATH_BY_TAG, mySelectedBookPath)
		));
		items.add(new TopLevelTree(
			myResource.getResource("fileTree"),
			R.drawable.fbreader,
			new Runnable() {
				public void run() {
					runFileManager();
				}
			}
		));
		setListAdapter(new LibraryAdapter(items));
	}

	@Override
	public void onDestroy() {
		Library = null;
		super.onDestroy();
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long rowId) {
		TopLevelTree tree = (TopLevelTree)((LibraryAdapter)getListAdapter()).getItem(position);
		tree.getAction().run();
	}
	
	private void runFileManager(){
		Log.v(FileManager.FILE_MANAGER_LOG_TAG, "runFileManager()");
		Intent i = new Intent(this, FileManager.class).putExtra(FileManager.FILE_MANAGER_PATH, "./sdcard");
        startActivity(i);
	}
}

class TopLevelTree extends FBTree implements ZLAndroidTree {
	private final ZLResource myResource;
	private final int myCoverResourceId;
	private final Runnable myAction;

	public TopLevelTree(ZLResource resource, int coverResourceId, Runnable action) {
		myResource = resource;
		myCoverResourceId = coverResourceId;
		myAction = action;
	}

	@Override
	public String getName() {
		return myResource.getValue();
	}

	@Override
	public String getSummary() {
		return myResource.getResource("summary").getValue();
	}

	public int getCoverResourceId() {
		return myCoverResourceId;
	}

	public Runnable getAction() {
		return myAction;
	}
}
