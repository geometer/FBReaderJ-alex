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

package org.geometerplus.android.fbreader;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.*;

import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.image.ZLLoadableImage;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.library.*;

import org.geometerplus.android.fbreader.preferences.EditBookInfoActivity;

public class BookInfoActivity extends Activity {
	public static final String CURRENT_BOOK_PATH_KEY = "CurrentBookPath";
	public static final String HIDE_OPEN_BUTTON_KEY = "hideOpenButton";

	private final ZLResource myResource = ZLResource.resource("bookInfo");
	private Book myBook;
	private ZLImage myImage;
	private boolean myHideOpenButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(
			new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this)
		);

		final String path = getIntent().getStringExtra(CURRENT_BOOK_PATH_KEY);
		myHideOpenButton = getIntent().getBooleanExtra(HIDE_OPEN_BUTTON_KEY, false);
		final ZLFile file = ZLFile.createFileByPath(path);

		myImage = Library.getCover(file);

		if (SQLiteBooksDatabase.Instance() == null) {
			new SQLiteBooksDatabase(this, "LIBRARY");
		}

		myBook = Book.getByFile(file);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.book_info);
	}

	@Override
	protected void onStart() {
		super.onStart();

		setupCover(myBook);
		setupBookInfo(myBook);
		setupFileInfo(myBook);

		if (myHideOpenButton) {
			findButton(R.id.book_info_button_open).setVisibility(View.GONE);
		} else {
			setupButton(R.id.book_info_button_open, "open", new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					startActivity(
						new Intent(getApplicationContext(), FBReader.class)
							.setAction(Intent.ACTION_VIEW)
							.putExtra(FBReader.BOOK_PATH_KEY, myBook.File.getPath())
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
					);
				}
			});
		}
		setupButton(R.id.book_info_button_edit, "edit", new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(
					new Intent(getApplicationContext(), EditBookInfoActivity.class)
						.putExtra(CURRENT_BOOK_PATH_KEY, myBook.File.getPath())
				);
			}
		});
		setupButton(R.id.book_info_button_reload, "reload", new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO: implement
			}
		});

		final View root = findViewById(R.id.book_info_root);
		root.invalidate();
		root.requestLayout();
	}

	private Button findButton(int buttonId) {
		return (Button)findViewById(buttonId);
	}

	private void setupButton(int buttonId, String resourceKey, View.OnClickListener listener) {
		final ZLResource buttonResource = ZLResource.resource("dialog").getResource("button");
		final Button button = findButton(buttonId);
		button.setText(buttonResource.getResource(resourceKey).getValue());
		button.setOnClickListener(listener);
	}

	private void setupInfoPair(int id, String key, CharSequence value) {
		LinearLayout layout = (LinearLayout)findViewById(id);
		if (value == null || value.length() == 0) {
			layout.setVisibility(View.GONE);
			return;
		}
		layout.setVisibility(View.VISIBLE);
		((TextView)layout.findViewById(R.id.book_info_key)).setText(myResource.getResource(key).getValue());
		((TextView)layout.findViewById(R.id.book_info_value)).setText(value);
	}

	private void setupCover(Book book) {
		final ImageView coverView = (ImageView)findViewById(R.id.book_cover);

		final int maxHeight = 250; // FIXME: hardcoded constant
		final int maxWidth = maxHeight * 3 / 4;

		coverView.setVisibility(View.GONE);
		coverView.setImageDrawable(null);

		if (myImage == null) {
			return;
		}

		if (myImage instanceof ZLLoadableImage) {
			final ZLLoadableImage loadableImage = (ZLLoadableImage)myImage;
			if (!loadableImage.isSynchronized()) {
				loadableImage.synchronize();
			}
		}
		final ZLAndroidImageData data =
			((ZLAndroidImageManager)ZLAndroidImageManager.Instance()).getImageData(myImage);
		if (data == null) {
			return;
		}

		final Bitmap coverBitmap = data.getBitmap(2 * maxWidth, 2 * maxHeight);
		if (coverBitmap == null) {
			return;
		}

		coverView.setVisibility(View.VISIBLE);
		coverView.getLayoutParams().width = maxWidth;
		coverView.getLayoutParams().height = maxHeight;
		coverView.setImageBitmap(coverBitmap);
	}

	private void setupBookInfo(Book book) {
		((TextView)findViewById(R.id.book_info_title)).setText(myResource.getResource("bookInfo").getValue());

		setupInfoPair(R.id.book_title, "title", book.getTitle());

		final StringBuilder buffer = new StringBuilder();
		for (Author author: book.authors()) {
			if (buffer.length() > 0) {
				buffer.append(", ");
			}
			buffer.append(author.DisplayName);
		}
		setupInfoPair(R.id.book_authors, "authors", buffer);

		SeriesInfo series = book.getSeriesInfo();
		setupInfoPair(R.id.book_series, "series",
				(series == null) ? null : series.Name);
		setupInfoPair(R.id.book_series_index, "indexInSeries",
				(series == null || series.Index <= 0) ? null : String.valueOf(series.Index));

		buffer.delete(0, buffer.length());
		final HashSet<String> tagNames = new HashSet<String>();
		for (Tag tag : book.tags()) {
			if (!tagNames.contains(tag.Name)) {
				if (buffer.length() > 0) {
					buffer.append(", ");
				}
				buffer.append(tag.Name);
				tagNames.add(tag.Name);
			}
		}
		setupInfoPair(R.id.book_tags, "tags", buffer);
	}

	private void setupFileInfo(Book book) {
		((TextView)findViewById(R.id.file_info_title)).setText(myResource.getResource("fileInfo").getValue());

		setupInfoPair(R.id.file_name, "name", book.File.getPath());
		setupInfoPair(R.id.file_type, "type", book.File.getExtension());

		final ZLFile physFile = book.File.getPhysicalFile();
		final File file = physFile == null ? null : new File(physFile.getPath());
		if (file != null && file.exists() && file.isFile()) {
			setupInfoPair(R.id.file_size, "size", formatSize(file.length()));
			setupInfoPair(R.id.file_time, "time", formatDate(file.lastModified()));
		} else {
			setupInfoPair(R.id.file_size, "size", null);
			setupInfoPair(R.id.file_time, "time", null);
		}
	}

	private String formatSize(long size) {
		if (size <= 0) {
			return null;
		}
		final int kilo = 1024;
		if (size < kilo) { // less than 1 kilobyte
			return myResource.getResource("sizeInBytes").getValue().replaceAll("%s", String.valueOf(size));
		}
		final String value;
		if (size < kilo * kilo) { // less than 1 megabyte
			value = String.format("%.2f", ((float)size) / kilo);
		} else {
			value = String.valueOf(size / kilo);
		}
		return myResource.getResource("sizeInKiloBytes").getValue().replaceAll("%s", value);
	}

	private String formatDate(long date) {
		if (date == 0) {
			return null;
		}
		return DateFormat.getDateTimeInstance().format(new Date(date));
	}
}
