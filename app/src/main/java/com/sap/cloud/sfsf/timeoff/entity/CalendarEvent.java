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
package com.sap.cloud.sfsf.timeoff.entity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.sfsf.timeoff.jackson.DateTimeTimeZoneDeserializer;
import com.sap.cloud.sfsf.timeoff.jackson.DateTimeTimeZoneSerializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CalendarEvent {

  public enum EventStatus {
    free, tentative, busy, oof, workingelsewhere, unknown;

    @JsonCreator
    public static EventStatus fromString(final String key) {
      return key == null ? null : EventStatus.valueOf(key.toLowerCase());
    }
  }

  protected String id;
  protected String subject;

  @JsonDeserialize(using = DateTimeTimeZoneDeserializer.class)
  @JsonSerialize(using = DateTimeTimeZoneSerializer.class)
  protected ZonedDateTime start;

  @JsonDeserialize(using = DateTimeTimeZoneDeserializer.class)
  @JsonSerialize(using = DateTimeTimeZoneSerializer.class)
  protected ZonedDateTime end;


  protected boolean isAllDay;
  protected EventStatus showAs;
  protected List<String> attendeesNames = new ArrayList<>();
  protected String bodyText;
  protected String webLink;

  public String getId() {
    return id;
  }

  public CalendarEvent setId(final String id) {
    this.id = id;
    return this;
  }

  public String getSubject() {
    return subject;
  }

  public CalendarEvent setSubject(final String subject) {
    this.subject = subject;
    return this;
  }

  public ZonedDateTime getStart() {
    return start;
  }

  public CalendarEvent setStart(final ZonedDateTime start) {
    this.start = start;
    return this;
  }

  public ZonedDateTime getEnd() {
    return end;
  }

  public CalendarEvent setEnd(final ZonedDateTime end) {
    this.end = end;
    return this;
  }

  @JsonProperty(value = "isAllDay")
  public boolean getIsAllDay() {
    return isAllDay;
  }

  public CalendarEvent setAllDay(final boolean isAllDay) {
    this.isAllDay = isAllDay;
    return this;
  }

  public EventStatus getShowAs() {
    return showAs;
  }

  public CalendarEvent setShowAs(final EventStatus showAs) {
    this.showAs = showAs;
    return this;
  }

  public List<String> getAttendeesNames() {
    return attendeesNames;
  }

  public CalendarEvent setAttendeesNames(final List<String> attendees) {
    attendeesNames = attendees;
    return this;
  }

  public String getBodyText() {
    return bodyText;
  }

  public CalendarEvent setBodyText(final String bodyText) {
    this.bodyText = bodyText;
    return this;
  }

  public String getWebLink() {
    return webLink;
  }

  public CalendarEvent setWebLink(final String webLink) {
    this.webLink = webLink;
    return this;
  }

}
