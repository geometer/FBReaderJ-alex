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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.*;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.view.style.ZLTextStyleCollection;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidActivity;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.ActionCode;

import org.geometerplus.android.fbreader.buttons.AbstractButton;
import org.geometerplus.android.fbreader.buttons.ButtonsCollection;
import org.geometerplus.android.fbreader.buttons.SQLiteButtonsDatabase;
import org.geometerplus.android.util.UIUtil;

public final class FBReader extends ZLAndroidActivity {
	public static final String BOOK_PATH_KEY = "BookPath";

	final static int REPAINT_CODE = 1;

	static FBReader Instance;

	private ArrayList<AbstractButton> myButtons = new ArrayList<AbstractButton>();
	private ImageView mySelector;
	private AbstractButton mySelectedButton;

	private boolean myReadMode;

	public static ZLResource getResource() {
		return ZLResource.resource("fbreader");
	}

	private static class TextSearchButtonPanel extends ControlButtonPanel {
		@Override
		public void onHide() {
			final ZLTextView textView = (ZLTextView)ZLApplication.Instance().getCurrentView();
			textView.clearFindResults();
		}
	}

	private static TextSearchButtonPanel myTextSearchPanel;
	private static ControlButtonPanel myFontSizeButtonPanel;


	private static class ReadingEPDView extends EPDView {

		public ReadingEPDView(FBReader activity) {
			super(activity);
		}

		@Override
		public boolean onTogglePressed(int arg1, int arg2) {
			final FBReader fbreader = (FBReader)getActivity();
			if (!fbreader.myReadMode) {
				changeFont();
			} else {
				Dialog dlg = SynchronousDialog.Instance;
				if (dlg != null) {
					dlg.dismiss();
				} else {
					new SynchronousDialog(getActivity(), this).show();
				}
			}
			return true;
		}

		private final static int FONT_DELTA = 9;
		private final static int FONT_START = 18;
		private final static int FONT_END = 63;
		private void changeFont() {
			final ZLIntegerRangeOption option =
				ZLTextStyleCollection.Instance().getBaseStyle().FontSizeOption;
			final int newValue = option.getValue() + FONT_DELTA;
			option.setValue((newValue > FONT_END) ? FONT_START : newValue);
			((FBReaderApp)ZLApplication.Instance()).clearTextCaches();
			ZLApplication.Instance().repaintView();
		}
	}
	private EPDView myEPDView = new ReadingEPDView(this);

	/*private String fileNameFromUri(Uri uri) {
		if (uri.equals(Uri.parse("file:///"))) {
			return Library.getHelpFile().getPath();
		} else {
			return uri.getPath();
		}
	}*/

	@Override
	protected ZLFile fileFromIntent(Intent intent) {
		String filePath = intent.getStringExtra(BOOK_PATH_KEY);
		if (filePath == null) {
			final Uri data = intent.getData();
			if (data != null) {
				filePath = data.getPath();
			}
		}
		return filePath != null ? ZLFile.createFileByPath(filePath) : null;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		((ZLAndroidLibrary)ZLAndroidLibrary.Instance()).setEventsListener(myEPDView);
		Instance = this;
		/*final ZLAndroidApplication application = ZLAndroidApplication.Instance();
		myFullScreenFlag =
			application.ShowStatusBarOption.getValue() ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
		getWindow().setFlags(
			WindowManager.LayoutParams.FLAG_FULLSCREEN, myFullScreenFlag
		);*/
		if (myTextSearchPanel == null) {
			myTextSearchPanel = new TextSearchButtonPanel();
			myTextSearchPanel.register();
		}
		if (myFontSizeButtonPanel == null) {
			myFontSizeButtonPanel = new ControlButtonPanel();
			myFontSizeButtonPanel.register();
		}

		final FBReaderApp fbReader = (FBReaderApp)ZLApplication.Instance();
		fbReader.addAction(ActionCode.SHOW_LIBRARY, new ShowLibraryAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_PREFERENCES, new ShowPreferencesAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOK_INFO, new ShowBookInfoAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_CONTENTS, new ShowTOCAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOKMARKS, new ShowBookmarksAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_NETWORK_LIBRARY, new ShowNetworkLibraryAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_NETWORK_BROWSER, new ShowNetworkBrowserAction(this, fbReader));

		fbReader.addAction(ActionCode.SEARCH, new SearchAction(this, fbReader));
		fbReader.addAction(ActionCode.ROTATE, new RotateAction(this, fbReader));
		fbReader.addAction(ActionCode.GOTO_PAGE, new GoToPageAction(this, fbReader));
		fbReader.addAction(ActionCode.FONT_SIZE, new FontSizeAction(this, fbReader));

		if (mySelector == null) {
			mySelector = new ImageView(this);
			mySelector.setImageResource(R.drawable.selector);
			mySelector.setLayoutParams(new ViewGroup.LayoutParams(96, 144));
		}

		setupEditMode();
		initializeButtons();
	}

	private final void initializeButtons() {
		myButtons.clear();
		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				updateButtons();
			}
		};
		UIUtil.wait("loadingButtons", new Runnable() {
			public void run() {
				ButtonsCollection.Instance().loadButtons(myButtons);
				handler.sendEmptyMessage(0);
			}
		}, this);
	}

	@Override
	public void onStart() {
		super.onStart();
		((ZLAndroidLibrary)ZLAndroidLibrary.Instance()).setEventsListener(myEPDView);

		/*final ZLAndroidApplication application = ZLAndroidApplication.Instance();
		final int fullScreenFlag =
			application.ShowStatusBarOption.getValue() ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
		if (fullScreenFlag != myFullScreenFlag) {
			finish();
			startActivity(new Intent(this, this.getClass()));
		}*/

		final RelativeLayout root = (RelativeLayout)findViewById(R.id.panels_layout);
		if (!myTextSearchPanel.hasControlPanel()) {
			final ControlPanel panel = new ControlPanel(this);

			panel.addButton(ActionCode.FIND_PREVIOUS, false, R.drawable.text_search_previous);
			panel.addButton(ActionCode.CLEAR_FIND_RESULTS, true, R.drawable.text_search_close);
			panel.addButton(ActionCode.FIND_NEXT, false, R.drawable.text_search_next);

			myTextSearchPanel.setControlPanel(panel, root, false);
		}
		if (!myFontSizeButtonPanel.hasControlPanel()) {
			final ControlPanel panel = new ControlPanel(this);

			final String string = getResource().getResource("rotationButton").getValue();
			panel.addButton(ActionCode.DECREASE_FONT, false,
				RotatedStringDrawable.create(string, ZLAndroidApplication.ROTATE_0, 28));
			panel.addButton(null, true, R.drawable.text_search_close);
			panel.addButton(ActionCode.INCREASE_FONT, false,
				RotatedStringDrawable.create(string, ZLAndroidApplication.ROTATE_0, 44));

			myFontSizeButtonPanel.setControlPanel(panel, root, false);
		}

		setupRotation();
	}

	@Override
	public void onResume() {
		super.onResume();
		myEPDView.onResume();
		ControlButtonPanel.restoreVisibilities();
		myReadMode = true;
	}

	@Override
	public void onPause() {
		ControlButtonPanel.saveVisibilities();
		myReadMode = false;
		myEPDView.onPause();
		super.onPause();
	}

	@Override
	public void onStop() {
		ControlButtonPanel.removeControlPanels();
		super.onStop();
	}


	protected ZLApplication createApplication(ZLFile file) {
		if (SQLiteBooksDatabase.Instance() == null) {
			new SQLiteBooksDatabase(this, "READER");
		}
		if (SQLiteButtonsDatabase.Instance() == null) {
			new SQLiteButtonsDatabase(this);
		}
		return new FBReaderApp(file != null ? file.getPath() : null);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REPAINT_CODE:
			{
				final FBReaderApp fbreader = (FBReaderApp)ZLApplication.Instance();
				fbreader.clearTextCaches();
				fbreader.repaintView();
				break;
			}
		}
	}

	void showTextSearchControls(boolean show) {
		if (show) {
			myTextSearchPanel.show(true);
		} else {
			myTextSearchPanel.hide(false);
		}
	}

	@Override
	public boolean onSearchRequested() {
		final LinkedList<Boolean> visibilities = new LinkedList<Boolean>();
		ControlButtonPanel.saveVisibilitiesTo(visibilities);
		ControlButtonPanel.hideAllPendingNotify();
		final SearchManager manager = (SearchManager)getSystemService(SEARCH_SERVICE);
		manager.setOnCancelListener(new SearchManager.OnCancelListener() {
			public void onCancel() {
				ControlButtonPanel.restoreVisibilitiesFrom(visibilities);
				manager.setOnCancelListener(null);
			}
		});
		final FBReaderApp fbreader = (FBReaderApp)ZLApplication.Instance();
		startSearch(fbreader.TextSearchPatternOption.getValue(), true, null, false);
		return true;
	}

	private void setupRotation() {
		final String string = getResource().getResource("rotationButton").getValue();
		setupRotationButton(string, R.id.rotate_bottom, ZLAndroidApplication.ROTATE_0);
		setupRotationButton(string, R.id.rotate_left, ZLAndroidApplication.ROTATE_90);
		setupRotationButton(string, R.id.rotate_top, ZLAndroidApplication.ROTATE_180);
		setupRotationButton(string, R.id.rotate_right, ZLAndroidApplication.ROTATE_270);
	}

	private void setupRotationButton(String string, int id, final int angle) {
		final ImageButton btn = (ImageButton)findViewById(id);
		btn.setImageDrawable(RotatedStringDrawable.create(string, angle));
		btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				((ZLAndroidLibrary)ZLAndroidLibrary.Instance()).rotate(angle);
				showRotationButtons(false);
				FBReaderApp.Instance().repaintView();
			}
		});
	}

	private void showRotationButtons(boolean show) {
		final View scrollView = findViewById(R.id.root_scroll_view);
		final View rotateView = findViewById(R.id.root_rotate_view);
		scrollView.setVisibility(show ? View.GONE : View.VISIBLE);
		rotateView.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public void onRotationRequested() {
		showRotationButtons(true);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			final View scrollView = findViewById(R.id.root_scroll_view);
			if (scrollView.getVisibility() == View.GONE) {
				findViewById(R.id.root_rotate_view).setVisibility(View.GONE);
				scrollView.setVisibility(View.VISIBLE);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	public void onFontSizeRequested() {
		myFontSizeButtonPanel.show(true);
	}

	public void onNavigationRequested() {
		new NavigationDialog(this, myEPDView).show();
		myEPDView.updateEpdView(200);
	}

	// --- Code from launcher ---

	private void setupEditMode() {
		((ImageButton)findViewById(R.id.exit)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				removeSelection();
				stopEdit();
				saveChanges();
				updateButtons();
			}
		});
		((ImageButton)findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				removeSelection();
				myButtons.clear();
				ButtonsCollection.Instance().loadButtons(myButtons);
				updateButtons();
				startEdit();
			}
		});
		((ImageButton) findViewById(R.id.turnLeft)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				final View selectedView = (View)mySelector.getParent();
				if (mySelectedButton != null && selectedView != null) {
					final LinearLayout parent = (LinearLayout)selectedView.getParent();
					final int index = getItemIndex(selectedView, parent);
					if (index > 0) {
						focusScroll(parent.getChildAt(index - 1));
						parent.removeView(selectedView);
						parent.addView(selectedView, index - 1);
						if (parent == findViewById(R.id.topDock)) {
							AbstractButton leftItem = myButtons.get(2 * index - 2);
							myButtons.remove(mySelectedButton);
							myButtons.add(2 * index - 2, mySelectedButton);
							myButtons.remove(leftItem);
							myButtons.add(2 * index, leftItem);
						} else {
							AbstractButton leftItem = myButtons.get(2 * index - 1);
							myButtons.remove(mySelectedButton);
							myButtons.add(2 * index - 1, mySelectedButton);
							myButtons.remove(leftItem);
							myButtons.add(2 * index + 1, leftItem);
						}
					} else {
						focusScroll(parent.getChildAt(0));
					}
				}
			}
		});
		((ImageButton) findViewById(R.id.turnRight)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				final View selectedView = (View)mySelector.getParent();
				if (mySelectedButton != null && selectedView != null) {
					LinearLayout parent = (LinearLayout)selectedView.getParent();
					final int index = getItemIndex(selectedView, parent);
					if (index < 0) {
						return;
					}
					if (index + 1 < parent.getChildCount()) {
						focusScroll(parent.getChildAt(index + 1));
						parent.removeView(selectedView);
						if (parent.getChildCount() < index + 1) {
							parent.addView(selectedView);
						} else {
							parent.addView(selectedView, index + 1);
						}
						focusScroll(selectedView);
						if (parent == findViewById(R.id.topDock)) {
							AbstractButton rightItem = myButtons.get(2 * index + 2);
							myButtons.remove(rightItem);
							myButtons.add(2 * index, rightItem);
							myButtons.remove(mySelectedButton);
							myButtons.add(2 * index + 2, mySelectedButton);
						} else {
							AbstractButton rightItem = myButtons.get(2 * index + 3);
							myButtons.remove(rightItem);
							myButtons.add(2 * (index) + 1, rightItem);
							myButtons.remove(mySelectedButton);
							myButtons.add(2 * (index) + 3, mySelectedButton);
						}
					} else if (index == parent.getChildCount() - 1) {
						focusScroll(parent.getChildAt(parent.getChildCount() - 1));
					}
				}
			}
		});
		((ImageButton) findViewById(R.id.turnUp)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				final View selectedView = (View)mySelector.getParent();
				if (mySelectedButton != null && selectedView != null) {
					final LinearLayout parent = (LinearLayout)selectedView.getParent();

					final LinearLayout topDock = (LinearLayout)findViewById(R.id.topDock);
					final LinearLayout bottomDock = (LinearLayout)findViewById(R.id.bottomDock);
					final LinearLayout opposite = (parent == topDock) ? bottomDock : topDock;

					final int index = getItemIndex(selectedView, parent);
					final View oppositeView = opposite.getChildAt(index);
					if (oppositeView != null) {
						opposite.removeView(oppositeView);
						parent.removeView(selectedView);
						opposite.addView(selectedView, index);
						parent.addView(oppositeView, index);
						if (parent == topDock) {
							myButtons.remove(mySelectedButton);
							myButtons.add(2 * index + 1, mySelectedButton);
						} else {
							myButtons.remove(mySelectedButton);
							myButtons.add(2 * index, mySelectedButton);
						}
					}
					focusScroll(selectedView);
				}
			}
		});
		((ImageButton) findViewById(R.id.turnDown)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				final View selectedView = (View)mySelector.getParent();
				if (mySelectedButton != null && selectedView != null) {
					final LinearLayout parent = (LinearLayout)selectedView.getParent();

					final LinearLayout topDock = (LinearLayout)findViewById(R.id.topDock);
					final LinearLayout bottomDock = (LinearLayout)findViewById(R.id.bottomDock);
					final LinearLayout opposite = (parent == topDock) ? bottomDock : topDock;

					final int index = getItemIndex(selectedView, parent);
					final View oppositeView = opposite.getChildAt(index);
					if (oppositeView != null) {
						opposite.removeView(oppositeView);
						parent.removeView(selectedView);
						opposite.addView(selectedView, index);
						parent.addView(oppositeView, index);
						if (parent == topDock) {
							myButtons.remove(mySelectedButton);
							myButtons.add(2 * index + 1, mySelectedButton);
						} else {
							myButtons.remove(mySelectedButton);
							myButtons.add(2 * index, mySelectedButton);
						}
					}
					focusScroll(selectedView);
				}
			}
		});
		((ImageButton) findViewById(R.id.add)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				final AddItemDialog.OnAddButtonListener listener = new AddItemDialog.OnAddButtonListener() {
					public void onAddButton(AbstractButton button) {
						final LinearLayout topDock = (LinearLayout)findViewById(R.id.topDock);
						final LinearLayout bottomDock = (LinearLayout)findViewById(R.id.bottomDock);
						final LinearLayout layout;
						if (topDock.getChildCount() <= bottomDock.getChildCount()) {
							layout = topDock;
						} else {
							layout = bottomDock;
						}
						addItemView(button, layout);
						final int resultIndex;
						if (layout == topDock) {
							resultIndex = layout.getChildCount() * 2 - 2;
						} else {
							resultIndex = layout.getChildCount() * 2 - 1;
						}
						myButtons.add(resultIndex, button);
						saveChanges();
					}
				};
				ArrayList<AbstractButton> buttons = new ArrayList<AbstractButton>();
				ButtonsCollection.Instance().loadAllButtons(buttons);
				buttons.removeAll(myButtons);
				new AddItemDialog(FBReader.this, buttons, listener).show();
			}
		});
		((ImageButton) findViewById(R.id.remove)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				final View selectedView = (View)mySelector.getParent();
				if (mySelectedButton != null && selectedView != null) {
					final int limit = 10;
					if (myButtons.size() <= limit) {
						final String message = getResource().getResource("buttonsLimit").getValue()
							.replaceAll("%s", String.valueOf(limit));
						Toast t = Toast.makeText(FBReader.this, message, Toast.LENGTH_LONG);
						t.setGravity(Gravity.CENTER, t.getXOffset(), t.getYOffset());
						t.show();
						return;
					}
					((LinearLayout)selectedView.getParent()).removeView(selectedView);
					myButtons.remove(mySelectedButton);
					removeSelection();
					saveChanges();
					updateButtons();
				}
			}
		});
	}

	private void updateButtons() {
		final LinearLayout topDock = (LinearLayout) findViewById(R.id.topDock);
		final LinearLayout bottomDock = (LinearLayout) findViewById(R.id.bottomDock);
		topDock.removeAllViews();
		bottomDock.removeAllViews();
		int count = 0;
		for (AbstractButton btn : myButtons) {
			if (count++ % 2 == 0) {
				addItemView(btn, topDock);
			} else {
				addItemView(btn, bottomDock);
			}

		}
	}

	private void addItemView(AbstractButton btn, LinearLayout layout) {
		final FrameLayout view = new FrameLayout(layout.getContext());
		view.setLayoutParams(new ViewGroup.LayoutParams(96, 144));

		btn.setStartEditListener(new AbstractButton.OnStartEditListener() {
			public void onStartEdit(AbstractButton button) {
				setSelection(button, view);
				startEdit();
			}
		});
		btn.setItemSelectedListener(new AbstractButton.OnButtonSelectedListener() {
			public void onButtonSelected(AbstractButton button) {
				setSelection(button, view);
			}
		});

		final View itemView = btn.createView(this);
		final ViewParent parent = itemView.getParent();
		if (parent != null) {
			((ViewGroup) parent).removeView(itemView);
		}
		view.addView(itemView);

		layout.addView(view);
	}

	private void startEdit() {
		for (AbstractButton btn : myButtons) {
			btn.startEdit();
		}
		findViewById(R.id.root_edit_view).setVisibility(View.VISIBLE);
	}

	private void stopEdit() {
		findViewById(R.id.root_edit_view).setVisibility(View.GONE);
		for (AbstractButton btn : myButtons) {
			btn.stopEdit();
		}
	}

	private void setSelection(AbstractButton btn, ViewGroup view) {
		removeSelection();
		view.addView(mySelector);
		mySelectedButton = btn;
	}

	private void removeSelection() {
		mySelectedButton = null;
		final ViewParent parent = mySelector.getParent();
		if (parent != null) {
			((ViewGroup)parent).removeView(mySelector);
		}
	}

	private void saveChanges() {
		ButtonsCollection.Instance().saveButtons(myButtons);
	}

	private void focusScroll(View view) {
		final HorizontalScrollView scrollView = (HorizontalScrollView)findViewById(R.id.root_scroll_view);
		if (scrollView.getScrollX() + scrollView.getWidth() < view.getRight()) {
			scrollView.smoothScrollTo(view.getRight() - scrollView.getWidth(), 0);
		} else if (scrollView.getScrollX() > view.getLeft()) {
			scrollView.smoothScrollTo(view.getLeft(), 0);
		}
	}

	private int getItemIndex(View view, LinearLayout dock) {
		for (int index = 0; index < dock.getChildCount(); ++index) {
			if (dock.getChildAt(index) == view) {
				return index;
			}
		}
		return -1;
	}

	private static class AddItemDialog extends Dialog {

		public interface OnAddButtonListener {
			void onAddButton(AbstractButton button);
		}

		public AddItemDialog(Context context, List<AbstractButton> buttons,
				final OnAddButtonListener listener) {
			super(context, android.R.style.Theme_Translucent_NoTitleBar);
			setContentView(R.layout.add_button);

			final AbstractButton.OnButtonSelectedListener buttonListener = new AbstractButton.OnButtonSelectedListener() {
				public void onButtonSelected(AbstractButton button) {
					if (listener != null) {
						listener.onAddButton(button);
					}
					AddItemDialog.this.dismiss();
				}
			};

			final LinearLayout apps = (LinearLayout)findViewById(R.id.apps);
			apps.removeAllViews();
			for (AbstractButton btn: buttons) {
				final View itemView = btn.createView(context);
				final ViewParent parent = itemView.getParent();
				if (parent != null) {
					((ViewGroup) parent).removeView(itemView);
				}
				apps.addView(itemView);
				btn.startEdit();
				btn.setItemSelectedListener(buttonListener);
				btn.setStartEditListener(null);
			}

			((ImageButton)findViewById(R.id.exit)).setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					AddItemDialog.this.cancel();
				}
			});

			final TextView tip = (TextView)findViewById(R.id.tip);
			tip.setText(FBReader.getResource().getResource("addButtonTip").getValue());
		}
	}
}
