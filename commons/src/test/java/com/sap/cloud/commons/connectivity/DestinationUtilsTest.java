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
package com.sap.cloud.commons.connectivity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sap.cloud.commons.connectivity.DestinationUtils;
import com.sap.cloud.commons.connectivity.DestinationUtils.DestinationProperties;
import com.sap.core.connectivity.api.configuration.ConnectivityConfiguration;
import com.sap.core.connectivity.api.configuration.DestinationConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class DestinationUtilsTest {

	private static final String CONNECTIVITY_CFG_LOOKUP_NAME = "java:comp/env/connectivityConfiguration";

	@Mock ConnectivityConfiguration connCfgMock;
	@Mock DestinationConfiguration destCfgMock;
	@Mock static Context ctxMock;

	public static class TestInitialContextFactory implements InitialContextFactory {
		public TestInitialContextFactory() {
		}
	    public Context getInitialContext(Hashtable<?, ?> arg0) throws NamingException {
	        return ctxMock;
	    }
	}

	@Before
	public void setUp() throws Exception {
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestInitialContextFactory.class.getName());
		when(ctxMock.lookup(CONNECTIVITY_CFG_LOOKUP_NAME)).thenReturn(connCfgMock);
	}

	@Test
	public void testWillChainBuild() {
		DestinationUtils du = DestinationUtils.builder().setDestinationName("test").build();

		assertThat(du.getDestinationName(), equalTo("test"));
	}

	@Test
	public void testChainedBuildFailsOnUnsetDestinationName() {
		IllegalStateException expectedException = null;
		try{
			DestinationUtils.builder().build();
		} catch(IllegalStateException e){
			expectedException = e;
		}

		assertNotNull(expectedException);
		assertEquals(expectedException.getMessage(), "The property destinationName value is null. Make sure to invoke setDestinationName(String) with a valid argument before the invocation of the build() method.");
	}

	@Test
	public void testChainedBuildFailsOnSetDestinationNameToNull() {
		IllegalArgumentException expectedException = null;
		try{
			DestinationUtils.builder().setDestinationName(null).build();
		} catch(IllegalArgumentException e){
			expectedException = e;
		}

		assertNotNull(expectedException);
		assertEquals(expectedException.getMessage(), "null is not a valid value for desitnationName");
	}

	@Test
	public void testGetDestinationName() {
		DestinationUtils du = DestinationUtils.builder().setDestinationName("test").build();

		assertEquals(du.getDestinationName(), "test");
	}

	@Test
	public void testGetDefaultConnectivityConfigurationLookupName() {
		DestinationUtils du = DestinationUtils.builder().setDestinationName("test").build();

		assertEquals(du.getConnectivityConfigurationLookupName(), "java:comp/env/connectivityConfiguration");
	}

	@Test
	public void testGetCustomConnectivityConfigurationLookupName() {
		DestinationUtils du = DestinationUtils.builder()
				.setDestinationName("test")
				.setConnectivityConfigurationLookupName("test-name").build();

		assertEquals(du.getConnectivityConfigurationLookupName(), "test-name");
	}

	@Test
	public void testLookupConnectivityConfiguration() {
		ConnectivityConfiguration cfg = null;
		try {
			cfg = DestinationUtils.lookupConnectivityConfiguration(CONNECTIVITY_CFG_LOOKUP_NAME);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertNotNull(cfg);
		assertEquals(cfg, connCfgMock);
	}

	@Test
	public void testLookupConnectivityConfigurationFailed() throws NamingException {
		NamingException ne = new NamingException("naming-exception");
		given(ctxMock.lookup(CONNECTIVITY_CFG_LOOKUP_NAME)).willThrow(ne);

		IOException expectedException = null;
		try {
			DestinationUtils.lookupConnectivityConfiguration(CONNECTIVITY_CFG_LOOKUP_NAME);
		} catch (IOException e) {
			expectedException = e;
		}

		assertNotNull(expectedException);
		assertEquals(expectedException.getMessage(), "naming-exception");
	}

	@Test
	public void testGetDestinationConfiguration() throws NamingException {
		given(connCfgMock.getConfiguration("test-destination")).willReturn(this.destCfgMock);

		DestinationConfiguration result = null;
		try {
			result = DestinationUtils.builder().setDestinationName("test-destination").build().getDestinationConfiguration();
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertNotNull(result);
		assertSame(this.destCfgMock, result);
		verify(ctxMock).lookup(CONNECTIVITY_CFG_LOOKUP_NAME);
	}

	@Test
	public void testGetCachedConnCfg() throws NamingException {
		given(connCfgMock.getConfiguration("test-destination")).willReturn(this.destCfgMock);

		ConnectivityConfiguration result = null;
		try {
			DestinationUtils du = DestinationUtils.builder().setDestinationName("test-destination").build();
			//will lookup and cache
			du.getConnectivityConfiguration();
			result = du.getConnectivityConfiguration();

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertNotNull(result);
		assertSame(this.connCfgMock, result);
		verify(ctxMock, times(1)).lookup(CONNECTIVITY_CFG_LOOKUP_NAME);
	}

	@Test
	public void testGetDestinationProperties() {
		given(this.connCfgMock.getConfiguration("test-destination")).willReturn(this.destCfgMock);

		try {
			DestinationUtils.getDestinationProperties(this.connCfgMock, "test-destination");
		} catch (ConfigurationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		verify(this.destCfgMock).getAllProperties();
	}

	@Test
	public void testGetProperty() {
		given(this.connCfgMock.getConfiguration("test-destination")).willReturn(this.destCfgMock);
		Map<String, String> props = new HashMap<>();
		props.put(DestinationProperties.User.toString(), "test-user");
		props.put(DestinationProperties.URL.toString(), "http://test.org");
		given(this.destCfgMock.getAllProperties()).willReturn(props);

		String user = null;
		URL url = null;
		try {
			DestinationUtils du = DestinationUtils.builder().setDestinationName("test-destination").build();
			user = du.getProperty(String.class, DestinationProperties.User.toString());
			url = du.getProperty(URL.class, DestinationProperties.URL.toString());
		} catch (ConfigurationException | IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals("test-user" ,user);
		assertEquals("http://test.org" ,url.toString());
		verify(this.destCfgMock, times(2)).getAllProperties();
	}

}
