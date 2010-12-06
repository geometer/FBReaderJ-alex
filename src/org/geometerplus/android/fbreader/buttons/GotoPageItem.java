package org.geometerplus.android.fbreader.buttons;

import android.content.Context;
import android.content.Intent;

import org.geometerplus.android.fbreader.NavigationActivity;

class GotoPageItem extends SpecialButton {

	@Override
	protected String getImageId() {
		return "page";
	}

	@Override
	public String getType() {
		return TYPE_GOTO_PAGE;
	}

	@Override
	public void onAction(Context context) {
		context.startActivity(
			new Intent(context.getApplicationContext(), NavigationActivity.class)
		);
	}
}
