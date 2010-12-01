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

import java.io.File;
import java.lang.reflect.*;

import android.net.Uri;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.*;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.application.ZLAndroidApplicationWindow;

public abstract class ZLAndroidActivity extends Activity {
	protected abstract ZLApplication createApplication(String fileName);

	protected abstract String fileNameForEmptyUri();

	private String fileNameFromUri(Uri uri) {
		if (uri.equals(Uri.parse("file:///"))) {
			return fileNameForEmptyUri();
		} else {
			return uri.getPath();
		}
	}

	private String extractFileNameFromIntent(Intent intent) {
		String fileToOpen = null;
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			final Uri uri = intent.getData();
			if (uri != null) {
				fileToOpen = fileNameFromUri(uri);
                final String scheme = uri.getScheme();
                if ("content".equals(scheme)) {
                    final File file = new File(fileToOpen);
                    if (!file.exists()) {
                        fileToOpen = file.getParent();
                    }
                }
			}
			intent.setData(null);
		}
		return fileToOpen;
	}

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		try {
			final WindowManager.LayoutParams attrs = getWindow().getAttributes();
			final Class<?> cls = attrs.getClass();
			final Field fld = cls.getField("buttonBrightness");
			if (fld != null && "float".equals(fld.getType().toString())) {
				fld.setFloat(attrs, 0);
			}
		} catch (NoSuchFieldException e) {
		} catch (IllegalAccessException e) {
		}
		//getWindow().getAttributes().buttonBrightness = 0;

		setContentView(R.layout.main);
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		((ZLAndroidLibrary)ZLAndroidLibrary.Instance()).setActivity(this);

		final String fileToOpen = extractFileNameFromIntent(getIntent());

		if (((ZLAndroidApplication)getApplication()).myMainWindow == null) {
			ZLApplication application = createApplication(fileToOpen);
			((ZLAndroidApplication)getApplication()).myMainWindow = new ZLAndroidApplicationWindow(application);
			application.initWindow();
		} else if (fileToOpen != null) {
			ZLApplication.Instance().openFile(ZLFile.createFileByPath(fileToOpen));
		}
		//ZLApplication.Instance().repaintView();
	}

	@Override
	public void onStart() {
		super.onStart();
		((ZLAndroidLibrary)ZLAndroidLibrary.Instance()).setActivity(this);
	}

	@Override
	public void onPause() {
		ZLApplication.Instance().onWindowClosing();
		super.onPause();
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		final String fileToOpen = extractFileNameFromIntent(intent);
		if (fileToOpen != null) {
			ZLApplication.Instance().openFile(ZLFile.createFileByPath(fileToOpen));
		}
		// ZLApplication.Instance().repaintView();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		((ZLAndroidApplication)getApplication()).myMainWindow.buildMenu(menu);
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		View view = findViewById(R.id.main_view_epd);
		return ((view != null) && view.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		View view = findViewById(R.id.main_view_epd);
		return ((view != null) && view.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
	}
}
