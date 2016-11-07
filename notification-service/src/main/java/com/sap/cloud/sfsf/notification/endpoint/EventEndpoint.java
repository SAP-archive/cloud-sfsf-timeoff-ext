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
package com.sap.cloud.sfsf.notification.endpoint;

import java.util.List;

import com.sap.cloud.sfsf.notification.EenAlertResponsePayload;
import com.sap.cloud.sfsf.notification.ExternalEvent;
import com.sap.cloud.sfsf.notification.ExternalEventResponse;
import com.sap.cloud.sfsf.notification.handler.NotificationHandler;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import rx.Observable;
import rx.schedulers.Schedulers;


@Endpoint
public class EventEndpoint {

  private static final String NAMESPACE_URI = "http://notification.event.successfactors.com";

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EventEndpoint.class);

  private final Observable<NotificationHandler> notificationHandlers;

  @Autowired
  public EventEndpoint(final List<NotificationHandler> notificationHandlers) {
    this.notificationHandlers = Observable.from(notificationHandlers);
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "ExternalEvent")
  @ResponsePayload
  public ExternalEventResponse externalEvent(@RequestPayload final ExternalEvent request) {
    final String requestId = MDC.get("request_id");
    final ExternalEventResponse eer = notificationHandlers.flatMap(notification -> {
      return notification.onNotification(request.getEvents(), requestId)
          .doOnError(e -> LOGGER.error("NotificationHandler error while processing event.", e)).onErrorReturn(e -> {
            final EenAlertResponsePayload resp = new EenAlertResponsePayload();
            resp.setErrorCode("500");
            resp.setStatus(500);
            resp.setErrorMessage(e.getMessage());
            return resp;
          });

    }).subscribeOn(Schedulers.io()).reduce(new EenAlertResponsePayload(), (accumulator, current) -> {
      return aggregateResponse(accumulator, current);
    }).map(resp -> new ExternalEventResponse().setResponsePayload(resp)).toBlocking()
        .singleOrDefault(new ExternalEventResponse());
    return eer;
  }

  private EenAlertResponsePayload aggregateResponse(final EenAlertResponsePayload accumulator,
      final EenAlertResponsePayload current) {
    if (!StringUtils.isEmpty(current.getStatusDetails())) {
      final StringBuilder sb =
          new StringBuilder(StringUtils.isEmpty(accumulator.getStatusDetails()) ? "" : accumulator.getStatusDetails());
      sb.append(current.getStatusDetails()).append("\n");
      accumulator.setStatusDetails(sb.toString());
    }
    if (!StringUtils.isEmpty(current.getErrorMessage())) {
      final StringBuilder sb =
          new StringBuilder(StringUtils.isEmpty(accumulator.getErrorMessage()) ? "" : accumulator.getErrorMessage());
      sb.append(current.getErrorMessage()).append("\n");
      accumulator.setErrorMessage(sb.toString());
    }
    accumulator.setErrorCode(current.getErrorCode());
    accumulator.setEntityId(current.getEntityId());
    accumulator.setStatus(current.getStatus());
    accumulator.setStatusDate(current.getStatusDate());
    return accumulator;
  }
}
