/*
 * Copyright (C) 2007-2010 Geometer Plus <contact@geometerplus.com>
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

package org.geometerplus.zlibrary.ui.android.library;

import java.io.*;
import java.util.*;

import android.app.Activity;
import android.app.Application;
import android.content.res.AssetFileDescriptor;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.image.ZLImage;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;
import org.geometerplus.zlibrary.ui.android.dialogs.ZLAndroidDialogManager;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidResourceBasedImageData;

import org.geometerplus.android.fbreader.network.BookDownloader;
import org.geometerplus.android.fbreader.network.BookDownloaderService;

import org.geometerplus.fbreader.network.NetworkLibrary;

public final class ZLAndroidLibrary extends ZLibrary {
	private Activity myActivity;
	private final Application myApplication;
	private ZLAndroidWidget myWidget;

	public interface EventsListener {
		/*
		 * singleChange parameter must be <code>true</code> if there will 
		 * be no changes in near future;
		 * 
		 * singleChange parameter must be <code>false</code> if there can be
		 * another changes in near future.  
		 */
		void notifyApplicationChanges(boolean singleChange);
		void onEpdRepaintFinished();
	}

	private EventsListener myEventsListener;

	ZLAndroidLibrary(Application application) {
		myApplication = application;
	}

	public void setActivity(Activity activity) {
		myActivity = activity;
		((ZLAndroidDialogManager)ZLAndroidDialogManager.Instance()).setActivity(activity);
		myWidget = (ZLAndroidWidget)myActivity.findViewById(R.id.main_view_epd);
		if (myWidget == null) {
			throw new RuntimeException("Activity " + myActivity.getClass() + " doesn't have R.id.main_view_epd view.");
		}
		myWidget.setRotation(ZLAndroidApplication.Instance().RotationFlag);
	}

	public void setEventsListener(EventsListener listener) {
		myEventsListener = listener;
	}

	public void rotate(int angle) {
		ZLAndroidApplication.Instance().RotationFlag = angle;
		myWidget.setRotation(angle);
	}

	public void finish() {
		if ((myActivity != null) && !myActivity.isFinishing()) {
			myActivity.finish();
		}
	}

	public void notifyApplicationChanges(boolean singleChange) {
		if (myEventsListener != null) {
			myEventsListener.notifyApplicationChanges(singleChange);
		}
	}

	public void onEpdRepaintFinished() {
		if (myEventsListener != null) {
			myEventsListener.onEpdRepaintFinished();
		}
	}

	public ZLAndroidWidget getWidget() {
		return myWidget;
	}

	public void openInBrowser(String reference) {
		final Intent intent = new Intent(Intent.ACTION_VIEW);
		boolean externalUrl = true;
		if (BookDownloader.acceptsUri(Uri.parse(reference))) {
			intent.setClass(myActivity, BookDownloader.class);
			intent.putExtra(BookDownloaderService.SHOW_NOTIFICATIONS_KEY, BookDownloaderService.Notifications.ALL);
			externalUrl = false;
		}
		final NetworkLibrary nLibrary = NetworkLibrary.Instance();
		try {
			nLibrary.initialize();
		} catch (ZLNetworkException e) {
		}
		reference = NetworkLibrary.Instance().rewriteUrl(reference, externalUrl);
		intent.setData(Uri.parse(reference));
		myActivity.startActivity(intent);
	}

	@Override
	public ZLResourceFile createResourceFile(String path) {
		return new AndroidAssetsFile(path);
	}

	@Override
	public ZLResourceFile createResourceFile(ZLResourceFile parent, String name) {
		return new AndroidAssetsFile((AndroidAssetsFile)parent, name);
	}

	public ZLImage createImage(int drawableId) {
		return new ZLAndroidResourceBasedImageData(myApplication.getResources(), drawableId);
	}

	@Override
	public String getVersionName() {
		try {
			return myApplication.getPackageManager().getPackageInfo(myApplication.getPackageName(), 0).versionName;
		} catch (Exception e) {
			return "";
		}
	}

	@Override
	public int getDisplayDPI() {
		if (myActivity == null) {
			return 0;
		}
		DisplayMetrics metrics = new DisplayMetrics();
		myActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return (int)(160 * metrics.density);
	}

	@Override
	public Collection<String> defaultLanguageCodes() {
		final TreeSet<String> set = new TreeSet<String>();
		set.add(Locale.getDefault().getLanguage());
		final TelephonyManager manager = (TelephonyManager)myApplication.getSystemService(Context.TELEPHONY_SERVICE);
		if (manager != null) {
			final String country0 = manager.getSimCountryIso().toLowerCase();
			final String country1 = manager.getNetworkCountryIso().toLowerCase();
			for (Locale locale : Locale.getAvailableLocales()) {
				final String country = locale.getCountry().toLowerCase();
				if (country != null && country.length() > 0 &&
					(country.equals(country0) || country.equals(country1))) {
					set.add(locale.getLanguage());
				}
			}
			if ("ru".equals(country0) || "ru".equals(country1)) {
				set.add("ru");
			} else if ("by".equals(country0) || "by".equals(country1)) {
				set.add("ru");
			} else if ("ua".equals(country0) || "ua".equals(country1)) {
				set.add("ru");
			}
		}
		set.add("multi");
		return set;
	}

	private final class AndroidAssetsFile extends ZLResourceFile {
		private final AndroidAssetsFile myParent;

		AndroidAssetsFile(AndroidAssetsFile parent, String name) {
			super(parent.getPath().length() == 0 ? name : parent.getPath() + '/' + name);
			myParent = parent;
		}

		AndroidAssetsFile(String path) {
			super(path);
			if (path.length() == 0) {
				myParent = null;
			} else {
				final int index = path.lastIndexOf('/');
				myParent = new AndroidAssetsFile(index >= 0 ? path.substring(0, path.lastIndexOf('/')) : "");
			}
		}

		@Override
		protected List<ZLFile> directoryEntries() {
			try {
				String[] names = myApplication.getAssets().list(getPath());
				if (names != null && names.length != 0) {
					ArrayList<ZLFile> files = new ArrayList<ZLFile>(names.length);
					for (String n : names) {
						files.add(new AndroidAssetsFile(this, n));
					}
					return files;
				}
			} catch (IOException e) {
			}
			return Collections.emptyList();
		}

		@Override
		public boolean isDirectory() {
			try {
				InputStream stream = myApplication.getAssets().open(getPath());
				if (stream == null) {
					return true;
				}
				stream.close();
				return false;
			} catch (IOException e) {
				return true;
			}
		}

		@Override
		public boolean exists() {
			try {
				InputStream stream = myApplication.getAssets().open(getPath());
				if (stream != null) {
					stream.close();
					// file exists
					return true;
				}
			} catch (IOException e) {
			}
			try {
				String[] names = myApplication.getAssets().list(getPath());
				if (names != null && names.length != 0) {
					// directory exists
					return true;
				}
			} catch (IOException e) {
			}
			return false;
		}

		@Override
		public long size() {
			try {
				// TODO: for some files (archives, crt) descriptor cannot be opened
				AssetFileDescriptor descriptor = myApplication.getAssets().openFd(getPath());
				if (descriptor == null) {
					return 0;
				}
				long length = descriptor.getLength();
				descriptor.close();
				return length;
			} catch (IOException e) {
				return 0;
			} 
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return myApplication.getAssets().open(getPath());
		}

		@Override
		public ZLFile getParent() {
			return myParent;
		}
	}
}
