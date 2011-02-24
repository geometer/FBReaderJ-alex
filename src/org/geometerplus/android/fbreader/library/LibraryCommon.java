package org.geometerplus.android.fbreader.library;

import org.geometerplus.android.fbreader.BookInfoActivity;
import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.BooksDatabase;
import org.geometerplus.fbreader.library.Library;
import org.geometerplus.fbreader.tree.FBTree;
import org.geometerplus.zlibrary.core.options.ZLIntegerOption;
import org.geometerplus.zlibrary.core.options.ZLStringOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;

class LibraryCommon {
	static BooksDatabase DatabaseInstance;
	static ViewType ViewTypeInstance;
	static SortType SortTypeInstance; 						// for FileManager
	static final ZLStringOption BookSearchPatternOption = 
		new ZLStringOption("BookSearch", "Pattern", "");	// for LibraryBaseActivity
	
	static Library LibraryInstance;
	private static int LibCount = 0;
	public static void incLibCount(){
		LibCount++;
	}
	public static void DestroyLibInstance(){
		LibCount--;
		if (LibCount < 1){
			LibraryInstance = null;
		}
	}
}

class FMCommon {
	static String LOG = "FileManager";
	static String InsertPath;
}

interface HasBaseConstants {
	public static final String SELECTED_BOOK_PATH_KEY = "SelectedBookPath";
	static final int OPEN_BOOK_ITEM_ID = 0;
	static final int SHOW_BOOK_INFO_ITEM_ID = 1;
	static final int ADD_TO_FAVORITES_ITEM_ID = 2;
	static final int REMOVE_FROM_FAVORITES_ITEM_ID = 3;
	static final int DELETE_BOOK_ITEM_ID = 4;

	static final int CHILD_LIST_REQUEST = 0;
	static final int BOOK_INFO_REQUEST = 1;
	static final int RESULT_DONT_INVALIDATE_VIEWS = 0;
	static final int RESULT_DO_INVALIDATE_VIEWS = 1;	
}

interface HasLibraryConstants {
	static final String TREE_PATH_KEY = "TreePath";
	static final String PARAMETER_KEY = "Parameter";

	static final String PATH_FAVORITES = "favorites";
	static final String PATH_SEARCH_RESULTS = "searchResults";
	static final String PATH_RECENT = "recent";
	static final String PATH_BY_AUTHOR = "byAuthor";
	static final String PATH_BY_TITLE = "byTitle";
	static final String PATH_BY_TAG = "byTag";
}

interface HasFileManagerConstants {
	static String FILE_MANAGER_PATH = "FileManagerPath";
	static final int DELETE_FILE_ITEM_ID = 10;
//	static final int RENAME_FILE_ITEM_ID = 11; //TODO may be later
	static final int MOVE_FILE_ITEM_ID = 12;
}

class LibraryUtil {
	
    public static MenuItem addMenuItem(Menu menu, int index, ZLResource resource, String resourceKey, int iconId) {
        final String label = resource.getResource("menu").getResource(resourceKey).getValue();
        final MenuItem item = menu.add(0, index, Menu.NONE, label);
        item.setIcon(iconId);
        return item;
    }
	
	public static void openBook(Activity activity, Book book) {
		activity.startActivity(new Intent(activity.getApplicationContext(), FBReader.class)
			.setAction(Intent.ACTION_VIEW)
			.putExtra(FBReader.BOOK_PATH_KEY, book.File.getPath())
			.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}
	
	public static void showBookInfo(Activity activity, Book book) {
		activity.startActivityForResult(
			new Intent(activity.getApplicationContext(), BookInfoActivity.class)
				.putExtra(BookInfoActivity.CURRENT_BOOK_PATH_KEY, book.File.getPath()),
			HasBaseConstants.BOOK_INFO_REQUEST
		);
	}
	
	public static void createBookContextMenu(ContextMenu menu, Book book, ZLResource resource) {
		menu.setHeaderTitle(book.getTitle());
		menu.add(0, HasBaseConstants.OPEN_BOOK_ITEM_ID, 0, resource.getResource("openBook").getValue());
		menu.add(0, HasBaseConstants.SHOW_BOOK_INFO_ITEM_ID, 0, resource.getResource("showBookInfo").getValue());
		if (LibraryCommon.LibraryInstance.isBookInFavorites(book)) {
			menu.add(0, HasBaseConstants.REMOVE_FROM_FAVORITES_ITEM_ID, 0, resource.getResource("removeFromFavorites").getValue());
		} else {
			menu.add(0, HasBaseConstants.ADD_TO_FAVORITES_ITEM_ID, 0, resource.getResource("addToFavorites").getValue());
		}
		if ((LibraryCommon.LibraryInstance.getRemoveBookMode(book) & Library.REMOVE_FROM_DISK) != 0) {
			menu.add(0, HasBaseConstants.DELETE_BOOK_ITEM_ID, 0, resource.getResource("deleteBook").getValue());
        }
	}
	
	public static void tryToDeleteBook(Activity activity, Book book, DialogInterface.OnClickListener bookDeleter) {
		final ZLResource dialogResource = ZLResource.resource("dialog");
		final ZLResource buttonResource = dialogResource.getResource("button");
		final ZLResource boxResource = dialogResource.getResource("deleteBookBox");
		new AlertDialog.Builder(activity)
			.setTitle(book.getTitle())
			.setMessage(boxResource.getResource("message").getValue())
			.setIcon(0)
			.setPositiveButton(buttonResource.getResource("yes").getValue(), bookDeleter)
			.setNegativeButton(buttonResource.getResource("no").getValue(), null)
			.create().show();
	}
	
	
	// from LibraryBaseActivity and GalleryLibraryBaseActivity
	// TODO
//	public static boolean isTreeSelected(FBTree tree, Book selectedBook) {
//		if (selectedBook == null) {
//			return false;
//		}
//
//		if (tree instanceof BookTree) {
//			return selectedBook.equals(((BookTree)tree).Book);
//		}
//		if (tree instanceof AuthorTree) {
//			return selectedBook.authors().contains(((AuthorTree)tree).Author);
//		}
//		if (tree instanceof TitleTree) {
//			final String title = selectedBook.getTitle();
//			return tree != null && title.trim().startsWith(((TitleTree)tree).Title);
//		}
//		if (tree instanceof SeriesTree) {
//			final SeriesInfo info = selectedBook.getSeriesInfo();
//			final String series = ((SeriesTree)tree).Series;
//			return info != null && series != null && series.equals(info.Name);
//		}
//		if (tree instanceof TagTree) {
//			final Tag tag = ((TagTree)tree).Tag;
//			for (Tag t : selectedBook.tags()) {
//				for (; t != null; t = t.Parent) {
//					if (t == tag) {
//						return true;
//					}
//				}
//			}
//			return false;
//		}
//		return false;
//	}

	// from LibraryTreeActivity and GalleryLibraryTreeActivity
	public static FBTree getTree(final String[] path) {
		FBTree tree = null;
		if (HasLibraryConstants.PATH_RECENT.equals(path[0])) {
			tree = LibraryCommon.LibraryInstance.recentBooks();
		} else if (HasLibraryConstants.PATH_SEARCH_RESULTS.equals(path[0])) {
			tree = LibraryCommon.LibraryInstance.searchResults();
		} else if (HasLibraryConstants.PATH_BY_AUTHOR.equals(path[0])) {
			tree = LibraryCommon.LibraryInstance.byAuthor();
			// TODO
//		} else if (HasLibraryConstants.PATH_BY_TITLE.equals(path[0])) {
//			tree = LibraryCommon.LibraryInstance.byTitle();
		} else if (HasLibraryConstants.PATH_BY_TAG.equals(path[0])) {
			tree = LibraryCommon.LibraryInstance.byTag();
		} else if (HasLibraryConstants.PATH_FAVORITES.equals(path[0])) {
			tree = LibraryCommon.LibraryInstance.favorites();
		}
	    
		for (int i = 1; i < path.length; ++i) {
			if (tree == null) {
				break;
			}
			tree = tree.getSubTreeByName(path[i]);
		}
		return tree;
	}
	

}

abstract class AStartTreeActivityRunnable implements Runnable {
	protected final String myTreePath;
	protected final String myParameter;
	public AStartTreeActivityRunnable(String treePath, String parameter) {
		myTreePath = treePath;
		myParameter = parameter;
	}
}

abstract class AOpenTreeRunnable implements Runnable {
	private final Library myLibrary;
	private final Runnable myPostRunnable;
	private final Activity myActivity;
	
	public AOpenTreeRunnable(Library library, Runnable postRunnable, Activity activity) {
		myLibrary = library;
		myPostRunnable = postRunnable;
		myActivity = activity;
	}

	public void run() {
		if (myLibrary.hasState(Library.STATE_FULLY_INITIALIZED)) {
			myPostRunnable.run();
		} else {
			UIUtil.runWithMessage(myActivity, "loadingBookList",
			new Runnable() {
				public void run() {
					myLibrary.waitForState(Library.STATE_FULLY_INITIALIZED);
				}
			},
			myPostRunnable);
		}
	}
}



class SortTypeConf{
	private static String SORT_GROUP = "sortGroup";
	private static String SORT_OPTION_NAME = "sortOptionName";
	private static int SORT_DEF_VALUE = 0;
	private static ZLIntegerOption mySortOption = new ZLIntegerOption(SORT_GROUP, SORT_OPTION_NAME, SORT_DEF_VALUE);

	public static SortType getSortType(){
		return SortType.values()[mySortOption.getValue()];
	}

	public static int getValue(){
		return mySortOption.getValue();
	}

	public static void setValue(int value){
		mySortOption.setValue(value);
	}
}

enum SortType{
	BY_NAME{
		public String getName() {
			return myResource.getResource("byName").getValue();
		}
	},
	BY_DATE{
		public String getName() {
			return myResource.getResource("byDate").getValue();
		}
	};

	private static ZLResource myResource = ZLResource.resource("libraryView").getResource("sortingBox");
	
	public abstract String getName();
	
	public static String[] toStringArray(){
		SortType[] sourse = values();
		String[] result = new String[sourse.length];
		for (int i = 0; i < sourse.length; i++){
			result[i] = sourse[i].getName();
		}
		return result;
	}
}



class ViewTypeConf{
	private static String VIEW_GROUP = "viewGroup";
	private static String VIEW_OPTION_NAME = "viewOptionName";
	private static int VIEW_DEF_VALUE = 0;
	private static ZLIntegerOption myViewOption = new ZLIntegerOption(VIEW_GROUP, VIEW_OPTION_NAME, VIEW_DEF_VALUE);
	
	public static ViewType getViewType(){
		return ViewType.values()[myViewOption.getValue()];
	}

	public static int getValue(){
		return myViewOption.getValue();
	}

	public static void setValue(int value){
		myViewOption.setValue(value);
	}
}

enum ViewType{

	SIMPLE{
		public String getName() {
			return myResource.getResource("simple").getValue();
		}
	},
	SKETCH{
		public String getName() {
			return myResource.getResource("sketch").getValue();
		}
	};
	
	private static ZLResource myResource = ZLResource.resource("libraryView").getResource("viewBox");
	
	public abstract String getName();

	public static String[] toStringArray(){
		ViewType[] sourse = values();
		String[] result = new String[sourse.length];
		for (int i = 0; i < sourse.length; i++){
			result[i] = sourse[i].getName();
		}
		return result;
	}
}