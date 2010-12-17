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

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.Library;

import org.geometerplus.android.fbreader.FBReader;


public final class FileManager extends BaseActivity {
	public static String FILE_MANAGER_PATH = "FileManagerPath";
	private final ZLResource myResource = ZLResource.resource("fileManagerView");

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FManagerAdapter adapter = new FManagerAdapter(this);
		setListAdapter(adapter);
		
		final Bundle extras = getIntent().getExtras();
		final String path = extras != null ? extras.getString(FILE_MANAGER_PATH) : null;

		if (path == null) {
			setTitle(ZLResource.resource("libraryView").getResource("fileTree").getValue());
			addItem(Paths.BooksDirectoryOption().getValue(), "books");
			addItem("/", "root");
			addItem(Environment.getExternalStorageDirectory().getPath(), "sdcard");
		} else {
			setTitle(path);
			final SmartFilter filter = new SmartFilter(this, adapter, ZLFile.createFileByPath(path));
			new Thread(filter).start();
		}
			
		getListView().setOnCreateContextMenuListener(adapter);
		getListView().setTextFilterEnabled(true);
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				runItem(((FManagerAdapter)getListAdapter()).getItem(position));
			}
		});
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		FManagerAdapter adapter = (FManagerAdapter)getListAdapter();
		FileItem fileItem = adapter.getItem(position);
		if (fileItem.getBook() != null) {
			Book book = fileItem.getBook(); 
			switch (item.getItemId()) {
				case OPEN_BOOK_ITEM_ID:
					openBook(fileItem.getBook().File.getPath());
					return true;
				case ADD_TO_FAVORITES_ITEM_ID:
					LibraryInstance.addBookToFavorites(book);
					return true;
				case REMOVE_FROM_FAVORITES_ITEM_ID:
					LibraryInstance.removeBookFromFavorites(book);
					getListView().invalidateViews();
					return true;
				case DELETE_BOOK_ITEM_ID:
					// TODO: implemen
					// TODO: if book is in favorites list do ((FManagerAdapter)getListAdapter())
					adapter.remove(fileItem);
					adapter.notifyDataSetChanged();
					fileItem.getBook().File.getPhysicalFile().delete();
					return true;
			}
		}
		return super.onContextItemSelected(item);
	}
	
	private void runItem(FileItem item) {
		final ZLFile file = item.getFile();
		if (file.isArchive() && file.getPath().endsWith("epub")){
			openBook(file.getPath());
		}else if (file.isDirectory() || file.isArchive()) {
			Intent i = new Intent(this, FileManager.class);
			i.putExtra(FILE_MANAGER_PATH, file.getPath());
			startActivity(i);
		} else {
			openBook(file.getPath());
		}
	}

	private void addItem(String path, String resourceKey) {
		final ZLResource resource = myResource.getResource(resourceKey);
		((FManagerAdapter)getListAdapter()).add(new FileItem(
			ZLFile.createFileByPath(path),
			resource.getValue(),
			resource.getResource("summary").getValue()
		));
	}
}
