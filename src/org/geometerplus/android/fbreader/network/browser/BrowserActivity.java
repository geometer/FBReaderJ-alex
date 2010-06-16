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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.util.ZLNetworkUtil;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.network.NetworkLibrary;

import org.geometerplus.android.fbreader.network.BookDownloaderService;


public class BrowserActivity extends Activity {

	protected final ZLResource myResource = ZLResource.resource("browser");

	private String myStoreInRecentUrls;
	private Object myStoreInRecentUrlsLock = new Object();

	private void setStoreInRecentUrls(String url) {
		synchronized (myStoreInRecentUrlsLock) {
			myStoreInRecentUrls = url;
		}
	}

	private void storeUrlInRecents() {
		synchronized (myStoreInRecentUrlsLock) {
			if (myStoreInRecentUrls != null) {
				System.err.println("STORE IN RECENTS: " + myStoreInRecentUrls);
				SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
						getApplicationContext(),
						RecentUrlsProvider.AUTHORITY,
						RecentUrlsProvider.MODE);
		        suggestions.saveRecentQuery(myStoreInRecentUrls, null);
			}
			myStoreInRecentUrls = null;
		}
	}

	private void downloadBook(String url) {
		startService(
			new Intent(Intent.ACTION_VIEW, Uri.parse(url),
					getApplicationContext(), BookDownloaderService.class)
				//.putExtra(BookDownloaderService.BOOK_FORMAT_KEY, ref.BookFormat)
				//.putExtra(BookDownloaderService.REFERENCE_TYPE_KEY, resolvedType)
				//.putExtra(BookDownloaderService.CLEAN_URL_KEY, ref.cleanURL())
				//.putExtra(BookDownloaderService.TITLE_KEY, book.Title)
				//.putExtra(BookDownloaderService.SSL_CERTIFICATE_KEY, sslCertificate)
		);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_PROGRESS);

		setContentView(R.layout.browser);
		setTitle(ZLResource.resource("networkView").getResource("browser").getValue());

		WebView view = (WebView) findViewById(R.id.webview);
		view.setWebChromeClient(new ChromeClient());
		view.setWebViewClient(new ViewClient());

		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setUserAgentString(ZLNetworkUtil.getUserAgent());
		view.getSettings().setBuiltInZoomControls(true);
		view.getSettings().setUseWideViewPort(true);

		final Intent intent = getIntent();
		final Uri uri = intent.getData();

		final NetworkLibrary library = NetworkLibrary.Instance();
		final String url;
		if (uri != null) {
			url = uri.toString();
			setStoreInRecentUrls(url);
		} else {
			url = library.NetworkBrowserPageOption.getValue();
		}
		view.loadUrl(url);
		library.NetworkBrowserPageOption.setValue(url);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		final Uri uri = intent.getData();
		if (uri != null) {
			WebView view = (WebView) findViewById(R.id.webview);
			final String url = uri.toString();
			setStoreInRecentUrls(url);
			view.loadUrl(url);
			NetworkLibrary.Instance().NetworkBrowserPageOption.setValue(url);
		}
	}

	private class ChromeClient extends WebChromeClient {
		@Override
		public void onReceivedTitle(WebView view, String title) {
			BrowserActivity.this.setTitle(title);
		}
		
		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			final boolean inProgress = newProgress < 100;
			setProgressBarIndeterminateVisibility(inProgress);
			setProgressBarVisibility(inProgress);
			if (inProgress) {
				setProgress(100 * newProgress); // title progress is in range 0..10000
			} else {
				storeUrlInRecents();
			}
		}
	}

	private class ViewClient extends WebViewClient {

		private String[] mySupportedBooksExtensions = {".epub", ".fb2", ".fb2.zip"};

		@Override
		public void onLoadResource(final WebView view, final String url) {
			final Uri uri = Uri.parse(url);
			String path = uri.getPath();
			if (path != null) {
				path = path.toLowerCase();
				for (String ext: mySupportedBooksExtensions) {
					if (path.endsWith(ext)) {
						view.stopLoading();
						downloadBook(url);
						break;
					}
				}
			}
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			final String errorText = myResource.getResource("errorText").getValue().replaceAll("%s", description);
			Toast.makeText(BrowserActivity.this, errorText, Toast.LENGTH_LONG).show();
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			storeUrlInRecents();
			return false;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			NetworkLibrary.Instance().NetworkBrowserPageOption.setValue(url);
		}
		
		@Override
		public void onFormResubmission(WebView view, final Message dontResend, final Message resend) {
			final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
						resend.sendToTarget();
					} else {
						dontResend.sendToTarget();
					}
				}
			};
			final ZLResource buttonResource = ZLResource.resource("dialog").getResource("button");
			new AlertDialog.Builder(BrowserActivity.this)
				.setTitle(myResource.getResource("confirmation").getValue())
				.setMessage(myResource.getResource("resubmitForm").getValue())
				.setPositiveButton(buttonResource.getResource("ok").getValue(), listener)
				.setNegativeButton(buttonResource.getResource("cancel").getValue(), listener)
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						listener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
					}
				}).create().show();
		}

		@Override
		public void onReceivedHttpAuthRequest(WebView view, 
				final HttpAuthHandler handler, String host, String realm) {
			final View layout = getLayoutInflater().inflate(R.layout.browser_authentication_dialog, null);
			setupLabel(layout, R.id.login_text, "login", R.id.login);
			setupLabel(layout, R.id.password_text, "password", R.id.password);
			final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
						TextView loginView = (TextView) layout.findViewById(R.id.login);
						TextView passwordView = (TextView) layout.findViewById(R.id.password);
						handler.proceed(loginView.getText().toString(), passwordView.getText().toString());
					} else {
						handler.cancel();
					}
				}
			};
			final ZLResource buttonResource = ZLResource.resource("dialog").getResource("button");
			new AlertDialog.Builder(BrowserActivity.this)
				.setView(layout)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(myResource.getResource("authenticationRequired")
						.getValue().replaceAll("%s1", host).replaceAll("%s2", realm))
				.setPositiveButton(buttonResource.getResource("ok").getValue(), listener)
				.setNegativeButton(buttonResource.getResource("cancel").getValue(), listener)
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						listener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
					}
				}).create().show();
		}
	}

	private void setupLabel(View layout, int labelId, String key, int labelForId) {
		final View viewFor = layout.findViewById(labelForId);
		viewFor.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		final TextView label = (TextView) layout.findViewById(labelId);
		label.setText(myResource.getResource(key).getValue());
		label.getLayoutParams().height = viewFor.getMeasuredHeight();
	}

	private MenuItem addMenuItem(Menu menu, int index, String resourceKey, int iconId) {
		final String label = myResource.getResource("menu").getResource(resourceKey).getValue();
		return menu.add(0, index, Menu.NONE, label)/*.setIcon(iconId)*/;
	}

	
	private static final int OPTION_GO = 1; 
	private static final int OPTION_STOP = 2;
	private static final int OPTION_RELOAD = 3;
	private static final int OPTION_BACK = 4;
	private static final int OPTION_FORWARD = 5;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		addMenuItem(menu, OPTION_GO, "go", 0);
		addMenuItem(menu, OPTION_STOP, "stop", 0);
		addMenuItem(menu, OPTION_RELOAD, "reload", 0);
		addMenuItem(menu, OPTION_BACK, "back", 0);
		addMenuItem(menu, OPTION_FORWARD, "forward", 0);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		WebView view = (WebView) findViewById(R.id.webview);
		final boolean loading = view.getUrl() != null && view.getProgress() < 100;
		menu.findItem(OPTION_GO).setEnabled(!loading).setVisible(!loading);
		menu.findItem(OPTION_STOP).setEnabled(loading).setVisible(loading);
		menu.findItem(OPTION_RELOAD).setEnabled(!loading && view.getUrl() != null);
		menu.findItem(OPTION_BACK).setEnabled(!loading && view.canGoBack());
		menu.findItem(OPTION_FORWARD).setEnabled(!loading && view.canGoForward());
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		WebView view = (WebView) findViewById(R.id.webview);
		switch (item.getItemId()) {
			default:
				return true;
			case OPTION_GO:
				return onSearchRequested();
			case OPTION_STOP:
				view.stopLoading();
				return true;
			case OPTION_RELOAD:
				view.reload();
				return true;
			case OPTION_BACK:
				view.goBack();
				return true;
			case OPTION_FORWARD:
				view.goForward();
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