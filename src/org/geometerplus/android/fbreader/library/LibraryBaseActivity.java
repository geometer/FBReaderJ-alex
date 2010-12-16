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

import java.util.List;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.image.ZLLoadableImage;
import org.geometerplus.zlibrary.core.options.ZLStringOption;

import org.geometerplus.fbreader.tree.FBTree;
import org.geometerplus.fbreader.library.*;
import org.geometerplus.android.fbreader.FBReader;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.android.util.UIUtil;
import org.geometerplus.android.fbreader.tree.ZLAndroidTree;

abstract class LibraryBaseActivity extends BaseActivity {
	static final String TREE_PATH_KEY = "TreePath";
	static final String PARAMETER_KEY = "Parameter";

	public static final String PATH_FAVORITES = "favorites";
	public static final String PATH_SEARCH_RESULTS = "searchResults";
	public static final String PATH_RECENT = "recent";
	public static final String PATH_BY_AUTHOR = "byAuthor";
	public static final String PATH_BY_TAG = "byTag";

	static final ZLStringOption BookSearchPatternOption =
		new ZLStringOption("BookSearch", "Pattern", "");

	private static int CHILD_LIST_REQUEST = 0;
	private static int RESULT_DONT_INVALIDATE_VIEWS = 0;
	private static int RESULT_DO_INVALIDATE_VIEWS = 1;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setResult(RESULT_DONT_INVALIDATE_VIEWS);
	}

	@Override
	protected void onActivityResult(int requestCode, int returnCode, Intent intent) {
		if (requestCode == CHILD_LIST_REQUEST && returnCode == RESULT_DO_INVALIDATE_VIEWS) {
			getListView().invalidateViews();
			setResult(RESULT_DO_INVALIDATE_VIEWS);
		}
	} 

	@Override
	public boolean onSearchRequested() {
		startSearch(BookSearchPatternOption.getValue(), true, null, false);
		return true;
	}

	protected static final String ACTION_FOUND = "fbreader.library.intent.FOUND";

	protected boolean runSearch(Intent intent) {
	   	final String pattern = intent.getStringExtra(SearchManager.QUERY);
		if (pattern == null || pattern.length() == 0) {
			return false;
		}
		BookSearchPatternOption.setValue(pattern);
		return LibraryInstance.searchBooks(pattern).hasChildren();
	}

	protected void showNotFoundToast() {
		Toast.makeText(
			this,
			ZLResource.resource("errorMessage").getResource("bookNotFound").getValue(),
			Toast.LENGTH_SHORT
		).show();
	}

	protected final class LibraryAdapter extends BaseAdapter implements View.OnCreateContextMenuListener {
		private final List<FBTree> myItems;

		public LibraryAdapter(List<FBTree> items) {
			myItems = items;
		}

		public final int getCount() {
			return myItems.size();
		}

		public final FBTree getItem(int position) {
			return myItems.get(position);
		}

		public final long getItemId(int position) {
			return position;
		}

		public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
			final int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
			final LibraryTree tree = (LibraryTree)getItem(position);
			if (tree instanceof BookTree) {
				createBookContextMenu(menu, ((BookTree)tree).Book);
			}
		}

		private int myCoverWidth = -1;
		private int myCoverHeight = -1;

		public View getView(int position, View convertView, final ViewGroup parent) {
			final FBTree tree = getItem(position);
			final View view = (convertView != null) ?  convertView :
				LayoutInflater.from(parent.getContext()).inflate(R.layout.library_tree_item, parent, false);

			if (tree instanceof BookTree &&
				mySelectedBookPath != null &&
				mySelectedBookPath.equals(((BookTree)tree).Book.File.getPath())) {
				view.setBackgroundColor(0xff808080);
			} else {
				view.setBackgroundColor(0);
			}

			((TextView)view.findViewById(R.id.library_tree_item_name)).setText(tree.getName());
			((TextView)view.findViewById(R.id.library_tree_item_childrenlist)).setText(tree.getSecondString());

			if (myCoverWidth == -1) {
				view.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				myCoverHeight = view.getMeasuredHeight();
				myCoverWidth = myCoverHeight * 15 / 32;
				view.requestLayout();
			}

			final ImageView coverView = (ImageView)view.findViewById(R.id.library_tree_item_icon);
			coverView.getLayoutParams().width = myCoverWidth;
			coverView.getLayoutParams().height = myCoverHeight;
			coverView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			coverView.requestLayout();

			if (tree instanceof ZLAndroidTree) {
				coverView.setImageResource(((ZLAndroidTree)tree).getCoverResourceId());
			} else {
				final Bitmap coverBitmap = getCoverBitmap(tree.getCover(), myCoverWidth, myCoverHeight);
				if (coverBitmap != null) {
					coverView.setImageBitmap(coverBitmap);
				} else if (tree instanceof AuthorTree) {
					coverView.setImageResource(R.drawable.ic_list_library_author);
				} else if (tree instanceof TagTree) {
					coverView.setImageResource(R.drawable.ic_list_library_tag);
				} else if (tree instanceof BookTree) {
					coverView.setImageResource(R.drawable.ic_list_library_book);
				} else {
					coverView.setImageResource(R.drawable.ic_list_library_books);
				}
			}
                
			return view;
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		final FBTree tree = ((LibraryAdapter)getListAdapter()).getItem(position);
		if (tree instanceof BookTree) {
			final Book book = ((BookTree)tree).Book;
			switch (item.getItemId()) {
				case OPEN_BOOK_ITEM_ID:
					openBook(book);
					return true;
				case ADD_TO_FAVORITES_ITEM_ID:
					LibraryInstance.addBookToFavorites(book);
					return true;
				case REMOVE_FROM_FAVORITES_ITEM_ID:
					LibraryInstance.removeBookFromFavorites(book);
					getListView().invalidateViews();
					return true;
				case DELETE_BOOK_ITEM_ID:
					tryToDeleteBook(book);
					return true;
			}
		}
		return super.onContextItemSelected(item);
	}

	private class BookDeleter implements DialogInterface.OnClickListener {
		private final Book myBook;
		private final int myMode;

		BookDeleter(Book book, int removeMode) {
			myBook = book;
			myMode = removeMode;
		}

		public void onClick(DialogInterface dialog, int which) {
			LibraryInstance.removeBook(myBook, myMode);
			getListView().invalidateViews();
			setResult(RESULT_DO_INVALIDATE_VIEWS);
		}
	}

	private void tryToDeleteBook(Book book) {
		final ZLResource dialogResource = ZLResource.resource("dialog");
		final ZLResource buttonResource = dialogResource.getResource("button");
		final ZLResource boxResource = dialogResource.getResource("deleteBookBox");
		new AlertDialog.Builder(this)
			.setTitle(book.getTitle())
			.setMessage(boxResource.getResource("message").getValue())
			.setIcon(0)
			.setPositiveButton(buttonResource.getResource("yes").getValue(), new BookDeleter(book, Library.REMOVE_FROM_DISK))
			.setNegativeButton(buttonResource.getResource("no").getValue(), null)
			.create().show();
	}

	protected class OpenTreeRunnable implements Runnable {
		private final String myTreePath;
		private final String myParameter;

		public OpenTreeRunnable(String treePath) {
			this(treePath, null);
		}

		public OpenTreeRunnable(String treePath, String parameter) {
			myTreePath = treePath;
			myParameter = parameter;
		}

		public void run() {
			final Runnable postRunnable = new Runnable() {
				public void run() {
					startActivityForResult(
						new Intent(LibraryBaseActivity.this, LibraryTreeActivity.class)
							.putExtra(SELECTED_BOOK_PATH_KEY, mySelectedBookPath)
							.putExtra(TREE_PATH_KEY, myTreePath)
							.putExtra(PARAMETER_KEY, myParameter),
						CHILD_LIST_REQUEST
					);
				}
			};
			if (LibraryInstance.hasState(Library.STATE_FULLY_INITIALIZED)) {
				postRunnable.run();
			} else {
				UIUtil.runWithMessage(LibraryBaseActivity.this, "loadingBookList",
				new Runnable() {
					public void run() {
						LibraryInstance.waitForState(Library.STATE_FULLY_INITIALIZED);
					}
				},
				postRunnable);
			}
		}
	}
}
