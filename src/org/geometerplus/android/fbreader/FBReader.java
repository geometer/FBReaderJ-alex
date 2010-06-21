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

import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidActivity;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.fbreader.ActionCode;

public final class FBReader extends ZLAndroidActivity {
	static FBReader Instance;

	//private int myFullScreenFlag;
	private boolean myReadMode;

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
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		/*
		android.telephony.TelephonyManager tele =
			(android.telephony.TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		System.err.println(tele.getNetworkOperator());
		*/
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

		final TextView bookPositionText = (TextView) findViewById(R.id.book_position_text);
		final SeekBar bookPositionSlider = (SeekBar) findViewById(R.id.book_position_slider);
		bookPositionText.setText("");
		bookPositionSlider.setProgress(0);
		bookPositionSlider.setMax(1);
		bookPositionSlider.setVisibility(View.INVISIBLE);

		bookPositionSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			private boolean myInTouch;

			private void gotoPage(int page) {
				final ZLView view = ZLApplication.Instance().getCurrentView();
				if (view instanceof ZLTextView) {
					ZLTextView textView = (ZLTextView) view;
					textView.gotoPage(page);
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

	private PowerManager.WakeLock myWakeLock;

	@Override
	public void onResume() {
		super.onResume();
		if (myPanel.ControlPanel != null) {
			myPanel.ControlPanel.setVisibility(myPanel.Visible ? View.VISIBLE : View.GONE);
		}
		if (ZLAndroidApplication.Instance().DontTurnScreenOffOption.getValue()) {
			myWakeLock =
				((PowerManager)getSystemService(POWER_SERVICE)).
					newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "FBReader");
			myWakeLock.acquire();
		} else {
			myWakeLock = null;
		}
		myReadMode = true;
	}

	@Override
	public void onPause() {
		if (myWakeLock != null) {
			myWakeLock.release();
		}
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
	public void notifyApplicationChanges(boolean singleChange) {
		updateEpdView(singleChange ? 0 : 200);
	}

	public void onEpdRepaintFinished() {
		final org.geometerplus.fbreader.fbreader.FBReader fbreader =
			(org.geometerplus.fbreader.fbreader.FBReader)ZLApplication.Instance();
		final TextView bookTitle = (TextView) findViewById(R.id.book_title);
		if (fbreader.Model != null && fbreader.Model.Book != null) {
			bookTitle.setText(fbreader.Model.Book.getTitle());
		} else {
			bookTitle.setText("");
		}

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
	}

	public void updateEpdView(int delay) {
		if (delay <= 0) {
			EPDView.Instance().updateEpdView();
		} else {
			EPDView.Instance().updateEpdViewDelay(delay);
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
		String[] args = (fileName != null) ? new String[] { fileName } : new String[0];
		return new org.geometerplus.fbreader.fbreader.FBReader(args);
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
		final org.geometerplus.fbreader.fbreader.FBReader fbreader =
			(org.geometerplus.fbreader.fbreader.FBReader)ZLApplication.Instance();
		startSearch(fbreader.TextSearchPatternOption.getValue(), true, null, false);
		return true;
	}
}
