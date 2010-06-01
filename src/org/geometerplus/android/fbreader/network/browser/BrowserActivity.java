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

package org.geometerplus.android.fbreader.network.browser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.ui.android.R;


public class BrowserActivity extends Activity {

	protected final ZLResource myResource = ZLResource.resource("browser");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.browser);
		setTitle(ZLResource.resource("networkView").getResource("browser").getValue());

		WebView view = (WebView) findViewById(R.id.webview);
		view.setWebChromeClient(new ChromeClient());
		view.setWebViewClient(new ViewClient());

		final Intent intent = getIntent();
		final Uri uri = intent.getData();
		if (uri != null) {
			view.loadUrl(uri.toString());
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		final Uri uri = intent.getData();
		if (uri != null) {
			WebView view = (WebView) findViewById(R.id.webview);
			view.loadUrl(uri.toString());
		}
	}
	
	private class ChromeClient extends WebChromeClient {
		@Override
		public void onReceivedTitle(WebView view, String title) {
			BrowserActivity.this.setTitle(title);
		}
	}

	private class ViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			return false;
		}
	}


	private MenuItem addMenuItem(Menu menu, int index, String resourceKey, int iconId) {
		final String label = myResource.getResource("menu").getResource(resourceKey).getValue();
		return menu.add(0, index, Menu.NONE, label).setIcon(iconId);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		addMenuItem(menu, 1, "go", R.drawable.ic_menu_networksearch);
		addMenuItem(menu, 2, "stop", R.drawable.ic_menu_networksearch);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		WebView view = (WebView) findViewById(R.id.webview);
		final boolean loading = view.getUrl() != null && view.getProgress() < 100;
		menu.findItem(1).setEnabled(!loading);
		menu.findItem(2).setEnabled(loading);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		WebView view = (WebView) findViewById(R.id.webview);
		switch (item.getItemId()) {
			case 1:
				return onSearchRequested();
			case 2:
				view.stopLoading();
				return true;
			default:
				return true;
		}
	}

	@Override
	public boolean onSearchRequested() {
		WebView view = (WebView) findViewById(R.id.webview);
		startSearch(view.getUrl(), true, null, false);
		return true;
	}
}