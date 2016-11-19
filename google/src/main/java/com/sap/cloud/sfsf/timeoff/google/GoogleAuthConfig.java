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
import java.util.Arrays;
import java.util.Collections;

import javax.naming.ConfigurationException;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;

import com.sap.cloud.commons.connectivity.DestinationUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.ClientTokenServices;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableOAuth2Client
@EnableWebSecurity
@Order(6)
public class GoogleAuthConfig extends WebSecurityConfigurerAdapter {


  private static final String PROPERTY_NOT_FOUND = "Property {0} is not found. Check your destination configuration!";
  private static final String GOOGLE_LOGIN_PATH = "/login/google";
  private static final String CLIENT_SECRET = "client_secret";
  private static final String CLIENT_ID = "client_id";
  private final OAuth2ClientContext oauth2ClientContext;
  private final HttpServletRequest req;
  private final GoogleCredentialService gcs;
  private final Logger logger = LoggerFactory.getLogger(GoogleAuthConfig.class);

  @Autowired
  public GoogleAuthConfig(final OAuth2ClientContext oauth2ClientContext, final HttpServletRequest req,
      final GoogleCredentialService gcs) {
    this.oauth2ClientContext = oauth2ClientContext;
    this.req = req;
    this.gcs = gcs;
  }

  @Override
  protected void configure(final HttpSecurity http) throws Exception {
    // @formatter:off
        http.antMatcher("/**").authorizeRequests().antMatchers("/login**", "/webjars/**").permitAll().anyRequest()
                .authenticated().and()
                .formLogin().loginPage(GOOGLE_LOGIN_PATH)
                .and()
                .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
        // @formatter:on
  }

  @Bean
  public FilterRegistrationBean oauth2ClientFilterRegistration(final OAuth2ClientContextFilter filter) {
    final FilterRegistrationBean registration = new FilterRegistrationBean();
    registration.setFilter(filter);
    registration.setOrder(-100);
    return registration;
  }

  @Bean
  public ClientTokenServices clientTokenServices() {

    return new ClientTokenServices() {

      @Override
      public void saveAccessToken(final OAuth2ProtectedResourceDetails resource, final Authentication authentication,
          final OAuth2AccessToken accessToken) {
        gcs.saveAccessToken(req.getRemoteUser(), accessToken);

      }

      @Override
      public void removeAccessToken(final OAuth2ProtectedResourceDetails resource,
          final Authentication authentication) {
        System.err.println("REMOVE " + req.getRemoteUser());
        gcs.removeAccessToken(req.getRemoteUser());

      }

      @Override
      public OAuth2AccessToken getAccessToken(final OAuth2ProtectedResourceDetails resource,
          final Authentication authentication) {
        return gcs.getAccessToken(req.getRemoteUser()).orElse(null);

      }
    };

  }

  private Filter ssoFilter() {
    final OAuth2ClientAuthenticationProcessingFilter googleFilter =
        new OAuth2ClientAuthenticationProcessingFilter(GOOGLE_LOGIN_PATH);
    final OAuth2RestTemplate googleTemplate = new OAuth2RestTemplate(google(), oauth2ClientContext);

    final AccessTokenProviderChain provider =
        new AccessTokenProviderChain(Arrays.asList(new AuthorizationCodeAccessTokenProvider()));
    provider.setClientTokenServices(clientTokenServices());
    googleTemplate.setAccessTokenProvider(provider);
    googleFilter.setRestTemplate(googleTemplate);
    googleFilter.setTokenServices(
        // new UserInfoTokenServices(googleResource().getUserInfoUri(), google().getClientId())
        new DummyTokenServices());
    return googleFilter;
  }

  @Bean("google-oauth")
  public DestinationUtils googleOAuthConfig() {
    return DestinationUtils.builder().setDestinationName("google_oauth").build();
  }


  @Bean
  public AuthorizationCodeResourceDetails google() {
    final DestinationUtils googleOAuthConfig = googleOAuthConfig();
    final ClientOnlyResourceDetails props = new ClientOnlyResourceDetails();
    final String clientId = getProperty(googleOAuthConfig, CLIENT_ID);
    final String clientSecret = getProperty(googleOAuthConfig, CLIENT_SECRET);

    props.setClientId(clientId);
    props.setClientSecret(clientSecret);
    props.setAccessTokenUri("https://www.googleapis.com/oauth2/v3/token");
    props.setUserAuthorizationUri("https://accounts.google.com/o/oauth2/auth?access_type=offline");
    props.setClientAuthenticationScheme(AuthenticationScheme.form);
    props.setScope(Arrays.asList("https://www.googleapis.com/auth/calendar", "https://www.googleapis.com/auth/plus.me",
        "https://www.googleapis.com/auth/gmail.settings.basic"));

    return props;
  }

  private class ClientOnlyResourceDetails extends AuthorizationCodeResourceDetails {

    @Override
    public boolean isClientOnly() {
      return true;
    }
  }

  private class DummyTokenServices implements ResourceServerTokenServices {

    @Override
    public OAuth2Authentication loadAuthentication(final String accessToken)
        throws AuthenticationException, InvalidTokenException {
      final OAuth2Request request = new OAuth2Request(null, "dummy", null, true, null, null, null, null, null);
      final UsernamePasswordAuthenticationToken token =
          new UsernamePasswordAuthenticationToken(req.getRemoteUser(), "N/A", Collections.emptyList());
      return new OAuth2Authentication(request, token);
    }

    @Override
    public OAuth2AccessToken readAccessToken(final String accessToken) {
      // Not used
      return new DefaultOAuth2AccessToken("123456789");
    }
  }

  private String getProperty(final DestinationUtils destinationConfiguration, final String propertyName) {
    try {
      return destinationConfiguration.getProperty(String.class, propertyName);
    } catch (ConfigurationException | IOException e) {
      final String messsage = MessageFormat.format(PROPERTY_NOT_FOUND, propertyName);
      logger.error(messsage);
      throw new RuntimeException(messsage);
    }
  }
}
