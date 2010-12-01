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

import org.geometerplus.zlibrary.ui.android.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


abstract class SpecialButton extends SimpleButton {

	@Override
	public final String getData() {
		return null;
	}

	protected abstract Drawable getIconDrawable();

	@Override
	protected final String getImageId() {
		return null;
	}

	@Override
	protected View createViewInternal(final Context context) {
		View res = super.createViewInternal(context);
		if (res != null) {
			return res;
		}
		final View view = LayoutInflater.from(context).inflate(R.layout.buttons_item, null);
		((TextView) view.findViewById(R.id.text)).setText(getCaption());
		((ImageView) view.findViewById(R.id.icon)).setImageDrawable(getIconDrawable());
		setDefaultListeners((ImageButton)view.findViewById(R.id.btn), context);
		return view;
	}
}
