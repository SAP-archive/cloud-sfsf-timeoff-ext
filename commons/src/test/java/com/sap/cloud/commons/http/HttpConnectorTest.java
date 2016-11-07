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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;

import com.sap.cloud.commons.connectivity.DestinationUtils;
import com.sap.cloud.commons.http.BasicAuthenticationHeaderProvider;
import com.sap.cloud.commons.http.HttpUrlConnectionConnector;
import com.sap.cloud.commons.http.BasicAuthenticationHeaderProvider.BasicAuthenticationHeader;
import com.sap.cloud.commons.http.HttpUrlConnectionConnector.HttpMethod;
import com.sap.core.connectivity.api.authentication.AuthenticationHeader;
import com.sap.core.connectivity.api.configuration.DestinationConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HttpConnectorTest {

	@Mock private DestinationUtils destinationUtils;
	@Mock private DestinationConfiguration destConfig;
	@Mock BasicAuthenticationHeaderProvider headerProvider;

	@Spy HttpUrlConnectionConnector http;

	@Before
	public void setUp() throws Exception {
		//when
		doReturn(destConfig).when(destinationUtils).getDestinationConfiguration();
		http.setAuthenticationHeaderProvider(headerProvider);
		http.setDestinationUtils(destinationUtils);
	}

	@Test
	public void testGet() throws Exception {

		final String responseContent = "test";

		// when
		final HttpURLConnection huc = Mockito.mock(HttpURLConnection.class);
		final InputStream stream = new ByteArrayInputStream(responseContent.getBytes(StandardCharsets.UTF_8));
		when(huc.getInputStream()).thenReturn(stream);
		/*java.net.URL is final so we can't mock it directly. But we can inject a mock connection using its constructor stream handler argument.*/
		final URLStreamHandler stubURLStreamHandler = new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(final URL u) throws IOException {
				return huc;
			}
		};
		final URL url = new URL(null, "http://test.com", stubURLStreamHandler);
		when(huc.getURL()).thenReturn(url);
		doReturn(url).when(http).getRequestURL(anyString());

		final AuthenticationHeader header = headerProvider.new BasicAuthenticationHeader(BasicAuthenticationHeader.AUTHORIZATION_HEADER, BasicAuthenticationHeader.BASIC_AUTHENTICATION_PREFIX + "encodedCredentials");
		doReturn(header).when(headerProvider).getAuthenticationHeader(destConfig);

		// then
		final String actualResponse = http.get("/entityName");

		// verify
		assertNotNull(actualResponse);
		assertTrue(actualResponse.equals(responseContent));

		verify(destinationUtils).getDestinationConfiguration();

		final ArgumentCaptor<DestinationConfiguration> argumentCaptor = ArgumentCaptor.forClass(DestinationConfiguration.class);
		verify(headerProvider).getAuthenticationHeader(argumentCaptor.capture());
		assertThat(argumentCaptor.getValue(), equalTo(destConfig));

		verify(huc).setRequestMethod(eq(HttpMethod.GET.toString()));
		verify(huc).addRequestProperty(eq(HttpUrlConnectionConnector.ACCEPT_HEADER), eq("application/json"));
		verify(huc).addRequestProperty(eq(BasicAuthenticationHeader.AUTHORIZATION_HEADER), eq(header.getValue()));
		verify(huc).getInputStream();

	}

}
