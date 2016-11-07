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

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import com.sap.cloud.commons.connectivity.DestinationUtils;
import com.sap.core.connectivity.api.authentication.AuthenticationHeader;
import com.sap.core.connectivity.api.configuration.DestinationConfiguration;

public class BasicAuthenticationHeaderProvider {

	class BasicAuthenticationHeader implements AuthenticationHeader {

		static final String BASIC_AUTHENTICATION_PREFIX = "Basic ";
		static final String SEPARATOR = ":";
		static final String AUTHORIZATION_HEADER = "Authorization";

		private final String name;
		private final String value;

		public BasicAuthenticationHeader(String name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public String getValue() {
			return this.value;
		}

	}

	public AuthenticationHeader getBasicAuthenticationHeader(String user, String password) {
		StringBuilder userPass = new StringBuilder();
		userPass.append(user);
		userPass.append(BasicAuthenticationHeader.SEPARATOR);
		userPass.append(password);
		String encodedPassword = DatatypeConverter
				.printBase64Binary(userPass.toString().getBytes(StandardCharsets.UTF_8));
		AuthenticationHeader basicAuthentication = new BasicAuthenticationHeader(BasicAuthenticationHeader.AUTHORIZATION_HEADER,
				BasicAuthenticationHeader.BASIC_AUTHENTICATION_PREFIX + encodedPassword);
		return basicAuthentication;
	}

	public AuthenticationHeader getAuthenticationHeader(DestinationConfiguration destinationConfiguration) {
		Map<String, String> properties = destinationConfiguration.getAllProperties();
		String user = properties.get(DestinationUtils.DestinationProperties.User.toString());
		String password = properties.get(DestinationUtils.DestinationProperties.Password.toString());
		return getBasicAuthenticationHeader(user, password);
	}

}
