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

import java.time.ZonedDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.gmail.model.VacationSettings;
import com.sap.cloud.sfsf.timeoff.entity.Messages;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.jackson.DefaultMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import rx.Observable;

@Component
public class AutoReplies {
	private final CalendarService calendarService;
	private final Messages message;

	private final ObjectMapper mapper = DefaultMapper.newMapper();

	private final Logger logger = LoggerFactory.getLogger(AutoReplies.class);

	@Autowired
	public AutoReplies(final CalendarService calendarService, final Messages message) {
		this.calendarService = calendarService;
		this.message = message;
	}

	public Observable<VacationSettings> getAutomaticReply(final SFSFEmployeeTime employeeTime) {
		return calendarService.getAutoReply(employeeTime.getUserId());
	}

	public Observable<VacationSettings> enableAutomaticReply(final SFSFEmployeeTime employeeTime) {
		final ZonedDateTime start = employeeTime.getStartDate();
		final ZonedDateTime end = employeeTime.getEndDate();
		final String replyMessage = message.getAutoReplyMessage(employeeTime);
		final VacationSettings autoReplySettings = new VacationSettings()
				// start time for autoreply
				.setStartTime(start.toInstant().toEpochMilli())
				// end time for autoreply
				.setEndTime(end.toInstant().toEpochMilli())
				// it's scheduled for specific period
				.setEnableAutoReply(true)
				// comment
				.setResponseBodyHtml(replyMessage);
		logger.debug(String.format("Enabling Google Autoreply: %s", asJson(autoReplySettings)));
		return calendarService.setAutoReply(autoReplySettings, employeeTime.getUserId());
	}

	public Observable<VacationSettings> disableAutomaticReply(final String userId) {
		final VacationSettings disabledAutoreplySetting = new VacationSettings().setEnableAutoReply(false);
		logger.debug(String.format("Disabling Google Autoreply: %s", asJson(disabledAutoreplySetting)));
		return calendarService.setAutoReply(disabledAutoreplySetting, userId);
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
