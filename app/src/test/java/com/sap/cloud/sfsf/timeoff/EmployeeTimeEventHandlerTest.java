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
package com.sap.cloud.sfsf.timeoff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import com.sap.cloud.sfsf.notification.EenAlertResponsePayload;
import com.sap.cloud.sfsf.timeoff.CalendarServiceProvider;
import com.sap.cloud.sfsf.timeoff.EmployeeTimeEventHandler;
import com.sap.cloud.sfsf.timeoff.entity.EmpJob;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.UserIdNav;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class EmployeeTimeEventHandlerTest {

  @Mock
  private CalendarServiceProvider calendarSvc;
  String requestId = "requestId";

  private EmployeeTimeEventHandler eventHandler;
  private SFSFEmployeeTime event;

  @Before
  public void setUp() throws Exception {
    eventHandler = new EmployeeTimeEventHandler(Arrays.asList(calendarSvc));
    final UserIdNav userIdNav = new UserIdNav().setJob(new EmpJob().setTimezone("US/Eastern"));
    final OffsetDateTime dummyDate = OffsetDateTime.now();
    event = new SFSFEmployeeTime(dummyDate, dummyDate, userIdNav, dummyDate, dummyDate);

  }

  @Test
  public void testOnCreateEvent() {
    final Observable<SFSFEmployeeTime> eventObs = Observable.just(event);

    given(calendarSvc.create(any(), anyString())).willReturn(eventObs.<Void>map(et -> {
      return null;
    }));// return void observable

    final EenAlertResponsePayload expectedResponse = new EenAlertResponsePayload();
    expectedResponse.setStatus(HttpServletResponse.SC_CREATED);
    expectedResponse.setStatusDetails(
        "[" + requestId + "] " + MessageFormat.format(EmployeeTimeEventHandler.TIMEOFF_EVENT_PROCESSED_OK, "created"));

    final EenAlertResponsePayload response = eventHandler.onCreateEvent(eventObs, requestId).toBlocking().first();

    assertThat(response != null);
    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

    verify(calendarSvc).create(event, requestId);
  }

  @Test
  public void testOnCreateEventFailedWithException() {
    final Observable<SFSFEmployeeTime> eventObs = Observable.just(event);

    given(calendarSvc.create(any(), anyString())).willReturn(Observable.<Void>error(new Exception("test")));

    final EenAlertResponsePayload expectedResponse = new EenAlertResponsePayload();
    expectedResponse.setErrorCode(EmployeeTimeEventHandler.ERROR_STATUS_CODE);
    expectedResponse.setErrorMessage("[" + requestId + "] "
        + MessageFormat.format(EmployeeTimeEventHandler.TIMEOFF_EVENT_PROCESS_ERROR, "creating") + ": test");

    final EenAlertResponsePayload response = eventHandler.onCreateEvent(eventObs, requestId).toBlocking().first();

    assertThat(response != null);
    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

    verify(calendarSvc).create(event, requestId);
  }

  @Test
  public void testOnUpdateEvent() {
    final Observable<SFSFEmployeeTime> eventObs = Observable.just(event);

    given(calendarSvc.update(any(), anyString())).willReturn(eventObs.<Void>map(et -> {
      return null;
    }));// return void observable

    final EenAlertResponsePayload expectedResponse = new EenAlertResponsePayload();
    expectedResponse.setStatus(HttpServletResponse.SC_OK);
    expectedResponse.setStatusDetails(("[" + requestId + "] "
        + MessageFormat.format(EmployeeTimeEventHandler.TIMEOFF_EVENT_PROCESSED_OK, "updated")));

    final EenAlertResponsePayload response = eventHandler.onUpdateEvent(eventObs, requestId).toBlocking().first();

    assertThat(response != null);
    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

    verify(calendarSvc).update(event, requestId);
  }

  @Test
  public void testOnUpdateEventFailedWithException() {
    final Observable<SFSFEmployeeTime> eventObs = Observable.just(event);

    given(calendarSvc.update(any(), anyString())).willReturn(Observable.<Void>error(new Exception("test")));

    final EenAlertResponsePayload expectedResponse = new EenAlertResponsePayload();
    expectedResponse.setErrorCode(EmployeeTimeEventHandler.ERROR_STATUS_CODE);
    expectedResponse.setErrorMessage("[" + requestId + "] "
        + MessageFormat.format(EmployeeTimeEventHandler.TIMEOFF_EVENT_PROCESS_ERROR, "updating") + ": test");

    final EenAlertResponsePayload response = eventHandler.onUpdateEvent(eventObs, requestId).toBlocking().first();

    assertThat(response != null);
    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

    verify(calendarSvc).update(event, requestId);
  }

  @Test
  public void testOnCancelEvent() {
    final Observable<SFSFEmployeeTime> eventObs = Observable.just(event);

    given(calendarSvc.cancel(any(), anyString())).willReturn(eventObs.<Void>map(et -> {
      return null;
    }));// return void observable

    final EenAlertResponsePayload expectedResponse = new EenAlertResponsePayload();
    expectedResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
    expectedResponse.setStatusDetails(
        "[" + requestId + "] " + MessageFormat.format(EmployeeTimeEventHandler.TIMEOFF_EVENT_PROCESSED_OK, "deleted"));

    final EenAlertResponsePayload response = eventHandler.onCancelEvent(eventObs, requestId).toBlocking().first();

    assertThat(response != null);
    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

    verify(calendarSvc).cancel(event, requestId);
  }

  @Test
  public void testOnCancelEventFailedWithException() {
    final Observable<SFSFEmployeeTime> eventObs = Observable.just(event);

    given(calendarSvc.cancel(any(), anyString())).willReturn(Observable.<Void>error(new Exception("test")));

    final EenAlertResponsePayload expectedResponse = new EenAlertResponsePayload();
    expectedResponse.setErrorCode(EmployeeTimeEventHandler.ERROR_STATUS_CODE);
    expectedResponse.setErrorMessage("[" + requestId + "] "
        + MessageFormat.format(EmployeeTimeEventHandler.TIMEOFF_EVENT_PROCESS_ERROR, "deleting") + ": test");

    final EenAlertResponsePayload response = eventHandler.onCancelEvent(eventObs, requestId).toBlocking().first();

    assertThat(response != null);
    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

    verify(calendarSvc).cancel(event, requestId);
  }

}
