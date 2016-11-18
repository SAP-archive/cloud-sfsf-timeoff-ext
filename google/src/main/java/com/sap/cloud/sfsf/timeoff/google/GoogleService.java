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

import java.io.IOException;
import java.text.MessageFormat;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.gmail.Gmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.stereotype.Component;

/**
 * Wrapper for Gmail and Calendar services.
 *
 */
@Component
public class GoogleService {


  private static final String APPLICATION_NAME = "Google Calendar API Java Quickstart 2";

  private final Logger logger = LoggerFactory.getLogger(GoogleService.class);

  private final NetHttpTransport httpTransport;
  private final JsonFactory jsonFactory;
  private final GoogleCredentialService credService;

  @Autowired
  public GoogleService(final NetHttpTransport httpTransport, final JsonFactory jsonFactory,
      final GoogleCredentialService credService) {
    this.httpTransport = httpTransport;
    this.jsonFactory = jsonFactory;
    this.credService = credService;
  }

  public Calendar calendar(final String userId) throws IOException {
    final Credential credential = getCredential(userId);
    logger.debug(MessageFormat.format("Returning new Google Calendar Service for user {0}: ", userId));
    return new Calendar.Builder(httpTransport, jsonFactory, credential).setApplicationName(APPLICATION_NAME).build();
  }

  public Gmail gmail(final String userId) throws IOException {
    final Credential credential = getCredential(userId);
    logger.debug(MessageFormat.format("Returning new Google Gmail Service for user {0}: ", userId));
    return new Gmail.Builder(httpTransport, jsonFactory, credential).setApplicationName(APPLICATION_NAME).build();
  }

  private Credential getCredential(final String userId) {
    final OAuth2AccessToken accessToken = credService.getAccessToken(userId).orElseThrow(() -> {
      final String error = MessageFormat.format("User [{0}] not authenticated with GMail", userId);
      return new UnauthorizedUserException(error);
    });

    return new GoogleCredential().setAccessToken(accessToken.getValue());
        //.setRefreshToken(accessToken.getRefreshToken().getValue())
  }

}
