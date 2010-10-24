/*
 * Copyright (C) 2009-2010 Geometer Plus <contact@geometerplus.com>
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

import android.app.SearchManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.*;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidActivity;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.library.Author;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.Library;

public final class FBReader extends ZLAndroidActivity {
	static FBReader Instance;

	//private int myFullScreenFlag;
	private boolean myReadMode;
	private Book myViewBook;

	public final ZLResource Resource = ZLResource.resource("fbreader"); 

	boolean isReadMode() {
		return myReadMode;
	}

	private static class TextSearchButtonPanel implements ZLApplication.ButtonPanel {
		boolean Visible;
		ControlPanel ControlPanel;

		public void hide() {
			Visible = false;
			if (ControlPanel != null) {
				ControlPanel.hide(false);
			}
		}

		public void updateStates() {
			if (ControlPanel != null) {
				ControlPanel.updateStates();
			}
		}
	}
	private static TextSearchButtonPanel myPanel;

	@Override
	protected String fileNameForEmptyUri() {
		return Library.getHelpFile().getPath();
	}

	@Override
	public void onCreate(Bundle icicle) {
		EPDView.Instance().setVdsActive(true);
		super.onCreate(icicle);
		Instance = this;
		/*final ZLAndroidApplication application = ZLAndroidApplication.Instance();
		myFullScreenFlag =
			application.ShowStatusBarOption.getValue() ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
		getWindow().setFlags(
			WindowManager.LayoutParams.FLAG_FULLSCREEN, myFullScreenFlag
		);*/
		if (myPanel == null) {
			myPanel = new TextSearchButtonPanel();
			ZLApplication.Instance().registerButtonPanel(myPanel);
		}

		final TextView statusPositionText = (TextView) findViewById(R.id.statusbar_position_text);
		final TextView bookPositionText = (TextView) findViewById(R.id.book_position_text);
		final SeekBar bookPositionSlider = (SeekBar) findViewById(R.id.book_position_slider);
		bookPositionText.setText("");
		statusPositionText.setText("");
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
				updateEpdView(0);
				myInTouch = false;
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				myInTouch = true;
			}

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					final int page = progress + 1;
					final int pagesNumber = seekBar.getMax() + 1; 
					bookPositionText.setText(makePositionText(page, pagesNumber));
					if (!myInTouch) {
						gotoPage(page);
						updateEpdView(250);
					}
				}
			}
		});

		final TextView bookNoCover = (TextView) findViewById(R.id.book_no_cover_text);
		bookNoCover.setText(Resource.getResource("noCover").getValue());

		final FBReaderApp fbReader = (FBReaderApp)ZLApplication.Instance();
		fbReader.addAction(ActionCode.SHOW_LIBRARY, new ShowLibraryAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_PREFERENCES, new PreferencesAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOK_INFO, new BookInfoAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_CONTENTS, new ShowTOCAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOKMARKS, new ShowBookmarksAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_NETWORK_LIBRARY, new ShowNetworkLibraryAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_NETWORK_BROWSER, new ShowNetworkBrowserAction(this, fbReader));

		fbReader.addAction(ActionCode.SEARCH, new SearchAction(this, fbReader));
	}

	private static String makePositionText(int page, int pagesNumber) {
		return "" + page + " / " + pagesNumber;
	}

	@Override
	public void onStart() {
		super.onStart();

		/*final ZLAndroidApplication application = ZLAndroidApplication.Instance();
		final int fullScreenFlag =
			application.ShowStatusBarOption.getValue() ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
		if (fullScreenFlag != myFullScreenFlag) {
			finish();
			startActivity(new Intent(this, this.getClass()));
		}*/

		if (myPanel.ControlPanel == null) {
			myPanel.ControlPanel = new ControlPanel(this);

			myPanel.ControlPanel.addButton(ActionCode.FIND_PREVIOUS, false, R.drawable.text_search_previous);
			myPanel.ControlPanel.addButton(ActionCode.CLEAR_FIND_RESULTS, true, R.drawable.text_search_close);
			myPanel.ControlPanel.addButton(ActionCode.FIND_NEXT, false, R.drawable.text_search_next);

			RelativeLayout root = (RelativeLayout)findViewById(R.id.root_view);
			RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
			p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			p.addRule(RelativeLayout.CENTER_HORIZONTAL);
			root.addView(myPanel.ControlPanel, p);
		}

		final LinearLayout view = (LinearLayout) findViewById(R.id.epd_layout);
		EPDView.Instance().bindLayout((ViewGroup) view);
		EPDView.Instance().setVdsActive(true);
		EPDView.Instance().updateEpdViewDelay(200);
	}

	//private PowerManager.WakeLock myWakeLock;

	@Override
	public void onResume() {
		super.onResume();
		if (myPanel.ControlPanel != null) {
			myPanel.ControlPanel.setVisibility(myPanel.Visible ? View.VISIBLE : View.GONE);
		}
		/*if (ZLAndroidApplication.Instance().DontTurnScreenOffOption.getValue()) {
			myWakeLock =
				((PowerManager)getSystemService(POWER_SERVICE)).
					newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "FBReader");
			myWakeLock.acquire();
		} else {
			myWakeLock = null;
		}*/
		myReadMode = true;
	}

	@Override
	public void onPause() {
		/*if (myWakeLock != null) {
			myWakeLock.release();
		}*/
		if (myPanel.ControlPanel != null) {
			myPanel.Visible = myPanel.ControlPanel.getVisibility() == View.VISIBLE;
		}
		myReadMode = false;
		super.onPause();
	}

	@Override
	public void onStop() {
		EPDView.Instance().setVdsActive(false);
		if (myPanel.ControlPanel != null) {
			myPanel.ControlPanel.hide(false);
			myPanel.ControlPanel = null;
		}
		super.onStop();
	}


	@Override
	public void onDestroy() {
		final FBReaderApp fbReader = (FBReaderApp)ZLApplication.Instance();

		fbReader.removeAction(ActionCode.SHOW_LIBRARY);
		fbReader.removeAction(ActionCode.SHOW_PREFERENCES);
		fbReader.removeAction(ActionCode.SHOW_BOOK_INFO);
		fbReader.removeAction(ActionCode.SHOW_CONTENTS);
		fbReader.removeAction(ActionCode.SHOW_BOOKMARKS);
		fbReader.removeAction(ActionCode.SHOW_NETWORK_LIBRARY);
		fbReader.removeAction(ActionCode.SHOW_NETWORK_BROWSER);

		fbReader.removeAction(ActionCode.SEARCH);

		super.onDestroy();
	}

	private final Handler myNotifyApplicationHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			/*final boolean singleChange = msg.what == 1;
			updateEpdView(singleChange ? 0 : 10);*/
			updateEpdView(0);
		};
	};

	@Override
	public void notifyApplicationChanges(boolean singleChange) {
		myNotifyApplicationHandler.sendEmptyMessage(singleChange ? 1 : 0);
	}


	private int myCoverWidth;
	private int myCoverHeight;

	public void onEpdRepaintFinished() {
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
				bookTitle.setText(myViewBook.getTitle());
				int count = 0;
				final StringBuilder authors = new StringBuilder();
				for (Author a: myViewBook.authors()) {
					if (count++ > 0) {
						authors.append(",  ");
					}
					authors.append(a.DisplayName);
					if (count == 5) {
						break;
					}
				}
				bookAuthors.setText(authors.toString());

				Bitmap coverBitmap = null;
				final FormatPlugin plugin = PluginCollection.instance().getPlugin(myViewBook.File);
				if (plugin != null) {
					final ZLImage image = plugin.readCover(myViewBook);
					if (image != null) {
						final ZLAndroidImageManager mgr = (ZLAndroidImageManager) ZLAndroidImageManager.Instance();
						ZLAndroidImageData data = mgr.getImageData(image);
						if (data != null) {
							coverBitmap = data.getBitmap(2 * myCoverWidth, 2 * myCoverHeight);
						}
					}
				}
				if (coverBitmap != null) {
					bookCover.setImageBitmap(coverBitmap);
					bookCover.setVisibility(View.VISIBLE);
					bookNoCoverLayout.setVisibility(View.GONE);
				} else {
					bookCover.setImageDrawable(null);
					bookCover.setVisibility(View.GONE);
					bookNoCoverLayout.setVisibility(View.VISIBLE);
				}
			}
			if (EPDView.Instance().rotateFlag()) {
				final RotateAnimation anim = new RotateAnimation(90.0f, 90.0f, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
				anim.setFillEnabled(true);
				anim.setFillAfter(true);
				final View coverView = bookCover.getVisibility() == View.VISIBLE ? bookCover : bookNoCoverText;
				coverView.startAnimation(anim);
				if (coverView == bookCover) {
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

		findViewById(R.id.root_view).invalidate();

		final TextView bookPositionText = (TextView) findViewById(R.id.book_position_text);
		final SeekBar bookPositionSlider = (SeekBar) findViewById(R.id.book_position_slider);

		final ZLView view = fbreader.getCurrentView();
		if (view instanceof ZLTextView
				&& ((ZLTextView) view).getModel() != null
				&& ((ZLTextView) view).getModel().getParagraphsNumber() != 0) {
			ZLTextView textView = (ZLTextView) view;

			final int page = textView.computeCurrentPage();
			final int pagesNumber = textView.computePageNumber();

			bookPositionText.setText(makePositionText(page, pagesNumber));
			bookPositionSlider.setVisibility(View.VISIBLE);
			bookPositionSlider.setMax(pagesNumber - 1);
			bookPositionSlider.setProgress(page - 1);
		} else {
			bookPositionText.setText("");
			bookPositionSlider.setProgress(0);
			bookPositionSlider.setMax(1);
			bookPositionSlider.setVisibility(View.INVISIBLE);
		}

		if (SynchronousActivity.Instance != null) {
			SynchronousActivity.Instance.updateImage();
		}
	}

	public void updateEpdView(int delay) {
		updateEpdStatusbar();
		if (delay <= 0) {
			EPDView.Instance().updateEpdView();
		} else {
			EPDView.Instance().updateEpdViewDelay(delay);
		}
	}

	private void updateEpdStatusbar() {
		final TextView statusPositionText = (TextView) findViewById(R.id.statusbar_position_text);
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view instanceof ZLTextView
				&& ((ZLTextView) view).getModel() != null
				&& ((ZLTextView) view).getModel().getParagraphsNumber() != 0) {
			ZLTextView textView = (ZLTextView) view;
			final int page = textView.computeCurrentPage();
			final int pagesNumber = textView.computePageNumber();
			statusPositionText.setText(makePositionText(page, pagesNumber));
		} else {
			statusPositionText.setText("");
		}
	}


	void showTextSearchControls(boolean show) {
		if (myPanel.ControlPanel != null) {
			if (show) {
				myPanel.ControlPanel.show(true);
			} else {
				myPanel.ControlPanel.hide(false);
			}
		}
	}

	protected ZLApplication createApplication(String fileName) {
		new SQLiteBooksDatabase();
		final String[] args = (fileName != null) ? new String[] { fileName } : new String[0];
		return new FBReaderApp(args);
	}

	@Override
	public boolean onSearchRequested() {
		if (myPanel.ControlPanel != null) {
			final boolean visible = myPanel.ControlPanel.getVisibility() == View.VISIBLE;
			myPanel.ControlPanel.hide(false);
			SearchManager manager = (SearchManager)getSystemService(SEARCH_SERVICE);
			manager.setOnCancelListener(new SearchManager.OnCancelListener() {
				public void onCancel() {
					if ((myPanel.ControlPanel != null) && visible) {
						myPanel.ControlPanel.show(false);
					}
				}
			});
		}
		final FBReaderApp fbreader = (FBReaderApp)ZLApplication.Instance();
		startSearch(fbreader.TextSearchPatternOption.getValue(), true, null, false);
		return true;
	}
}
