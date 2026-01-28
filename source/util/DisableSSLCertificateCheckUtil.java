//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
//
//  Contact: info@doctor-doc.com

package util;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class DisableSSLCertificateCheckUtil {
    /**
     * Prevent instantiation of utility class.
     */
    private DisableSSLCertificateCheckUtil() {

    }

    /**
     * Trust manager that does not perform nay checks.
     */
    private static class NullX509TrustManager implements X509TrustManager {
        public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
        }

        public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

    }

    /**
     * Host name verifier that does not perform nay checks.
     */
    private static class NullHostnameVerifier implements HostnameVerifier {
        public boolean verify(final String hostname, final SSLSession session) {
            return true;
        }
    }

    /**
     * Disable trust checks for SSL connections.
     */
    public static void disableChecks() throws NoSuchAlgorithmException, KeyManagementException {

        try {
            new URL("https://0.0.0.0/").getContent();
            // CHECKSTYLE:OFF
        } catch (final IOException e) {
            // This invocation will always fail, but it will register the
            // default SSL provider to the URL class.
        }
        // CHECKSTYLE:ON

        final SSLContext context = SSLContext.getInstance("SSLv3");
        final TrustManager[] trustManagerArray = { new NullX509TrustManager() };
        context.init(null, trustManagerArray, null);

        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new NullHostnameVerifier());
    }
}
