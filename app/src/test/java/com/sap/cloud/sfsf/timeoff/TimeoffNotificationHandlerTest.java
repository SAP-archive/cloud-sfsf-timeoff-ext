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
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.OffsetDateTime;

import javax.servlet.http.HttpServletResponse;

import com.sap.cloud.sfsf.notification.EenAlertRequestData;
import com.sap.cloud.sfsf.notification.EenAlertResponsePayload;
import com.sap.cloud.sfsf.notification.Events;
import com.sap.cloud.sfsf.notification.Param;
import com.sap.cloud.sfsf.notification.EenAlertRequestData.EntityKeys;
import com.sap.cloud.sfsf.timeoff.EmployeeTimeEventHandler;
import com.sap.cloud.sfsf.timeoff.SFSFEmployeeTimeService;
import com.sap.cloud.sfsf.timeoff.TimeoffNotificationHandler;
import com.sap.cloud.sfsf.timeoff.entity.EmpJob;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTimeList;
import com.sap.cloud.sfsf.timeoff.entity.UserIdNav;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime.ApprovalStatus;

import org.ehcache.Cache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class TimeoffNotificationHandlerTest {

  private static final String TEST_REQUEST_ID = "test-request-id";

  private static final String EXTERNAL_CODE = "123456789";

  private static final String ERROR_MSG_PREFIX =
      "[" + TEST_REQUEST_ID + "] " + TimeoffNotificationHandler.ERROR_MESSAGE + ": ";

  @Mock
  private EmployeeTimeEventHandler employeeTimeEventHandlerMock;

  @Mock
  private SFSFEmployeeTimeService timeOffClientMock;

  @Mock
  private Cache<String, SFSFEmployeeTimeList> cacheMock;

  private TimeoffNotificationHandler handler;
  private EenAlertResponsePayload expectedResponse;

  private UserIdNav userIdNav;

  private OffsetDateTime dummyDate;

  @Before
  public void before() throws Exception {
    handler = new TimeoffNotificationHandler(employeeTimeEventHandlerMock, timeOffClientMock, cacheMock);
    expectedResponse = new EenAlertResponsePayload();
    userIdNav = new UserIdNav().setEmail("test-email").setJob(new EmpJob().setTimezone("US/Eastern"));
    dummyDate = OffsetDateTime.now();
  }

  @After
  public void after() {
    verifyNoMoreInteractions(employeeTimeEventHandlerMock, timeOffClientMock, cacheMock);
  }

  @Test
  public void testWillDispatchCreateEvent() throws Exception {

    expectedResponse.setStatus(HttpServletResponse.SC_CREATED);
    expectedResponse.setStatusDetails("[" + TEST_REQUEST_ID + "] "
        + MessageFormat.format(EmployeeTimeEventHandler.TIMEOFF_EVENT_PROCESSED_OK, "created"));
    given(employeeTimeEventHandlerMock.onCreateEvent(any(), anyString())).willReturn(Observable.just(expectedResponse));


    final OffsetDateTime createdDate = OffsetDateTime.parse("2007-12-03T10:15:30+01:00");
    final OffsetDateTime lastModifiedDate = OffsetDateTime.parse("2007-12-03T10:15:30+01:00");
    final SFSFEmployeeTime sfsfEmployeeTime =
        new SFSFEmployeeTime(dummyDate, dummyDate, userIdNav, createdDate, lastModifiedDate);
    sfsfEmployeeTime.setApprovalStatus(ApprovalStatus.APPROVED);

    sfsfEmployeeTime.setUserId("user-1");
    given(timeOffClientMock.getTimeoffEvent(EXTERNAL_CODE)).willReturn(sfsfEmployeeTime);

    final SFSFEmployeeTimeList sfsfEmployeeTimeList = new SFSFEmployeeTimeList();
    given(cacheMock.get("user-1")).willReturn(sfsfEmployeeTimeList);

    final Param param = new Param();
    param.setName("externalCode");
    param.setValue(EXTERNAL_CODE);
    final Events events = getEvent(param);

    final EenAlertResponsePayload response = handler.onNotification(events, TEST_REQUEST_ID).toBlocking().first();

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);
    assertThat(sfsfEmployeeTimeList.getResults()).hasSize(1).containsOnly(sfsfEmployeeTime);

    verify(timeOffClientMock).getTimeoffEvent(anyString());
    verify(employeeTimeEventHandlerMock).onCreateEvent(any(), anyString());
    verify(cacheMock).get("user-1");
    verify(cacheMock).put("user-1", sfsfEmployeeTimeList);
  }

  @Test
  public void testWillDispatchUpdateEvent() throws Exception {

    expectedResponse.setStatus(HttpServletResponse.SC_OK);
    expectedResponse.setStatusDetails("[" + TEST_REQUEST_ID + "] "
        + MessageFormat.format(EmployeeTimeEventHandler.TIMEOFF_EVENT_PROCESSED_OK, "updated"));
    given(employeeTimeEventHandlerMock.onUpdateEvent(any(), anyString())).willReturn(Observable.just(expectedResponse));

    final OffsetDateTime createdDate = OffsetDateTime.parse("2007-12-03T10:15:30+01:00");
    final OffsetDateTime lastModified = OffsetDateTime.parse("2008-12-03T10:15:30+01:00");

    final SFSFEmployeeTime sfsfEmployeeTime =
        new SFSFEmployeeTime(dummyDate, dummyDate, userIdNav, createdDate, lastModified);
    sfsfEmployeeTime.setApprovalStatus(ApprovalStatus.APPROVED);

    sfsfEmployeeTime.setUserId("user-1");
    given(timeOffClientMock.getTimeoffEvent(EXTERNAL_CODE)).willReturn(sfsfEmployeeTime);

    final SFSFEmployeeTimeList sfsfEmployeeTimeList = new SFSFEmployeeTimeList();
    sfsfEmployeeTimeList.getResults().add(sfsfEmployeeTime);
    given(cacheMock.get("user-1")).willReturn(sfsfEmployeeTimeList);

    final Param param = new Param();
    param.setName("externalCode");
    param.setValue(EXTERNAL_CODE);
    final Events events = getEvent(param);

    final EenAlertResponsePayload response = handler.onNotification(events, TEST_REQUEST_ID).toBlocking().first();

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);
    assertThat(sfsfEmployeeTimeList.getResults()).hasSize(1).containsOnly(sfsfEmployeeTime);

    verify(timeOffClientMock).getTimeoffEvent(anyString());
    verify(employeeTimeEventHandlerMock).onUpdateEvent(any(), anyString());
    verify(cacheMock).get("user-1");
    verify(cacheMock).put("user-1", sfsfEmployeeTimeList);
  }

  @Test
  public void testWillDispatchCancelEvent() throws Exception {

    expectedResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
    expectedResponse.setStatusDetails("[" + TEST_REQUEST_ID + "] "
        + MessageFormat.format(EmployeeTimeEventHandler.TIMEOFF_EVENT_PROCESSED_OK, "deleted"));
    given(employeeTimeEventHandlerMock.onCancelEvent(any(), anyString())).willReturn(Observable.just(expectedResponse));

    final SFSFEmployeeTime sfsfEmployeeTime =
        new SFSFEmployeeTime(dummyDate, dummyDate, userIdNav, dummyDate, dummyDate);
    sfsfEmployeeTime.setApprovalStatus(ApprovalStatus.CANCELLED);
    sfsfEmployeeTime.setUserId("user-1");
    given(timeOffClientMock.getTimeoffEvent(EXTERNAL_CODE)).willReturn(sfsfEmployeeTime);

    final SFSFEmployeeTimeList sfsfEmployeeTimeList = new SFSFEmployeeTimeList();
    sfsfEmployeeTimeList.getResults().add(sfsfEmployeeTime);
    given(cacheMock.get("user-1")).willReturn(sfsfEmployeeTimeList);

    final Param param = new Param();
    param.setName("externalCode");
    param.setValue(EXTERNAL_CODE);
    final Events events = getEvent(param);

    final EenAlertResponsePayload response = handler.onNotification(events, TEST_REQUEST_ID).toBlocking().first();

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);
    assertThat(sfsfEmployeeTimeList.getResults()).hasSize(0);

    verify(timeOffClientMock).getTimeoffEvent(anyString());
    verify(employeeTimeEventHandlerMock).onCancelEvent(any(), anyString());
    verify(cacheMock).get("user-1");
    verify(cacheMock).replace("user-1", sfsfEmployeeTimeList);
  }

  @Test
  public void testFailOnNoExternalCode() throws Exception {

    expectedResponse.setErrorCode("500");
    expectedResponse
        .setErrorMessage(ERROR_MSG_PREFIX + "The externalCode property is missing from the event request payload");

    final Param param = new Param();
    param.setName("not-valid");
    final Events events = getEvent(param);

    final EenAlertResponsePayload response = handler.onNotification(events, TEST_REQUEST_ID).toBlocking().first();

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

  }

  @Test
  public void testFailOnSFSFServiceIOErrors() throws Exception {

    expectedResponse.setErrorCode("500");
    expectedResponse.setErrorMessage(ERROR_MSG_PREFIX + "IO issues");

    given(timeOffClientMock.getTimeoffEvent(EXTERNAL_CODE)).willThrow(new IOException("IO issues"));

    final Param param = new Param();
    param.setName("externalCode");
    param.setValue(EXTERNAL_CODE);
    final Events events = getEvent(param);

    final EenAlertResponsePayload response = handler.onNotification(events, TEST_REQUEST_ID).toBlocking().first();

    verify(timeOffClientMock).getTimeoffEvent(EXTERNAL_CODE);

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

  }

  @Test
  public void testFailOnSFSFEventTypeNotSupported() throws Exception {

    expectedResponse.setErrorCode("500");
    expectedResponse.setErrorMessage(
        ERROR_MSG_PREFIX + "Unsupported EmployeeTime approval status. Supported statuses - APPROVED, CANCELLED");

    final SFSFEmployeeTime sfsfEmployeeTime =
        new SFSFEmployeeTime(dummyDate, dummyDate, userIdNav, dummyDate, dummyDate);
    sfsfEmployeeTime.setApprovalStatus(ApprovalStatus.PENDING);
    given(timeOffClientMock.getTimeoffEvent(EXTERNAL_CODE)).willReturn(sfsfEmployeeTime);

    final Param param = new Param();
    param.setName("externalCode");
    param.setValue(EXTERNAL_CODE);
    final Events events = getEvent(param);

    final EenAlertResponsePayload response = handler.onNotification(events, TEST_REQUEST_ID).toBlocking().first();

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

    verify(timeOffClientMock).getTimeoffEvent(EXTERNAL_CODE);

  }

  @Test
  public void testFailOnSFSFUserNotFoundException() throws IOException {

    expectedResponse.setErrorCode("500");
    expectedResponse.setErrorMessage(ERROR_MSG_PREFIX + "EmployeeTime entity with id 123456789 is not found");

    given(timeOffClientMock.getTimeoffEvent(EXTERNAL_CODE)).willThrow(new FileNotFoundException("ops"));

    final Param param = new Param();
    param.setName("externalCode");
    param.setValue(EXTERNAL_CODE);
    final Events events = getEvent(param);

    final EenAlertResponsePayload response = handler.onNotification(events, TEST_REQUEST_ID).toBlocking().first();

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

    verify(timeOffClientMock).getTimeoffEvent(EXTERNAL_CODE);
  }

  // @Test
  // public void testFailOnSFSFUserParseException() throws IOException {
  //
  // expectedResponse.setErrorCode("500");
  // expectedResponse.setErrorMessage(ERROR_MSG_PREFIX + "Invalid DateTime format mallformedDate");
  // final Observable<EenAlertResponsePayload> payloadObs = Observable.just(expectedResponse);
  // given(employeeTimeEventHandlerMock.onCreateEvent(any(), anyString())).willReturn(payloadObs);
  //
  // final SFSFEmployeeTime sfsfEmployeeTime = new SFSFEmployeeTime();
  // sfsfEmployeeTime.setApprovalStatus(ApprovalStatus.APPROVED);
  // sfsfEmployeeTime.setCreatedDateTime("mallformedDate");
  // sfsfEmployeeTime.setUserId("user-1");
  // given(timeOffClientMock.getTimeoffEvent(EXTERNAL_CODE)).willReturn(sfsfEmployeeTime);
  //
  // final Param param = new Param();
  // param.setName("externalCode");
  // param.setValue(EXTERNAL_CODE);
  // final Events events = getEvent(param);
  //
  // final EenAlertResponsePayload response = handler.onNotification(events, null,
  // TEST_REQUEST_ID).toBlocking().first();
  //
  // assertThat(response).isEqualToComparingFieldByField(expectedResponse);
  //
  // verify(timeOffClientMock).getTimeoffEvent(EXTERNAL_CODE);
  // }


  @Test
  public void testFailOnEventHandlerFails() throws Exception {

    expectedResponse.setErrorCode("500");
    expectedResponse.setErrorMessage(ERROR_MSG_PREFIX + " ");
    final Observable<EenAlertResponsePayload> payloadObs = Observable.just(expectedResponse);
    given(employeeTimeEventHandlerMock.onCreateEvent(any(), anyString())).willReturn(payloadObs);

    final OffsetDateTime createdDate = OffsetDateTime.parse("2007-12-03T10:15:30+01:00");
    final OffsetDateTime lastModified = OffsetDateTime.parse("2007-12-03T10:15:30+01:00");
    final SFSFEmployeeTime sfsfEmployeeTime =
        new SFSFEmployeeTime(dummyDate, dummyDate, userIdNav, createdDate, lastModified);
    sfsfEmployeeTime.setApprovalStatus(ApprovalStatus.APPROVED);

    sfsfEmployeeTime.setUserId("user-1");

    given(timeOffClientMock.getTimeoffEvent(EXTERNAL_CODE)).willReturn(sfsfEmployeeTime);

    final SFSFEmployeeTimeList sfsfEmployeeTimeList = new SFSFEmployeeTimeList();
    given(cacheMock.get("user-1")).willReturn(sfsfEmployeeTimeList);

    final Param param = new Param();
    param.setName("externalCode");
    param.setValue(EXTERNAL_CODE);
    final Events events = getEvent(param);

    final EenAlertResponsePayload response = handler.onNotification(events, TEST_REQUEST_ID).toBlocking().first();

    assertThat(sfsfEmployeeTimeList.getResults()).hasSize(1).containsOnly(sfsfEmployeeTime);
    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

    verify(timeOffClientMock).getTimeoffEvent(EXTERNAL_CODE);
    verify(employeeTimeEventHandlerMock).onCreateEvent(any(), anyString());
    verify(cacheMock).get("user-1");
    verify(cacheMock).put("user-1", sfsfEmployeeTimeList);

  }

  private Events getEvent(final Param param) {
    final Events events = new Events();
    final EenAlertRequestData eenAlertRequestData = new EenAlertRequestData();
    final EntityKeys entityKeys = new EenAlertRequestData.EntityKeys();
    entityKeys.getEntityKey().add(param);
    eenAlertRequestData.setEntityKeys(entityKeys);
    events.getEvent().add(eenAlertRequestData);
    return events;
  }

}
