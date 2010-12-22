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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.image.ZLLoadableImage;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.text.view.ZLTextView;

import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.library.Author;
import org.geometerplus.fbreader.library.Book;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;


public class NavigationDialog extends Dialog {

	public NavigationDialog(Context context, final EPDView epd) {
		super(context, android.R.style.Theme_Translucent_NoTitleBar);
		setContentView(R.layout.navigation_view);

		final EPDView.EventsListener listener = new EPDView.EventsListener() {
			public void onPageScrolling() {
			}
			public void onEpdRepaintFinished() {
				updateView();
			}
		};
		epd.addEventsListener(listener);

		//final TextView statusPositionText = (TextView) findViewById(R.id.statusbar_position_text);
		final TextView bookPositionText = (TextView) findViewById(R.id.book_position_text);
		final SeekBar bookPositionSlider = (SeekBar) findViewById(R.id.book_position_slider);
		bookPositionText.setText("");
		//statusPositionText.setText("");
		bookPositionSlider.setProgress(0);
		bookPositionSlider.setMax(1);
		bookPositionSlider.setVisibility(View.INVISIBLE);

		bookPositionSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			private boolean myInTouch;

			private void gotoPage(int page) {
				final ZLView view = ZLApplication.Instance().getCurrentView();
				if (view instanceof ZLTextView) {
					ZLTextView textView = (ZLTextView) view;
					if (page == 1) {
						textView.gotoHome();
					} else {
						textView.gotoPage(page);
					}
				}
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				gotoPage(seekBar.getProgress() + 1);
				epd.updateEpdDisplay();
				myInTouch = false;
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				myInTouch = true;
			}

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					final int page = progress + 1;
					final int pagesNumber = seekBar.getMax() + 1;
					bookPositionText.setText(EPDView.makePositionText(page, pagesNumber));
					if (!myInTouch) {
						gotoPage(page);
						epd.updateEpdDisplay();
					}
				}
			}
		});

		setOnDismissListener(new OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				epd.removeEventsListener(listener);
			}
		});

		final TextView bookNoCover = (TextView) findViewById(R.id.book_no_cover_text);
		bookNoCover.setText(ZLResource.resource("fbreader").getResource("noCover").getValue());
	}

	@Override
	protected void onStart() {
		super.onStart();
		myViewBook = null;
		FBReaderApp.Instance().repaintView();
	}


	private int myCoverWidth;
	private int myCoverHeight;
	private Book myViewBook;

	public void updateView() {
		final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();

		final TextView bookTitle = (TextView) findViewById(R.id.book_title);
		final TextView bookAuthors = (TextView) findViewById(R.id.book_authors);
		final ImageView bookCover = (ImageView) findViewById(R.id.book_cover);
		final TextView bookNoCoverText = (TextView) findViewById(R.id.book_no_cover_text);
		final RelativeLayout bookNoCoverLayout = (RelativeLayout) findViewById(R.id.book_no_cover_layout);

		if (myCoverWidth == 0) {
			myCoverWidth = bookCover.getWidth();
			myCoverHeight = bookCover.getHeight();
			final int viewHeight = myCoverWidth * 4 / 3;
			if (myCoverHeight > viewHeight) {
				final int margin = (myCoverHeight - viewHeight) / 2;
				ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bookNoCoverLayout.getLayoutParams();
				params.topMargin = params.bottomMargin = margin;
				bookNoCoverLayout.invalidate();
				bookNoCoverLayout.requestLayout();
			}
		}

		bookCover.setAnimation(null);
		bookCover.setPadding(0, 0, 0, 0);
		bookNoCoverText.setAnimation(null);

		if (fbreader.Model != null && fbreader.Model.Book != null) {
			if (fbreader.Model.Book != myViewBook) {
				myViewBook = fbreader.Model.Book;

				final ZLFile bookFile = ((FBReaderApp)FBReaderApp.Instance()).Model.Book.File;
				final Book book = Book.getByFile(bookFile);

				bookTitle.setText(book.getTitle());
				int count = 0;
				final StringBuilder authors = new StringBuilder();
				for (Author a: book.authors()) {
					if (count++ > 0) {
						authors.append(",  ");
					}
					authors.append(a.DisplayName);
					if (count == 5) {
						break;
					}
				}
				bookAuthors.setText(authors.toString());

				bookCover.setImageDrawable(null);
				bookCover.setVisibility(View.GONE);
				bookNoCoverLayout.setVisibility(View.VISIBLE);

				final FormatPlugin plugin = PluginCollection.Instance().getPlugin(myViewBook.File);
				if (plugin != null) {
					final ZLImage image = plugin.readCover(myViewBook);
					if (image != null) {
						final ZLAndroidImageManager mgr = (ZLAndroidImageManager) ZLAndroidImageManager.Instance();
						final Runnable refreshRunnable = new Runnable() {
							public void run() {
								ZLAndroidImageData data = mgr.getImageData(image);
								if (data != null) {
									final Bitmap coverBitmap = data.getBitmap(2 * myCoverWidth, 2 * myCoverHeight);
									if (coverBitmap != null) {
										bookCover.setImageBitmap(coverBitmap);
										bookCover.setVisibility(View.VISIBLE);
										bookNoCoverLayout.setVisibility(View.GONE);
									}
								}
							}
						};
						if (image instanceof ZLLoadableImage) {
							ZLLoadableImage loadable = (ZLLoadableImage)image;
							if (loadable.isSynchronized()) {
								refreshRunnable.run();
							} else {
								loadable.startSynchronization(refreshRunnable);
							}
						} else {
							refreshRunnable.run();
						}
					}
				}
			}
			final int angle = ZLAndroidApplication.Instance().RotationFlag;
			if (angle != ZLAndroidApplication.ROTATE_0) {
				final RotateAnimation anim = new RotateAnimation(angle, angle, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
				anim.setFillEnabled(true);
				anim.setFillAfter(true);
				final View coverView = bookCover.getVisibility() == View.VISIBLE ? bookCover : bookNoCoverText;
				coverView.startAnimation(anim);
				if (coverView == bookCover && angle != ZLAndroidApplication.ROTATE_180) {
					final int padding = (bookCover.getHeight() - bookCover.getWidth()) / 2;
					bookCover.setPadding(0, padding, 0, padding);
				}
			}
		} else {
			myViewBook = null;
			bookTitle.setText("");
			bookAuthors.setText("");
			bookCover.setImageDrawable(null);
			bookCover.setVisibility(View.VISIBLE);
			bookNoCoverLayout.setVisibility(View.GONE);
		}

		findViewById(R.id.navigation_view).invalidate();

		final TextView bookPositionText = (TextView) findViewById(R.id.book_position_text);
		final SeekBar bookPositionSlider = (SeekBar) findViewById(R.id.book_position_slider);

		final ZLView view = fbreader.getCurrentView();
		if (view instanceof ZLTextView
				&& ((ZLTextView) view).getModel() != null
				&& ((ZLTextView) view).getModel().getParagraphsNumber() != 0) {
			ZLTextView textView = (ZLTextView) view;

			final int page = textView.computeCurrentPage();
			final int pagesNumber = textView.computePageNumber();

			bookPositionText.setText(EPDView.makePositionText(page, pagesNumber));
			bookPositionSlider.setVisibility(View.VISIBLE);
			bookPositionSlider.setMax(pagesNumber - 1);
			bookPositionSlider.setProgress(page - 1);
		} else {
			bookPositionText.setText("");
			bookPositionSlider.setProgress(0);
			bookPositionSlider.setMax(1);
			bookPositionSlider.setVisibility(View.INVISIBLE);
		}
	}
}
