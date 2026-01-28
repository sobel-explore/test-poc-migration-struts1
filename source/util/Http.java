//  Copyright (C) 2012  Markus Fischer
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
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import enums.Connect;

public class Http {

    private static final Logger LOG = LoggerFactory.getLogger(Http.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0";

    /**
     * @param link
     * @return web site as String with a timeout of 2 seconds and 3 tries.
     */
    public String getContent(final String link) {
        return getContent(link, Connect.TIMEOUT_2.getValue(), Connect.TRIES_3.getValue(), null);
    }

    /**
     * @param link
     * @return web site as String with a timeout of 2 seconds and 3 tries.
     */
    public String getContent(final String link, final List<NameValuePair> nameValuePairs) {
        return getContent(link, nameValuePairs, Connect.TIMEOUT_2.getValue(), Connect.TRIES_3.getValue(), null);
    }

    /**
     * @param String link
     * @param int timeout_ms
     * @return web site as string
     */
    public String getContent(final String link, final int timeoutMs, final int trys, final String encoding) {

        String content = "";
        HttpEntity entity = null;

        final HttpGet httpget = new HttpGet(link);

        final HttpParams httpParameters = new BasicHttpParams();
        // Set the timeout in milliseconds until a connection is established.
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutMs);
        // Set the default socket timeout in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutMs);
        // Set user agent
        httpParameters.setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);

        final HttpClient httpclient = new DefaultHttpClient(httpParameters);

        int runs = 1;

        while (entity == null && runs <= trys) {

            runs++;

            try {

                // Execute the request
                final HttpResponse response = httpclient.execute(httpget);

                // Get hold of the response entity
                entity = response.getEntity();

                if (entity != null) {
                    if (encoding == null) {
                        // let httpclient guess encoding
                        content = EntityUtils.toString(entity);
                    } else {
                        // use predefined encoding
                        content = EntityUtils.toString(entity, encoding);
                    }
                }

            } catch (final IOException e) {
                LOG.error(e.toString() + ": " + link);
            } catch (final Exception e) {
                LOG.error(e.toString() + ": " + link);
            } finally {
                EntityUtils.consumeQuietly(entity);
            }

        }

        return content;
    }

    /**
     * @param String link
     * @param List<NameValuePair> nameValuePairs
     * @param int timeout_ms
     * @return web site as string
     */
    public String getContent(final String link, final List<NameValuePair> nameValuePairs, final int timeoutMs,
            final int trys, final String encoding) {

        String content = "";
        HttpEntity entity = null;

        try {

            final HttpPost httppost = new HttpPost(link);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "utf-8"));

            final HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutMs);
            // Set the default socket timeout in milliseconds which is the timeout for waiting for data.
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutMs);
            // Set user agent
            httpParameters.setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);

            final HttpClient httpclient = new DefaultHttpClient(httpParameters);

            int runs = 1;

            while (entity == null && runs <= trys) {

                runs++;

                try {

                    // Execute the request
                    final HttpResponse response = httpclient.execute(httppost);

                    // Get hold of the response entity
                    entity = response.getEntity();

                    if (entity != null) {
                        if (encoding == null) {
                            // let httpclient guess encoding
                            content = EntityUtils.toString(entity);
                        } else {
                            // use predefined encoding
                            content = EntityUtils.toString(entity, encoding);
                        }
                    }

                } catch (final ClientProtocolException e) {
                    LOG.error(e.toString() + ": " + link);
                } catch (final IOException e) {
                    LOG.error(e.toString() + ": " + link);
                } catch (final Exception e) {
                    LOG.error(e.toString() + ": " + link);
                } finally {
                    EntityUtils.consumeQuietly(entity);
                }

            }

        } catch (final UnsupportedEncodingException e) {
            LOG.error(e.toString());
        }

        return content;
    }

}
