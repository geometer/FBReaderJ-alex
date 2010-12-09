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
import java.util.List;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.*;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.text.view.style.ZLTextStyleCollection;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidActivity;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.library.Library;

import org.geometerplus.android.fbreader.buttons.AbstractButton;
import org.geometerplus.android.fbreader.buttons.ButtonsCollection;
import org.geometerplus.android.fbreader.buttons.SQLiteButtonsDatabase;

public final class FBReader extends ZLAndroidActivity {
	final static int REPAINT_CODE = 1;

	public static final String ACTION_START_SEARCH = "org.geometerplus.android.fbreader.FBReader.START_SEARCH";

	static FBReader Instance;

	private ArrayList<AbstractButton> myButtons = new ArrayList<AbstractButton>();
	private ImageView mySelector;
	private AbstractButton mySelectedButton;

	private boolean myReadMode;

	public static ZLResource getResource() {
		return ZLResource.resource("fbreader");
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


	private static class ReadingEPDView extends EPDView {

		public ReadingEPDView(FBReader activity) {
			super(activity);
		}

		@Override
		public boolean onTogglePressed(int arg1, int arg2) {
			final FBReader fbreader = (FBReader)getActivity();
			if (!fbreader.myReadMode /*&& SynchronousActivity.Instance == null*/) {
				changeFont();
			} else {
				fbreader.startActivity(
					new Intent(fbreader.getApplicationContext(), SynchronousActivity.class)
				);
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

		public void onEpdRepaintFinished() {
		}
	}
	private EPDView myEPDView = new ReadingEPDView(this);

	@Override
	protected String fileNameForEmptyUri() {
		return Library.getHelpFile().getPath();
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
		if (myPanel == null) {
			myPanel = new TextSearchButtonPanel();
			ZLApplication.Instance().registerButtonPanel(myPanel);
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

		if (mySelector == null) {
			mySelector = new ImageView(this);
			mySelector.setImageResource(R.drawable.selector);
			mySelector.setLayoutParams(new ViewGroup.LayoutParams(96, 144));
		}

		setupEditMode();

		myButtons.clear();
		ButtonsCollection.Instance().loadButtons(myButtons);
		updateButtons();
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

		if (myPanel.ControlPanel == null) {
			myPanel.ControlPanel = new ControlPanel(this);

			myPanel.ControlPanel.addButton(ActionCode.FIND_PREVIOUS, false, R.drawable.text_search_previous);
			myPanel.ControlPanel.addButton(ActionCode.CLEAR_FIND_RESULTS, true, R.drawable.text_search_close);
			myPanel.ControlPanel.addButton(ActionCode.FIND_NEXT, false, R.drawable.text_search_next);

			RelativeLayout root = (RelativeLayout)findViewById(R.id.panels_layout);
			RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
			p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			p.addRule(RelativeLayout.CENTER_HORIZONTAL);
			root.addView(myPanel.ControlPanel, p);
		}

		setupRotation();
	}

	//private PowerManager.WakeLock myWakeLock;

	@Override
	public void onResume() {
		super.onResume();
		myEPDView.onResume();
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
		myEPDView.onPause();
		super.onPause();
	}

	@Override
	public void onStop() {
		if (myPanel.ControlPanel != null) {
			myPanel.ControlPanel.hide(false);
			myPanel.ControlPanel = null;
		}
		super.onStop();
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (ACTION_START_SEARCH.equals(intent.getAction())) {
			onSearchRequested();
		}
	}

	protected ZLApplication createApplication(String fileName) {
		if (SQLiteBooksDatabase.Instance() == null) {
			new SQLiteBooksDatabase(this, "READER");
		}
		if (SQLiteButtonsDatabase.Instance() == null) {
			new SQLiteButtonsDatabase(this);
		}
		return new FBReaderApp(fileName);
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
		if (myPanel.ControlPanel != null) {
			if (show) {
				myPanel.ControlPanel.show(true);
			} else {
				myPanel.ControlPanel.hide(false);
			}
		}
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
