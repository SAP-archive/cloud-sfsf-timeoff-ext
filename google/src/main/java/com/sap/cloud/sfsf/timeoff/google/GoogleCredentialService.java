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
package com.sap.cloud.sfsf.timeoff.google;

import java.util.Optional;

import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;

/**
 * In-memory storage for tokens.
 *
 * Replace with persistent storage for production.
 *
 */
@Service
public class GoogleCredentialService {

  private final ConcurrentHashMap<String, OAuth2AccessToken> tokens = new ConcurrentHashMap<>();

  public void saveAccessToken(final String user, final OAuth2AccessToken accessToken) {
    tokens.put(user, accessToken);

  }

  public void removeAccessToken(final String user) {
    tokens.remove(user);

  }

  public Optional<OAuth2AccessToken> getAccessToken(final String user) {
    return Optional.ofNullable(tokens.get(user));

  }
}
