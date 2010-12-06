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
import org.geometerplus.zlibrary.core.resources.ZLResource;

import android.content.Context;

class FBActionButton extends SimpleButton {

	private final String myImageId;
	private final String myActionId;

	public FBActionButton(String imageId, String actionId) {
		myImageId = imageId;
		myActionId = actionId;
	}

	@Override
	public final String getCaption() {
		return ZLResource.resource("menu").getResource(myActionId).getValue();
	}

	@Override
	public final String getData() {
		return myImageId + ":" + myActionId;
	}

	@Override
	protected String getImageId() {
		return myImageId;
	}

	@Override
	public String getType() {
		return FBREADER_ACTION;
	}

	@Override
	public void onAction(Context context) {
		FBReaderApp.Instance().doAction(myActionId);
	}
}
