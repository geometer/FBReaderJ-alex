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

import java.io.InputStream;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

abstract class SimpleButton extends AbstractButton {

    protected abstract String getImageId();

    private Drawable createDrawable(Context context, String id) {
		final String fileName = id + ".png";
		try {
			final String locale = context.getResources().getConfiguration().locale.getLanguage();
			final InputStream image = context.getAssets().open("buttons-" + locale + "/" + fileName);
			return BitmapDrawable.createFromStream(image, fileName);
		} catch (Exception e) {
		}
		try {
			final InputStream image = context.getAssets().open("buttons/" + fileName);
			return BitmapDrawable.createFromStream(image, fileName);
		} catch (Exception e) {
		}
		return null;
	}

    private Drawable createDrawableNormal(Context context) {
    	final String id = getImageId();
    	if (id == null) {
    		return null;
    	}
        return createDrawable(context, id);
    }

    private Drawable createDrawablePressed(Context context) {
    	final String id = getImageId();
    	if (id == null) {
    		return null;
    	}
        return createDrawable(context, id + "_focus");
    }

    private Drawable getImageDrawable(Context context) {
        final Drawable normal = createDrawableNormal(context);
        if (normal == null) {
            return null;
        }
        final Drawable pressed = createDrawablePressed(context);
        if (pressed == null) {
            return null;
        }
        android.graphics.drawable.StateListDrawable image = new StateListDrawable();
        image.addState(new int[]{-android.R.attr.state_pressed}, normal);
        image.addState(new int[]{android.R.attr.state_pressed}, pressed);
        return image;
    }

    protected View createViewInternal(final Context context) {
        final Drawable image = getImageDrawable(context);
        if (image == null) {
        	return null;
        }
        final ImageButton view = new ImageButton(context);
        view.setImageDrawable(image);
        setDefaultListeners(view, context);
        return view;
    }

    private View myView;

    @Override
    public View createView(final Context context) {
        if (myView == null) {
            myView = createViewInternal(context);
            if (myView == null) {
                final Button view = new Button(context);
                view.setText(getCaption());
                setDefaultListeners(view, context);
                myView = view;
            }
            myView.setPadding(0, 0, 0, 0);
            myView.setLayoutParams(new ViewGroup.LayoutParams(96, 144));
        }
        return myView;
    }
}
