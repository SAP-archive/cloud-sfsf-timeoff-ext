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
package com.sap.cloud.sfsf.timeoff.svc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.ConfigurationException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.sap.cloud.sfsf.timeoff.CalendarServiceProvider;
import com.sap.cloud.sfsf.timeoff.SFSFEmployeeTimeService;
import com.sap.cloud.sfsf.timeoff.entity.CalendarEvent;
import com.sap.cloud.sfsf.timeoff.entity.CalendarEventsList;
import com.sap.cloud.sfsf.timeoff.entity.EmpJob;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTimeList;
import com.sap.cloud.sfsf.timeoff.entity.UserIdNav;
import com.sap.cloud.sfsf.timeoff.entity.CalendarEvent.EventStatus;
import com.sap.cloud.sfsf.timeoff.svc.CalendarEventsSvc;
import com.sap.cloud.sfsf.timeoff.svc.ConflictingEventsException;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class CalendarEventsSvcImplTest {


  private static final String TIMEOFF_ALIAS = "timeoff";
  private static Cache<String, SFSFEmployeeTimeList> cache;
  private static CacheManager cacheManager;

  private SFSFEmployeeTime employeeTime;
  private SFSFEmployeeTimeList employeeTimeList;
  private CalendarEvent calendarEventOOF;
  private CalendarEvent calendarEventNotOOF;
  private CalendarEventsList calendarEventsList;

  @Mock
  private CalendarServiceProvider calendarSvcMock;

  @Mock
  private SFSFEmployeeTimeService sfsfEmployeeTimeServiceMock;

  @Mock
  private Principal principalMock;

  private CalendarEventsSvc calendarEventsSvcImpl;

  @BeforeClass
  public static void beforeClass() {
    cacheManager = CacheManagerBuilder.newCacheManagerBuilder().withCache(TIMEOFF_ALIAS, CacheConfigurationBuilder
        .newCacheConfigurationBuilder(String.class, SFSFEmployeeTimeList.class, ResourcePoolsBuilder.heap(10).build()))
        .build();
    cacheManager.init();
    cache = cacheManager.getCache(TIMEOFF_ALIAS, String.class, SFSFEmployeeTimeList.class);
  }

  @AfterClass
  public static void afterClass() {
    cacheManager.close();
  }

  @Before
  public void before() {
    employeeTimeList = new SFSFEmployeeTimeList();
    final OffsetDateTime now = OffsetDateTime.now();
    final EmpJob empJob = new EmpJob();
    empJob.setTimezone("US/Eastern");
    employeeTime = new SFSFEmployeeTime(now, now, new UserIdNav().setJob(empJob), now, now);

    calendarEventNotOOF = new CalendarEvent();
    calendarEventNotOOF.setShowAs(EventStatus.oof);

    calendarEventOOF = new CalendarEvent();
    calendarEventOOF.setShowAs(EventStatus.free);

    calendarEventsList = new CalendarEventsList();
    calendarEventsSvcImpl = new CalendarEventsSvc(sfsfEmployeeTimeServiceMock, Arrays.asList(calendarSvcMock), cache);
  }

  @After
  public void after() {
    cache.clear();
    verifyNoMoreInteractions(sfsfEmployeeTimeServiceMock, calendarSvcMock, sfsfEmployeeTimeServiceMock);
  }

  @Test
  public void testGetConflictingEvents()
      throws JsonParseException, JsonMappingException, ConfigurationException, IOException {
    // given
    employeeTimeList.setResults(Arrays.asList(employeeTime));
    calendarEventsList.setValue(Arrays.asList(calendarEventOOF));
    given(principalMock.getName()).willReturn("test-user");
    given(sfsfEmployeeTimeServiceMock.getLatestTimeOffEventForUser("test-user")).willReturn(employeeTimeList);
    given(calendarSvcMock.getConflicting(employeeTimeList)).willReturn(Observable.just(calendarEventsList));

    // when
    final List<CalendarEvent> conflictingEvents = calendarEventsSvcImpl.getConflictingEvents(principalMock);

    // then
    verify(sfsfEmployeeTimeServiceMock).getLatestTimeOffEventForUser("test-user");
    assertThat(cache.get("test-user")).isEqualToComparingFieldByFieldRecursively(employeeTimeList);
    assertThat(cache.iterator()).hasSize(1);
    verify(calendarSvcMock).getConflicting(employeeTimeList);
    assertThat(conflictingEvents).containsExactly(calendarEventOOF);
  }

  @Ignore("Enable Spring 5.0 and reactive programming")
  @Test
  public void testErrorInCommunication()
      throws JsonParseException, JsonMappingException, ConfigurationException, IOException {
    // given
    employeeTimeList.setResults(new ArrayList<>());
    given(principalMock.getName()).willReturn("test-user");
    given(sfsfEmployeeTimeServiceMock.getLatestTimeOffEventForUser("test-user"))
        .willThrow(new IOException("network issues"));


    assertThatThrownBy(() -> {
      // when
      calendarEventsSvcImpl.getConflictingEvents(principalMock);
    }).isInstanceOf(ConflictingEventsException.class).hasMessage("network issues");


    assertThat(cache.iterator()).isEmpty();
    verify(sfsfEmployeeTimeServiceMock).getLatestTimeOffEventForUser("test-user");
  }

  @Test
  public void testEmptyResponseFromSFSF()
      throws JsonParseException, JsonMappingException, ConfigurationException, IOException {
    // given
    employeeTimeList.setResults(new ArrayList<>());
    given(principalMock.getName()).willReturn("test-user");
    given(sfsfEmployeeTimeServiceMock.getLatestTimeOffEventForUser("test-user")).willReturn(employeeTimeList);

    // when
    final List<CalendarEvent> conflictingEvents = calendarEventsSvcImpl.getConflictingEvents(principalMock);

    // then
    assertThat(cache.iterator()).isEmpty();
    verify(sfsfEmployeeTimeServiceMock).getLatestTimeOffEventForUser("test-user");
    assertThat(conflictingEvents).isEmpty();
  }

  @Test
  public void testNullResponseFromSFSF()
      throws JsonParseException, JsonMappingException, ConfigurationException, IOException {
    // given
    employeeTimeList.setResults(new ArrayList<>());
    given(principalMock.getName()).willReturn("test-user");
    given(sfsfEmployeeTimeServiceMock.getLatestTimeOffEventForUser("test-user")).willReturn(null);

    // when
    final List<CalendarEvent> conflictingEvents = calendarEventsSvcImpl.getConflictingEvents(principalMock);

    // then
    assertThat(cache.iterator()).isEmpty();
    assertThat(conflictingEvents).isEmpty();
    verify(sfsfEmployeeTimeServiceMock).getLatestTimeOffEventForUser("test-user");
  }


  @Test
  public void testNoReponseFromCalendarService()
      throws JsonParseException, JsonMappingException, ConfigurationException, IOException {
    // given
    employeeTimeList.setResults(Arrays.asList(employeeTime));
    given(principalMock.getName()).willReturn("test-user");
    given(sfsfEmployeeTimeServiceMock.getLatestTimeOffEventForUser("test-user")).willReturn(employeeTimeList);
    given(calendarSvcMock.getConflicting(employeeTimeList)).willReturn(Observable.empty());

    // when
    final List<CalendarEvent> conflictingEvents = calendarEventsSvcImpl.getConflictingEvents(principalMock);

    // then
    verify(sfsfEmployeeTimeServiceMock).getLatestTimeOffEventForUser("test-user");
    assertThat(cache.get("test-user")).isEqualToComparingFieldByFieldRecursively(employeeTimeList);
    assertThat(cache.iterator()).hasSize(1);
    assertThat(conflictingEvents).isEmpty();
    verify(calendarSvcMock).getConflicting(employeeTimeList);
  }

  @Test
  public void testFilterOnlyRightEventsFromCalendarService()
      throws JsonParseException, JsonMappingException, ConfigurationException, IOException {
    // given
    employeeTimeList.setResults(Arrays.asList(employeeTime));
    calendarEventsList.setValue(Arrays.asList(calendarEventNotOOF, calendarEventOOF));
    given(principalMock.getName()).willReturn("test-user");
    given(sfsfEmployeeTimeServiceMock.getLatestTimeOffEventForUser("test-user")).willReturn(employeeTimeList);
    given(calendarSvcMock.getConflicting(employeeTimeList)).willReturn(Observable.just(calendarEventsList));

    // when
    final List<CalendarEvent> conflictingEvents = calendarEventsSvcImpl.getConflictingEvents(principalMock);

    // then
    assertThat(conflictingEvents).containsOnly(calendarEventOOF);
    verify(sfsfEmployeeTimeServiceMock).getLatestTimeOffEventForUser("test-user");
    assertThat(cache.get("test-user")).isEqualToComparingFieldByFieldRecursively(employeeTimeList);
    assertThat(cache.iterator()).hasSize(1);
    verify(calendarSvcMock).getConflicting(employeeTimeList);
  }

}
