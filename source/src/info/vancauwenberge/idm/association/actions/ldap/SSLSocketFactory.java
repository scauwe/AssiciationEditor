/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.actions.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

public class SSLSocketFactory extends javax.net.ssl.SSLSocketFactory {
	private SSLSocketFactory factory;

	public SSLSocketFactory() {
		try {
			SSLContext sslcontext = SSLContext.getInstance("TLS");
			sslcontext.init(
					null, // No KeyManager required
					new TrustManager[] { new TrustManager() },//Our own trustmanager
					new java.security.SecureRandom());
			factory = (SSLSocketFactory) sslcontext.getSocketFactory();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static SocketFactory getDefault() {
		return new SSLSocketFactory();
	}

	public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException {
		return factory.createSocket(socket, s, i, flag);
	}

	public Socket createSocket(InetAddress inaddr, int i, InetAddress inaddr1, int j) throws IOException {
		return factory.createSocket(inaddr, i, inaddr1, j);
	}

	public Socket createSocket(InetAddress inaddr, int i) throws IOException {
		return factory.createSocket(inaddr, i);
	}

	public Socket createSocket(String s, int i, InetAddress inaddr, int j) throws IOException {
		return factory.createSocket(s, i, inaddr, j);
	}

	public Socket createSocket(String s, int i) throws IOException {
		return factory.createSocket(s, i);
	}

	public String[] getDefaultCipherSuites() {
		return factory.getSupportedCipherSuites();
	}

	public String[] getSupportedCipherSuites() {
		return factory.getSupportedCipherSuites();
	}
}
