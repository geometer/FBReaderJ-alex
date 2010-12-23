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

package org.geometerplus.fbreader.network.opds;

import java.util.*;

import org.geometerplus.zlibrary.core.network.ZLNetworkManager;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest;

import org.geometerplus.fbreader.network.*;

class OPDSCatalogItem extends NetworkCatalogItem {
	static class State extends NetworkOperationData {
		public String LastLoadedId;
		public final HashSet<String> LoadedIds = new HashSet<String>();

		public State(INetworkLink link, OnNewItemListener listener) {
			super(link, listener);
		}
	}
	private State myLoadingState;
	private final Map<String,String> myExtraData;

	OPDSCatalogItem(INetworkLink link, String title, String summary, String cover, Map<Integer, String> urlByType, Map<String,String> extraData) {
		super(link, title, summary, cover, urlByType);
		myExtraData = extraData;
	}

	OPDSCatalogItem(INetworkLink link, String title, String summary, String cover, Map<Integer, String> urlByType, int visibility, int catalogType) {
		super(link, title, summary, cover, urlByType, visibility, catalogType);
		myExtraData = null;
	}

	private void doLoadChildren(NetworkOperationData.OnNewItemListener listener,
			ZLNetworkRequest networkRequest) throws ZLNetworkException {
		while (networkRequest != null) {
			try {
				ZLNetworkManager.Instance().perform(networkRequest);
			} catch (ZLNetworkException e) {
				myLoadingState = null;
				throw e;
			}
			if (listener.confirmInterrupt()) {
				return;
			}
			networkRequest = myLoadingState.resume();
		}
	}

	@Override
	public final Map<String,String> extraData() {
		return myExtraData;
	}

	@Override
	public final void loadChildren(NetworkOperationData.OnNewItemListener listener) throws ZLNetworkException {
		OPDSNetworkLink opdsLink = (OPDSNetworkLink) Link;

		myLoadingState = opdsLink.createOperationData(Link, listener);

		ZLNetworkRequest networkRequest =
			opdsLink.createNetworkData(URLByType.get(URL_CATALOG), myLoadingState);

		doLoadChildren(listener, networkRequest);
	}

	@Override
	public final boolean supportsResumeLoading() {
		return true;
	}

	@Override
	public final void resumeLoading(NetworkOperationData.OnNewItemListener listener) throws ZLNetworkException {
		if (myLoadingState != null) {
			myLoadingState.Listener = listener;
			ZLNetworkRequest networkRequest = myLoadingState.resume();
			doLoadChildren(listener, networkRequest);
		}
	}
}
