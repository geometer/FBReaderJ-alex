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

package org.geometerplus.android.fbreader.network;

import java.util.*;
import java.io.*;
import java.net.URLConnection;

import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.net.Uri;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.network.*;

import org.geometerplus.fbreader.network.BookReference;


public class BookDownloaderService extends Service {

	public static final String BOOK_FORMAT_KEY = "org.geometerplus.android.fbreader.network.BookFormat";
	public static final String REFERENCE_TYPE_KEY = "org.geometerplus.android.fbreader.network.ReferenceType";
	public static final String CLEAN_URL_KEY = "org.geometerplus.android.fbreader.network.CleanURL";
	public static final String TITLE_KEY = "org.geometerplus.android.fbreader.network.Title";
	public static final String SSL_CERTIFICATE_KEY = "org.geometerplus.android.fbreader.network.SSLCertificate";

	public static final String SHOW_NOTIFICATIONS_KEY = "org.geometerplus.android.fbreader.network.ShowNotifications";

	public interface Notifications {
		int DOWNLOADING_STARTED = 0x0001;
		int ALREADY_DOWNLOADING = 0x0002;

		int ALL = 0x0003;
	}


	private Set<String> myDownloadingURLs = Collections.synchronizedSet(new HashSet<String>());
	private Set<Integer> myOngoingNotifications = new HashSet<Integer>();

	private volatile int myServiceCounter;

	private void doStart() {
		++myServiceCounter;
	}

	private void doStop() {
		if (--myServiceCounter == 0) {
			stopSelf();
		}
	}

	public static ZLResource getResource() {
		return ZLResource.resource("bookDownloader");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new BookDownloaderInterface.Stub() {
			public boolean isBeingDownloaded(String url) {
				return myDownloadingURLs.contains(url);
			}
		};
	}

	@Override
	public void onDestroy() {
		final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		for (int notificationId: myOngoingNotifications) {
			notificationManager.cancel(notificationId);
		}
		myOngoingNotifications.clear();
		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		doStart();

		final Uri uri = intent.getData();
		if (uri == null) {
			doStop();
			return;
		}
		intent.setData(null);

		final int notifications = intent.getIntExtra(SHOW_NOTIFICATIONS_KEY, 0);

		final String url = uri.toString();
		final int bookFormat = intent.getIntExtra(BOOK_FORMAT_KEY, BookReference.Format.NONE);
		final int referenceType = intent.getIntExtra(REFERENCE_TYPE_KEY, BookReference.Type.UNKNOWN);
		String cleanURL = intent.getStringExtra(CLEAN_URL_KEY);
		if (cleanURL == null) {
			cleanURL = url;
		}

		if (myDownloadingURLs.contains(url)) {
			if ((notifications & Notifications.ALREADY_DOWNLOADING) != 0) {
				Toast.makeText(
					getApplicationContext(),
					getResource().getResource("alreadyDownloading").getValue(),
					Toast.LENGTH_SHORT
				).show();
			}
			doStop();
			return;
		}

		String fileName = BookReference.makeBookFileName(cleanURL, bookFormat, referenceType);
		if (fileName == null) {
			doStop();
			return;
		}

		int index = fileName.lastIndexOf(File.separator);
		if (index != -1) {
			final String dir = fileName.substring(0, index);
			final File dirFile = new File(dir);
			if (!dirFile.exists() && !dirFile.mkdirs()) {
				// TODO: error message
				doStop();
				return;
			}
			if (!dirFile.exists() || !dirFile.isDirectory()) {
				// TODO: error message
				doStop();
				return;
			}
		}

		final File fileFile = new File(fileName);
		if (fileFile.exists()) {
			if (!fileFile.isFile()) {
				// TODO: error message
				doStop();
				return;
			}
			// TODO: question box: redownload?
			/*
			ZLDialogManager.Instance().showQuestionBox(
				"redownloadBox", "Redownload?",
				"no", null,
				"yes", null,
				null, null
			);
			*/
			doStop();
			startActivity(getFBReaderIntent(fileFile));
			return;
		}
		String title = intent.getStringExtra(TITLE_KEY);
		if (title == null || title.length() == 0) {
			title = fileFile.getName();
		}
		if ((notifications & Notifications.DOWNLOADING_STARTED) != 0) {
			Toast.makeText(
				getApplicationContext(),
				getResource().getResource("downloadingStarted").getValue(),
				Toast.LENGTH_SHORT
			).show();
		}
		final String sslCertificate = intent.getStringExtra(SSL_CERTIFICATE_KEY);
		startFileDownload(url, sslCertificate, fileFile, title);
	}

	private Intent getFBReaderIntent(final File file) {
		final Intent intent = new Intent(getApplicationContext(), org.geometerplus.android.fbreader.FBReader.class);
		if (file != null) {
			intent.setAction(Intent.ACTION_VIEW).setData(Uri.fromFile(file));
		}
		return intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
	}

	private Notification createDownloadFinishNotification(File file, String title, boolean success) {
		final ZLResource resource = getResource();
		final String tickerText = success ?
			resource.getResource("tickerSuccess").getValue() :
			resource.getResource("tickerError").getValue();
		final String contentText = success ?
			resource.getResource("contentSuccess").getValue() :
			resource.getResource("contentError").getValue();
		final Notification notification = new Notification(
			android.R.drawable.stat_sys_download_done,
			tickerText,
			System.currentTimeMillis()
		);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		final Intent intent = success ? getFBReaderIntent(file) : new Intent();
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(getApplicationContext(), title, contentText, contentIntent);
		return notification;
	}

	private Notification createDownloadProgressNotification(String title) {
		final RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.download_notification);
		contentView.setTextViewText(R.id.download_notification_title, title);
		contentView.setTextViewText(R.id.download_notification_progress_text, "");
		contentView.setProgressBar(R.id.download_notification_progress_bar, 100, 0, true);

		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(), 0);

		final Notification notification = new Notification();
		notification.icon = android.R.drawable.stat_sys_download;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.contentView = contentView;
		notification.contentIntent = contentIntent;

		return notification;
	}

	private void startCallbackActivity() {
		startActivity(
			new Intent(getApplicationContext(), BookDownloaderCallback.class)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		);
	}

	private void startFileDownload(final String urlString, final String sslCertificate, final File file, final String title) {
		myDownloadingURLs.add(urlString);
		startCallbackActivity();

		final int notificationId = NetworkNotifications.Instance().getBookDownloadingId();
		final Notification progressNotification = createDownloadProgressNotification(title);

		final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		myOngoingNotifications.add(Integer.valueOf(notificationId));
		notificationManager.notify(notificationId, progressNotification);

		final Handler progressHandler = new Handler() {
			public void handleMessage(Message message) {
				final int progress = message.what;
				final RemoteViews contentView = (RemoteViews)progressNotification.contentView;

				if (progress < 0) {
					contentView.setTextViewText(R.id.download_notification_progress_text, "");
					contentView.setProgressBar(R.id.download_notification_progress_bar, 100, 0, true);
				} else {
					contentView.setTextViewText(R.id.download_notification_progress_text, "" + progress + "%");
					contentView.setProgressBar(R.id.download_notification_progress_bar, 100, progress, false);
				}
				final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				notificationManager.notify(notificationId, progressNotification);
			}
		};

		final Handler downloadFinishHandler = new Handler() {
			public void handleMessage(Message message) {
				myDownloadingURLs.remove(urlString);
				final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				notificationManager.cancel(notificationId);
				myOngoingNotifications.remove(Integer.valueOf(notificationId));
				notificationManager.notify(
					notificationId,
					createDownloadFinishNotification(file, title, message.what != 0)
				);
				startCallbackActivity();
				doStop();
			}
		};

		final ZLNetworkRequest request = new ZLNetworkRequest(urlString, sslCertificate) {

			public String handleStream(URLConnection connection, InputStream inputStream) throws IOException {
				final int updateIntervalMillis = 1000; // FIXME: remove hardcoded time constant

				final int fileLength = connection.getContentLength();
				int downloadedPart = 0;
				long progressTime = System.currentTimeMillis() + updateIntervalMillis;
				if (fileLength <= 0) {
					progressHandler.sendEmptyMessage(-1);
				}
				OutputStream outStream;
				try {
					outStream = new FileOutputStream(file);
				} catch (FileNotFoundException ex) {
					return ZLNetworkErrors.errorMessage(ZLNetworkErrors.ERROR_CREATE_FILE, file.getPath());
				}
				try {
					final byte[] buffer = new byte[8192];
					while (true) {
						final int size = inputStream.read(buffer);
						if (size <= 0) {
							break;
						}
						downloadedPart += size;
						if (fileLength > 0) {
							final long currentTime = System.currentTimeMillis();
							if (currentTime > progressTime) {
								progressTime = currentTime + updateIntervalMillis;
								progressHandler.sendEmptyMessage(downloadedPart * 100 / fileLength);
							}
							/*if (downloadedPart * 100 / fileLength > 95) {
								throw new IOException("debug exception");
							}*/
						}
						outStream.write(buffer, 0, size);
						/*try {
							Thread.currentThread().sleep(200);
						} catch (InterruptedException ex) {
						}*/
					}
				} finally {
					outStream.close();
				}
				return null;
			}
		};

		final Thread downloader = new Thread(new Runnable() {
			public void run() {
				final String err = ZLNetworkManager.Instance().perform(request);
				// TODO: show error message to User
				final boolean success = (err == null);
				if (!success) {
					file.delete();
				}
				downloadFinishHandler.sendEmptyMessage(success ? 1 : 0);
			}
		});
		downloader.setPriority(Thread.MIN_PRIORITY);
		downloader.start();
	}
}
