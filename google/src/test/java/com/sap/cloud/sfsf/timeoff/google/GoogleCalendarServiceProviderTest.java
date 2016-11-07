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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.gmail.model.VacationSettings;
import com.sap.cloud.sfsf.timeoff.entity.CalendarEvent;
import com.sap.cloud.sfsf.timeoff.entity.CalendarEventsList;
import com.sap.cloud.sfsf.timeoff.entity.EmpJob;
import com.sap.cloud.sfsf.timeoff.entity.Messages;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTimeList;
import com.sap.cloud.sfsf.timeoff.entity.UserIdNav;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;
import rx.observers.TestSubscriber;

@RunWith(MockitoJUnitRunner.class)
public class GoogleCalendarServiceProviderTest {

	private static final String TEST_AUTO_REPLY = "test auto reply";
	private static final String SFSF_EMP_TIME_CREATED_STR = "2016-04-21T00:00-04:00";
	private static final String SFSF_EMP_TIME_START_STR = "2016-07-13T00:00:00-04:00";
	private static final String SFSF_EMP_TIME_END_STR = "2016-07-17T00:00:00-04:00";

	private SFSFEmployeeTime empTime;
	private EmpJob empJob;
	private UserIdNav userIdNav;
	private GoogleCalendarServiceProvider googleProvider;
	private AutoReplies automaticReplies;

	@Mock
	private CalendarService calendarService;

	@Mock
	private Messages messages;

	@Captor
	private ArgumentCaptor<Event> eventCaptor;

	@Captor
	private ArgumentCaptor<VacationSettings> vacationCaptor;

	@Captor
	private ArgumentCaptor<String> idCaptor;

	@Captor
	private ArgumentCaptor<DateTime> dateTimeStartCaptor;

	@Captor
	private ArgumentCaptor<DateTime> dateTimeEndCaptor;

	@Captor
	private ArgumentCaptor<String> eventIdCaptor;

	@Captor
	private ArgumentCaptor<String> userEmailCaptor;

	@Before
	public void setUp() throws Exception {
		empJob = new EmpJob();
		empJob.setTimezone("US/Eastern");
		final UserIdNav manager = new UserIdNav();
		empJob.setManager(manager);
		userIdNav = new UserIdNav().setJob(empJob);
		userIdNav.setEmail("test@test.com");
		automaticReplies = new AutoReplies(calendarService, messages);
		googleProvider = new GoogleCalendarServiceProvider(calendarService, automaticReplies);
	}

	@After
	public void cleanUp() {
		verifyNoMoreInteractions(calendarService);
	}

	@Test
	public void testCreateEvent() {
		// given
		empTime = employeeTime();

		given(calendarService.createEvent(any(Event.class), any(String.class))).willReturn(Observable.just(null));
		given(calendarService.getAutoReply(any(String.class)))
				.willReturn(Observable.just(new VacationSettings().setEnableAutoReply(false)));
		given(messages.getAutoReplyMessage(empTime)).willReturn(TEST_AUTO_REPLY);

		final TestSubscriber<Void> testSubscriber = new TestSubscriber<>();

		// when
		googleProvider.create(empTime, "test-request-id").subscribe(testSubscriber);

		// then
		testSubscriber.assertNoErrors();
		testSubscriber.assertCompleted();

		verify(calendarService).createEvent(eventCaptor.capture(), any(String.class));
		verify(calendarService).setAutoReply(vacationCaptor.capture(), any(String.class));
		verify(calendarService).getAutoReply(any(String.class));
		verify(messages).getAutoReplyMessage(empTime);

		final DateTime expextedStart = new DateTime("2016-07-13T00:00:00.000-04:00");
		final DateTime expectedEnd = new DateTime("2016-07-18T00:00:00.000-04:00");
		final Event expectedEvent = new Event().setSummary("Out of office").setDescription("I am out of office!")
				.setStart(new EventDateTime().setDateTime(expextedStart).setTimeZone("US/Eastern"))
				.setEnd(new EventDateTime().setDateTime(expectedEnd).setTimeZone("US/Eastern"));

		final VacationSettings expectedVacation = new VacationSettings().setEnableAutoReply(true)
				.setResponseBodyHtml(TEST_AUTO_REPLY)
				.setStartTime(empTime.getStartDate().toInstant().toEpochMilli())
				.setEndTime(empTime.getEndDate().toInstant().toEpochMilli());

		assertThat(eventCaptor.getValue()).isEqualTo(expectedEvent);
		assertThat(vacationCaptor.getValue()).isEqualTo(expectedVacation);
	}

	@Test
	public void testUpdateEvent() {
		// given
		empTime = employeeTime();
		final String id = "asd";
		empTime.setId(id);

		final VacationSettings vs = new VacationSettings().setEnableAutoReply(true)
				.setResponseBodyPlainText(TEST_AUTO_REPLY)
				.setStartTime(empTime.getStartDate().plusDays(1).toInstant().toEpochMilli())
				.setEndTime(empTime.getEndDate().toInstant().toEpochMilli());

		given(calendarService.updateEvent(any(String.class), any(Event.class), any(String.class)))
				.willReturn(Observable.just(null));
		given(calendarService.getAutoReply(any(String.class))).willReturn(Observable.just(vs));
		given(messages.getAutoReplyMessage(empTime)).willReturn(TEST_AUTO_REPLY);

		final TestSubscriber<Void> testSubscriber = new TestSubscriber<>();

		// when
		googleProvider.update(empTime, "test-request-id").subscribe(testSubscriber);

		// then
		testSubscriber.assertNoErrors();
		testSubscriber.assertCompleted();

		verify(calendarService).updateEvent(any(String.class), eventCaptor.capture(), any(String.class));
		verify(calendarService).setAutoReply(vacationCaptor.capture(), any(String.class));
		verify(calendarService).getAutoReply(any(String.class));
		verify(messages).getAutoReplyMessage(empTime);

		final DateTime expextedStart = new DateTime("2016-07-13T00:00:00.000-04:00");
		final DateTime expectedEnd = new DateTime("2016-07-18T00:00:00.000-04:00");
		final Event expectedEvent = new Event().setSummary("Out of office").setDescription("I am out of office!")
				.setStart(new EventDateTime().setDateTime(expextedStart).setTimeZone("US/Eastern"))
				.setEnd(new EventDateTime().setDateTime(expectedEnd).setTimeZone("US/Eastern"));

		final VacationSettings expectedVacation = new VacationSettings().setEnableAutoReply(true)
				.setResponseBodyHtml(TEST_AUTO_REPLY)
				.setStartTime(empTime.getStartDate().toInstant().toEpochMilli())
				.setEndTime(empTime.getEndDate().toInstant().toEpochMilli());

		assertThat(eventCaptor.getValue()).isEqualTo(expectedEvent);
		assertThat(vacationCaptor.getValue()).isEqualTo(expectedVacation);

	}


	@Test
	public void testGetConflictingEvents() {
		// given
		empTime = employeeTime();
		final String id = "asd";
		empTime.setId(id);
		final SFSFEmployeeTimeList sfsfList = new SFSFEmployeeTimeList();
		sfsfList.setResults(Arrays.asList(empTime));

		final DateTime startEv1 = new DateTime("2016-07-11T00:00:00.000-04:00");
		final DateTime endEv1 = new DateTime("2016-07-12T00:00:00.000-04:00");
		final EventDateTime eventDateTimeStartEv1 = new EventDateTime().setDateTime(startEv1).setTimeZone("US/Eastern");
		final EventDateTime eventDateTimeEndEv1 = new EventDateTime().setDateTime(endEv1).setTimeZone("US/Eastern");

		final DateTime startEv2 = new DateTime("2016-07-15T00:00:00.000-04:00");
		final DateTime endEv2 = new DateTime("2016-07-17T00:00:00.000-04:00");
		final EventDateTime eventDateTimeStartEv2 = new EventDateTime().setDateTime(startEv2).setTimeZone("US/Eastern");
		final EventDateTime eventDateTimeEndEv2 = new EventDateTime().setDateTime(endEv2).setTimeZone("US/Eastern");

		final Event ev1 = new Event();
		ev1.setStart(eventDateTimeStartEv1);
		ev1.setEnd(eventDateTimeEndEv1);
		ev1.setSummary("Out of office");
		final Event ev2 = new Event();
		ev2.setStart(eventDateTimeStartEv2);
		ev2.setEnd(eventDateTimeEndEv2);
		ev2.setSummary("Meeting");
		final Events events = new Events();
		events.setItems(Arrays.asList(ev1, ev2));

		final CalendarEventsList expected = new CalendarEventsList();
		final CalendarEvent expectedEvent = new CalendarEvent();
		expected.setValue(Arrays.asList(expectedEvent));

		given(calendarService.getEvents(any(DateTime.class), any(DateTime.class), any(String.class)))
				.willReturn(Observable.just(events));

		// when
		final Observable<CalendarEventsList> result = new GoogleCalendarServiceProvider(calendarService,
				automaticReplies).getConflicting(sfsfList);
		final TestSubscriber<CalendarEventsList> testSubscriber = new TestSubscriber<>();
		result.subscribe(testSubscriber);

		// then
		verify(calendarService).getEvents(any(DateTime.class), any(DateTime.class), any(String.class));

		assertEquals(testSubscriber.getOnNextEvents().size(), 1);

		final CalendarEventsList cel = testSubscriber.getOnNextEvents().get(0);
		final List<CalendarEvent> calEvents = cel.getValue();

		assertEquals(calEvents.size(), 1);

		final CalendarEvent calEv = calEvents.get(0);

		assertEquals(calEv.getStart().toInstant().getEpochSecond() * 1000,
				eventDateTimeStartEv2.getDateTime().getValue());

	}

	private SFSFEmployeeTime employeeTime() {
		final OffsetDateTime dateUpdated = OffsetDateTime.parse(SFSF_EMP_TIME_CREATED_STR);
		final OffsetDateTime dateCreated = OffsetDateTime.parse(SFSF_EMP_TIME_CREATED_STR);
		final OffsetDateTime startDate = OffsetDateTime.parse(SFSF_EMP_TIME_START_STR);
		final OffsetDateTime endDate = OffsetDateTime.parse(SFSF_EMP_TIME_END_STR);
		empTime = new SFSFEmployeeTime(startDate, endDate, userIdNav, dateCreated, dateUpdated);
		empTime.setComment("I am out of office!");
		return empTime;
	}

}
