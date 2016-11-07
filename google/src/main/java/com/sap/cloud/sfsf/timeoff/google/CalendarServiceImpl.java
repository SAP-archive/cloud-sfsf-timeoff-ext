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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.gmail.model.VacationSettings;
import com.sap.cloud.sfsf.timeoff.jackson.DefaultMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rx.Observable;

@Service
public class CalendarServiceImpl implements CalendarService {

	private static final String PRIMARY = "primary";
	private final GoogleService googleServices;
	private final ObjectMapper mapper = DefaultMapper.newMapper();

	private static final Logger logger = LoggerFactory.getLogger(CalendarServiceImpl.class);

	@Autowired
	public CalendarServiceImpl(final GoogleService googleServices) {
		this.googleServices = googleServices;
	}

	@Override
	public Observable<Event> createEvent(final Event event, final String userId) {

		return Observable.fromCallable(() -> {
			logger.debug(String.format("Creating calendar event: %s", asJson(event)));
			return googleServices.calendar(userId).events().insert(PRIMARY, event).execute();
		});
	}

	@Override
	public Observable<Event> updateEvent(final String eventId, final Event event, final String userId) {

		return Observable.fromCallable(() -> {
			logger.debug(String.format("Updating existing calendar event: %s", asJson(event)));
			return googleServices.calendar(userId).events().update(PRIMARY, eventId, event).execute();
		});
	}

	@Override
	public Observable<Void> deleteEvent(final String eventId, final String userId) {

		return Observable.fromCallable(() -> {
			logger.debug(String.format("Deleting calendar event with id: %s", eventId));
			return googleServices.calendar(userId).events().delete(PRIMARY, eventId).execute();
		});
	}

	@Override
	public Observable<Events> getEvents(final DateTime start, final DateTime end, final String userId) {

		return Observable.fromCallable(() -> {
			logger.debug(String.format("Returning calendar events for user %s: ", userId));
			return googleServices.calendar(userId).events().list(PRIMARY).setTimeMin(start).setTimeMax(end)
					.execute();
		});
	}

	@Override
	public Observable<VacationSettings> setAutoReply(final VacationSettings vs, final String userId) {

		return Observable.fromCallable(() -> {
			logger.debug(String.format("Updating auto-reply settings %s: ", asJson(vs)));
			return googleServices.gmail(userId).users().settings().updateVacation("me", vs).execute();
		});
	}

	@Override
	public Observable<VacationSettings> getAutoReply(final String userId) {

		return Observable.fromCallable(() -> {
			logger.debug(String.format("Returning auto-reply settings for user %s: ", userId));
			return googleServices.gmail(userId).users().settings().getVacation("me").execute();
		});
	}

	private <T> String asJson(final T obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (final JsonProcessingException e) {
			logger.error("Failed to serialize object.", e);
		}
		return null;
	}
}
