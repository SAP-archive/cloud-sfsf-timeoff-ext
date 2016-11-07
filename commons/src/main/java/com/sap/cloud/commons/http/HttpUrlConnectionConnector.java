/**
 * Copyright 2016 SAP SE
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.sap.cloud.commons.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.sap.cloud.commons.connectivity.DestinationUtils;
import com.sap.core.connectivity.api.authentication.AuthenticationHeader;
import com.sap.core.connectivity.api.configuration.DestinationConfiguration;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUrlConnectionConnector implements HttpConnector{

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpUrlConnectionConnector.class);

	static final String ACCEPT_HEADER = "Accept";
	static final String DESTINATION_URL = "URL";

	enum HttpMethod {
		GET, POST;
	}

	private DestinationUtils destinations;
	private BasicAuthenticationHeaderProvider headerProvider;

	public HttpUrlConnectionConnector() {}

	public HttpUrlConnectionConnector(final DestinationUtils destinations, final BasicAuthenticationHeaderProvider headerProvider) {
		this.destinations = destinations;
		this.headerProvider = headerProvider;
	}

	public static HttpUrlConnectionConnector forDestinationName(final String destinationName){
		final HttpUrlConnectionConnector http = new HttpUrlConnectionConnector();
		http.setAuthenticationHeaderProvider(new BasicAuthenticationHeaderProvider());
		final DestinationUtils destUtils = DestinationUtils.builder().setDestinationName(destinationName).build();
		http.setDestinationUtils(destUtils);
		LOGGER.debug("HttpUrlConnectionConnector initialized for destination {}", destinationName);
		return http;
	}

	@Override
	public String get(final String url) throws IOException {
		final DestinationConfiguration destinationConfiguration = destinations.getDestinationConfiguration();
		URL requestURL;
		try {
			requestURL = getRequestURL(url);
		} catch (final ConfigurationException e) {
			throw new IOException(e);
		}
		final HttpURLConnection urlConnection = (HttpURLConnection) requestURL.openConnection();
		injectAuthenticationHeaders(urlConnection, destinationConfiguration);
		return execute(urlConnection, HttpMethod.GET);
	}

	@Override
	public void post(final String url, final String payload) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	private String execute(final HttpURLConnection connection, final HttpMethod method) throws IOException {
		connection.setRequestMethod(method.toString());

		final String requestLine = connection.getRequestMethod() + " " + connection.getURL().toString() + "\r\n";
		final String requestHeaders = listHeaders(connection.getRequestProperties());
		LOGGER.debug("--> " + requestLine + requestHeaders);

		String content = null;
		try{

			final InputStream stream = connection.getInputStream();
			content = IOUtils.toString(stream);

		} catch(final Throwable t){
			final InputStream errorDetails = connection.getErrorStream();
			LOGGER.error("Server error response is: {}", IOUtils.toString(errorDetails));
			throw t;
		} finally {
			final String responseHeaders = listHeaders(connection.getHeaderFields());
			final String responseStatusLine = getStatusLine(connection) + "\r\n";
			LOGGER.debug("<-- " + responseStatusLine + responseHeaders);
		}
		return content;
	}

	private void injectAuthenticationHeaders(final HttpURLConnection urlConnection, final DestinationConfiguration destinationConfiguration) {
		urlConnection.addRequestProperty(ACCEPT_HEADER, "application/json");
		final List<AuthenticationHeader> authenticationHeaders = getAuthenticationHeaders(destinationConfiguration);
		for (final AuthenticationHeader authenticationHeader : authenticationHeaders) {
			urlConnection.addRequestProperty(authenticationHeader.getName(), authenticationHeader.getValue());
		}
	}

	private List<AuthenticationHeader> getAuthenticationHeaders(final DestinationConfiguration destinationConfiguration) {
		final List<AuthenticationHeader> authenticationHeaders = new ArrayList<>();
		authenticationHeaders.add(headerProvider.getAuthenticationHeader(destinationConfiguration));
		return authenticationHeaders;
	}

	URL getRequestURL(final String url) throws IOException, ConfigurationException {
		if(!url.startsWith("/")) {
      return new URL(url);
    }

		final String requestBaseURL = destinations.getProperty(String.class, DESTINATION_URL);
		if (StringUtils.isEmpty(requestBaseURL)) {
			final String errorMessage = String.format(
					"Request URL in Destination %s is not configured. Make sure to have the destination configured.",
					destinations.getDestinationName());
			throw new ConfigurationException(errorMessage);
		}

		final URL fullURL = new URL(requestBaseURL + url);
		return fullURL;
	}

	public void setDestinationUtils(final DestinationUtils destinations){
		this.destinations = destinations;
	}

	public void setAuthenticationHeaderProvider(final BasicAuthenticationHeaderProvider provider){
		headerProvider = provider;
	}

	private String listHeaders(final Map<String, List<String>> headers){
		final StringBuilder headersList = new StringBuilder();
		for (final Map.Entry<String, List<String>> header : headers.entrySet()) {
			if(header.getKey()==null || header.getValue()==null) {
        continue;
      }
			headersList.append(header.getKey()).append(":").append(header.getValue().toString()).append("\r\n");
		}
		return headersList.toString();
	}

	private String getStatusLine(final HttpURLConnection connection){
		String responseStatusLine = null;
		if(connection.getHeaderFields()!=null){
			if(connection.getHeaderFields().get(null)!=null){
				final List<String> statusLineList = connection.getHeaderFields().get(null);
				if(statusLineList.size()>0) {
          responseStatusLine = statusLineList.get(0);
        }
			}
		}
		if(responseStatusLine==null){
			try {
				responseStatusLine = "HTTP 1.0 " + connection.getResponseCode() + " " + connection.getResponseMessage();
			} catch (final IOException e) {
				responseStatusLine  = "Could not retrieve status line from connection reponse";
			}
		}
		return responseStatusLine;
	}

}
