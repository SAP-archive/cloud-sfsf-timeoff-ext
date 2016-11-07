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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.gmail.model.VacationSettings;
import com.sap.cloud.sfsf.timeoff.CalendarServiceProvider;
import com.sap.cloud.sfsf.timeoff.entity.CalendarEvent;
import com.sap.cloud.sfsf.timeoff.entity.CalendarEventsList;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTimeList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import rx.Observable;

@Component
public class GoogleCalendarServiceProvider implements CalendarServiceProvider {

  private static final String OUT_OF_OFFICE = "Out of office";

  private final CalendarService calendarService;
  private final AutoReplies autoReplies;

  @Autowired
  public GoogleCalendarServiceProvider(final CalendarService calendarService, final AutoReplies autoReplies) {
    this.calendarService = calendarService;
    this.autoReplies = autoReplies;
  }

  @Override
  public Observable<Void> create(final SFSFEmployeeTime employeeTime, final String requestId) {
    return Observable.just(employeeTime).flatMap(et -> {
      final Observable<Event> outOfOfficeEvent =
          calendarService.createEvent(createEvent(employeeTime), employeeTime.getUserId());
      final Observable<VacationSettings> automaticReplay = autoReplies.getAutomaticReply(et)
          // only correct items
          .filter(ar -> (!ar.getEnableAutoReply()) || isStartTimeBeforeOrEquals(et, ar)).flatMap(ar -> {
            return autoReplies.enableAutomaticReply(et);
          }).defaultIfEmpty(null);
      return Observable.zip(outOfOfficeEvent, automaticReplay, (calEvent, autoReplay) -> {
        return null;
      });
    });
  }

  @Override
  public Observable<Void> update(final SFSFEmployeeTime employeeTime, final String requestId) {
    return Observable.just(employeeTime).flatMap(et -> {
      final Observable<Event> outOfOfficeEvent = calendarService.updateEvent(employeeTime.getExternalCode(),
          createEvent(employeeTime), employeeTime.getUserId());
      final Observable<VacationSettings> automaticReplay = autoReplies.getAutomaticReply(et)
          // filter
          .filter(ar -> isStartTimeBeforeOrEquals(et, ar)).flatMap(ar -> {
            return autoReplies.enableAutomaticReply(et);
          }).defaultIfEmpty(null);
      return Observable.zip(outOfOfficeEvent, automaticReplay, (calEvent, autoReplay) -> {
        return null;
      });
    });
  }

  @Override
  public Observable<Void> cancel(final SFSFEmployeeTime employeeTime, final String requestId) {
    return Observable.just(employeeTime).flatMap(et -> {
      final Observable<Void> o = calendarService.deleteEvent(employeeTime.getExternalCode(), employeeTime.getUserId());
      final Observable<VacationSettings> automaticReplay = autoReplies.getAutomaticReply(et)
          // filter
          .filter(ar -> isAutoreplyCreatedBySFSFEvent(et, ar)).flatMap(ar -> {
            return autoReplies.disableAutomaticReply(et.getUserId());
          }).defaultIfEmpty(null);
      return Observable.zip(o, automaticReplay, (a, autoReplay) -> {
        return null;
      });
    });
  }

  @Override
  public Observable<CalendarEventsList> getConflicting(final SFSFEmployeeTimeList employeeTime) {
    return Observable.from(employeeTime.getResults())
        .flatMap(emplTm -> calendarService.getEvents(fromZonedDateToDateTime(emplTm.getStartDate()),
            fromZonedDateToDateTime(emplTm.getEndDate()), emplTm.getUserId()))

        .reduce(new ArrayList<CalendarEvent>(), (listEv, events) -> {
          listEv.addAll(toCalendaEvent(events));
          return listEv;
        }).map(new CalendarEventsList()::setValue);
  }

  private List<CalendarEvent> toCalendaEvent(final Events events) {
    return events.getItems().stream().filter(event -> !OUT_OF_OFFICE.equals(event.getSummary()))
        .map(event -> parse(event, events.getTimeZone())).collect(Collectors.toList());
  }

  private CalendarEvent parse(final Event event, final String calendarTimeZone) {
    final CalendarEvent calEv = new CalendarEvent();
    calEv.setId(event.getId());
    calEv.setBodyText(event.getDescription());
    calEv.setStart(parse(event.getStart(), calendarTimeZone));
    calEv.setEnd(parse(event.getEnd(), calendarTimeZone));
    calEv.setWebLink(event.getHtmlLink());
    calEv.setSubject(event.getSummary());

    if (event.getAttendees() != null) {
      calEv.setAttendeesNames(event.getAttendees().stream().map(attend -> attend.getDisplayName())
          .filter(name -> name != null).collect(Collectors.toCollection(ArrayList::new)));
    }

    return calEv;
  }

  private ZonedDateTime parse(final EventDateTime start, final String calendarTimeZone) {
    DateTime dt = start.getDateTime();
    if (dt == null) {
      dt = start.getDate();
    }

    final String timeZone = start.getTimeZone() != null ? start.getTimeZone() : calendarTimeZone;

    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(dt.getValue()), ZoneId.of(timeZone));
  }

  private EventDateTime fromZonedDateToEventDate(final ZonedDateTime dt) {
    return new EventDateTime().setDateTime(fromZonedDateToDateTime(dt)).setTimeZone(dt.getZone().getId());
  }

  private DateTime fromZonedDateToDateTime(final ZonedDateTime dt) {
    final TimeZone timeZone = TimeZone.getTimeZone(dt.getZone());
    final DateTime dateTime = new DateTime(Date.from(dt.toInstant()), timeZone);

    return dateTime;
  }

  private Event createEvent(final SFSFEmployeeTime employeeTime) {
    final Event event = new Event().setSummary(OUT_OF_OFFICE).setDescription(employeeTime.getComment());

    final ZonedDateTime startDT = employeeTime.getStartDate();
    event.setStart(fromZonedDateToEventDate(startDT));

    final ZonedDateTime endDT = employeeTime.getEndDate();
    event.setEnd(fromZonedDateToEventDate(endDT));

    event.setId(employeeTime.getExternalCode());

    return event;
  }

  private boolean isStartTimeBeforeOrEquals(final SFSFEmployeeTime etemployeeTime, final VacationSettings autoreply) {
    final Long autoreplyMs = autoreply.getStartTime();
    return autoreplyMs == null || etemployeeTime.getStartDate().toInstant().toEpochMilli() <= autoreplyMs;
  }

  private boolean isAutoreplyCreatedBySFSFEvent(final SFSFEmployeeTime employeeTimeEvent,
      final VacationSettings autoreply) {
    if (!autoreply.getEnableAutoReply()) {
      return false;
    }

    // check if the same start time
    final Long autoReplyStart = autoreply.getStartTime();
    if (autoReplyStart == null) {
      return false;
    }

    final Long eventStartTime = employeeTimeEvent.getStartDate().toInstant().getEpochSecond() * 1000;

    if (!eventStartTime.equals(autoReplyStart)) {
      return false;
    }

    // check if the same end time
    final Long autoReplyEnd = autoreply.getEndTime();
    if (autoReplyEnd == null) {
      return false;
    }

    final Long eventEndTime = employeeTimeEvent.getEndDate().toInstant().getEpochSecond() * 1000;
    if (!eventEndTime.equals(autoReplyEnd)) {
      return false;
    }

    return true;

  }
}
