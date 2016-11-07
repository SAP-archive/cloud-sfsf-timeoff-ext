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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;

import com.sap.cloud.sfsf.notification.EenAlertResponsePayload;
import com.sap.cloud.sfsf.notification.Events;
import com.sap.cloud.sfsf.notification.ExternalEvent;
import com.sap.cloud.sfsf.notification.ExternalEventResponse;
import com.sap.cloud.sfsf.notification.endpoint.EventEndpoint;
import com.sap.cloud.sfsf.notification.handler.NotificationHandler;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import rx.Observable;

@RunWith(MockitoJUnitRunner.class)


public class EventEndpointTest {

  private static final String REQUEST_ID = "test-request";

  private EventEndpoint eventEndpointImpl;

  @Mock
  private NotificationHandler handler1;

  @Mock
  private NotificationHandler handler2;

  @Mock
  private EenAlertResponsePayload responseMock;

  @Mock
  private Events eventsMock;

  private static Logger logger;

  @BeforeClass
  public static void beforeClass() {
    logger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    logger.setLevel(Level.OFF);
  }

  @AfterClass
  public static void afterClass() {
    logger.setLevel(Level.ERROR);
  }

  @Before
  public void before() {
    MDC.put("request_id", REQUEST_ID);
  }

  @After
  public void after() {
    MDC.clear();
    verifyNoMoreInteractions(handler1, handler2, responseMock, eventsMock);
  }


  @Test
  public void noHandlersThrowNull() {
    assertThatThrownBy(() ->new EventEndpoint(null)).isInstanceOf(NullPointerException.class).describedAs("It should fail to create a instance with null");
  };

  @Test
  public void oneHandlerFailure() {
    // given
    final EenAlertResponsePayload payload = new EenAlertResponsePayload();
    payload.setErrorCode("500");
    payload.setStatus(500);
    payload.setErrorMessage("random exception\n");
    final ExternalEventResponse expectedResponse = new ExternalEventResponse().setResponsePayload(payload);

    eventEndpointImpl = new EventEndpoint(Arrays.asList(handler1));
    given(handler1.onNotification(eventsMock, REQUEST_ID))
        .willReturn(Observable.error(new Exception("random exception")));

    // when
    final ExternalEventResponse result = eventEndpointImpl.externalEvent(new ExternalEvent().setEvents(eventsMock));

    // then
    assertThat(result).isEqualToComparingFieldByFieldRecursively(expectedResponse);
    verify(handler1).onNotification(eventsMock, REQUEST_ID);
  }

  @Test
  public void failureAndSuccess() {
    // given
    final EenAlertResponsePayload payload = new EenAlertResponsePayload();
    payload.setStatus(200);
    payload.setErrorMessage("random exception\n");
    payload.setStatusDetails("created-test\n");
    final ExternalEventResponse expectedResponse = new ExternalEventResponse().setResponsePayload(payload);

    final EenAlertResponsePayload handler2Response = new EenAlertResponsePayload();
    handler2Response.setStatus(200);
    handler2Response.setStatusDetails("created-test");


    eventEndpointImpl = new EventEndpoint(Arrays.asList(handler1, handler2));
    given(handler1.onNotification(eventsMock, REQUEST_ID))
        .willReturn(Observable.error(new Exception("random exception")));
    given(handler2.onNotification(eventsMock, REQUEST_ID)).willReturn(Observable.just(handler2Response));

    // when
    final ExternalEventResponse result = eventEndpointImpl.externalEvent(new ExternalEvent().setEvents(eventsMock));

    // then
    assertThat(result).isEqualToComparingFieldByFieldRecursively(expectedResponse);
    verify(handler1).onNotification(eventsMock, REQUEST_ID);
    verify(handler2).onNotification(eventsMock, REQUEST_ID);
  }

  @Test
  public void failureAndSuccessEmptyMessages() {
    // given
    final EenAlertResponsePayload payload = new EenAlertResponsePayload();
    payload.setStatus(200);
    payload.setErrorMessage("random exception\n");
    payload.setStatusDetails("created-test\n");
    final ExternalEventResponse expectedResponse = new ExternalEventResponse().setResponsePayload(payload);

    final EenAlertResponsePayload handler1Response = new EenAlertResponsePayload();
    handler1Response.setStatus(500);
    handler1Response.setErrorMessage("random exception");

    final EenAlertResponsePayload handler2Response = new EenAlertResponsePayload();
    handler2Response.setStatus(200);
    handler2Response.setStatusDetails("created-test");

    eventEndpointImpl = new EventEndpoint(Arrays.asList(handler1, handler2));
    given(handler1.onNotification(eventsMock, REQUEST_ID)).willReturn(Observable.just(handler1Response));
    given(handler2.onNotification(eventsMock, REQUEST_ID)).willReturn(Observable.just(handler2Response));

    // when
    final ExternalEventResponse result = eventEndpointImpl.externalEvent(new ExternalEvent().setEvents(eventsMock));

    // then
    assertThat(result).isEqualToComparingFieldByFieldRecursively(expectedResponse);
    verify(handler1).onNotification(eventsMock, REQUEST_ID);
    verify(handler2).onNotification(eventsMock, REQUEST_ID);
  }
}
