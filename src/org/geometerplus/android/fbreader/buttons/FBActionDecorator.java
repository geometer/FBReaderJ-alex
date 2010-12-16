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

import org.geometerplus.fbreader.fbreader.FBReaderApp;

import android.content.Context;

class FBActionDecorator extends FBActionButton {

	private final SimpleButton myButton;

	public FBActionDecorator(String imageId, String actionId, SimpleButton button) {
		super(imageId, actionId);
		myButton = button;
	}

	@Override
	public final String getCaption() {
		if (FBReaderApp.Instance().isActionVisible(myActionId)) {
			return super.getCaption();
		}
		return myButton.getCaption();
	}

	@Override
	public final String getData() {
		return myImageId + ":" + myActionId + ":" + myButton.getType() + ":" + myButton.getData();
	}

	@Override
	protected String getImageId() {
		if (FBReaderApp.Instance().isActionVisible(myActionId)) {
			return super.getImageId();
		}
		return myButton.getImageId();
	}

	@Override
	public String getType() {
		return FBREADER_ACTION_DECORATOR;
	}

	@Override
	public void onAction(Context context) {
		if (FBReaderApp.Instance().isActionVisible(myActionId)) {
			super.onAction(context);
		} else {
			myButton.onAction(context);
		}
		updateView();
	}

	@Override
	public boolean isVisible() {
		if (super.isVisible()) {
			return true;
		}
		return myButton.isVisible();
	}
}
