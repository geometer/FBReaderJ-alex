package org.geometerplus.android.fbreader.buttons;

import android.content.Context;
import android.graphics.drawable.Drawable;

class GotoPageItem extends SpecialButton {

	@Override
	protected Drawable getIconDrawable() {
		return null;
	}

	@Override
	public void onAction(Context context) {
	}

	@Override
	public String getCaption() {
		return "Go to page";
	}

	@Override
	public String getType() {
		return TYPE_GOTO_PAGE;
	}
}
