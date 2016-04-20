/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.actions.ldap;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public class TrustManager implements X509TrustManager {
	public void checkClientTrusted(X509Certificate[] cert, String authType) {
		//OK, we trust all
		return;
	}

	public void checkServerTrusted(X509Certificate[] cert, String authType) {
		//OK, we trust all
		return;
	}

	public X509Certificate[] getAcceptedIssuers() {
		//Return empty array
		return new X509Certificate[0];
	}
}