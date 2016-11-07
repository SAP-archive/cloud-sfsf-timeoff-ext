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
package com.sap.cloud.commons.logging;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.ClassicConstants;

@WebFilter(filterName = "AddRequestId", urlPatterns = {"/services/*"}, asyncSupported = true)
public class RequestIdFilter implements Filter {

  private static final String X_REQUEST_ID = "X-Request-ID";
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";
  private static final String USER_AGENT = "User-Agent";
  private static final String REQUEST_ID = "request_id";
  private final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {


    final String id = UUID.randomUUID().toString();

    MDC.put(ClassicConstants.REQUEST_REMOTE_HOST_MDC_KEY, request.getRemoteHost());
    MDC.put(REQUEST_ID, id);
    if (request instanceof HttpServletRequest) {
      final HttpServletRequest httpServletRequest = (HttpServletRequest) request;

      final String requestURI = httpServletRequest.getRequestURI();
      final String method = httpServletRequest.getMethod();
      final String userAgent = httpServletRequest.getHeader(USER_AGENT);
      final String xForwardedFor = httpServletRequest.getHeader(X_FORWARDED_FOR);

      MDC.put(ClassicConstants.REQUEST_REQUEST_URI, requestURI);
      final StringBuffer requestURL = httpServletRequest.getRequestURL();
      if (requestURL != null) {
        MDC.put(ClassicConstants.REQUEST_REQUEST_URL, requestURL.toString());
      }

      MDC.put(ClassicConstants.REQUEST_METHOD, method);
      MDC.put(ClassicConstants.REQUEST_QUERY_STRING, httpServletRequest.getQueryString());
      MDC.put(ClassicConstants.REQUEST_USER_AGENT_MDC_KEY, userAgent);
      MDC.put(ClassicConstants.REQUEST_X_FORWARDED_FOR, xForwardedFor);

      final String message = new StringBuilder().append(method).append(" ").append(requestURI).append(" ")
          .append(userAgent).append(" ").append(xForwardedFor).toString();
      log.debug(message);
    }


    if (response instanceof HttpServletResponse) {
      final HttpServletResponse resp = (HttpServletResponse) response;
      resp.addHeader(X_REQUEST_ID, id);
    }


    try {
      chain.doFilter(request, response);
    } finally {

      MDC.remove(REQUEST_ID);

      MDC.remove(ClassicConstants.REQUEST_REMOTE_HOST_MDC_KEY);
      MDC.remove(ClassicConstants.REQUEST_REQUEST_URI);
      MDC.remove(ClassicConstants.REQUEST_QUERY_STRING);
      MDC.remove(ClassicConstants.REQUEST_REQUEST_URL);
      MDC.remove(ClassicConstants.REQUEST_METHOD);
      MDC.remove(ClassicConstants.REQUEST_USER_AGENT_MDC_KEY);
      MDC.remove(ClassicConstants.REQUEST_X_FORWARDED_FOR);
    }
  }

  @Override
  public void destroy() {

  }

}
