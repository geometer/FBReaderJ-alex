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

package org.geometerplus.android.fbreader.buttons;

import android.content.Context;
import android.view.View;

import org.geometerplus.zlibrary.core.util.ZLMiscUtil;

public abstract class AbstractButton {

	public static final String FBREADER_ACTION = "fbReaderAction";

	public static final String TYPE_GOTO_PAGE = "gotoPage";
	public static final String TEXT_SEARCH = "textSearch";

	public static AbstractButton createButton(String type, String data) {
		return null;
	}

	public abstract String getType();
	public abstract String getData();

	public abstract View createView(Context context);
	public abstract String getCaption();

	public abstract void onAction(Context context);

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AbstractButton)) {
			return false;
		}
		final AbstractButton button = (AbstractButton) o;
		return button.getType().equals(getType())
				&& ZLMiscUtil.equals(button.getData(), getData());
	}

	@Override
	public int hashCode() {
		final String data = getData();
		return getType().hashCode()
			+ ((data == null) ? 0 : data.hashCode());
	}

	public interface OnStartEditListener {
		public void onStartEdit(AbstractButton button);
	}

	public interface OnButtonSelectedListener {
		public void onButtonSelected(AbstractButton button);
	}

	protected OnButtonSelectedListener mySelectedListener;
	protected OnStartEditListener myEditListener;
	protected boolean myIsEditing = false;

	public void setStartEditListener(OnStartEditListener editListener) {
		myEditListener = editListener;
	}

	public void setItemSelectedListener(OnButtonSelectedListener listener) {
		mySelectedListener = listener;
	}

	public void startEdit() {
		myIsEditing = true;
	}

	public void stopEdit() {
		myIsEditing = false;
	}

	protected void setDefaultListeners(View view, final Context context) {
		view.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (myIsEditing) {
					if (mySelectedListener != null) {
						mySelectedListener.onButtonSelected(AbstractButton.this);
					}
				} else {
					onAction(context);
				}
			}
		});
		view.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View view) {
				if (myEditListener != null) {
					myEditListener.onStartEdit(AbstractButton.this);
					return true;
				}
				return false;
			}
		});
	}
}
