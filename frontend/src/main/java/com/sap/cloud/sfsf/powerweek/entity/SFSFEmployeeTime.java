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
package com.sap.cloud.sfsf.powerweek.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sap.cloud.sfsf.powerweek.jackson.SFSFDayTimeDeserializer;

@JsonRootName("d")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SFSFEmployeeTime implements Serializable {

	private static final long serialVersionUID = -4423414036020519040L;

	private static final Logger LOGGER = LoggerFactory.getLogger(SFSFEmployeeTime.class);

	static final String DEFAULT_TIME_ZONE = "UTC";

	private String id;
	private String externalCode;
	private String userId;
	private String quantityInDays;

	private final ZonedDateTime startDate;
	private final ZonedDateTime endDate;

	private String timeType;
	private String quantityInHours;
	private final UserIdNav userIdNav;
	private TimeTypeNav timeTypeNav;
	private String comment;
	private ApprovalStatus approvalStatus;

	private final ZonedDateTime createdDateTime;
	private final ZonedDateTime lastModifiedDateTime;

	private Action action;

	public enum Action {
		CREATE, UPDATE, DELETE;
	}

	@JsonCreator
	public SFSFEmployeeTime(@JsonDeserialize(using = SFSFDayTimeDeserializer.class) final OffsetDateTime startDate,
			@JsonDeserialize(using = SFSFDayTimeDeserializer.class) final OffsetDateTime endDate,
			final UserIdNav userIdNav,
			@JsonDeserialize(using = SFSFDayTimeDeserializer.class) final OffsetDateTime createdDateTime,
			@JsonDeserialize(using = SFSFDayTimeDeserializer.class) final OffsetDateTime lastModifiedDateTime) {
		this.userIdNav = userIdNav;

		String timeZoneString = getUserIdNav().getJob().getTimezone();
		if (timeZoneString == null) {
			timeZoneString = getUserIdNav().getTimeZone();
		}
		if (StringUtils.isEmpty(timeZoneString)) {
			LOGGER.warn("The property timeZone for User entity with id {} is not set. Falling back to default: {}",
					getUserId(), DEFAULT_TIME_ZONE);
			timeZoneString = DEFAULT_TIME_ZONE;
		}
		final TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
		LOGGER.debug("User {} timezone resolved to {}", getUserId(), timeZone.getDisplayName());

		final TimeZone userTimeZone = TimeZone.getTimeZone(timeZoneString);
		final ZoneId zoneId = userTimeZone.toZoneId();
		this.startDate = startDate.atZoneSimilarLocal(zoneId);
		this.endDate = endDate
				// TODO: remove this once SFSF fixes their API
				.plusDays(1).atZoneSimilarLocal(zoneId);
		this.createdDateTime = createdDateTime.atZoneSimilarLocal(zoneId);
		this.lastModifiedDateTime = lastModifiedDateTime.atZoneSimilarLocal(zoneId);
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(final String userId) {
		this.userId = userId;
	}

	public String getExternalCode() {
		return externalCode;
	}

	public void setExternalCode(final String externalCode) {
		this.externalCode = externalCode;
	}

	public String getQuantityInDays() {
		return quantityInDays;
	}

	public void setQuantityInDays(final String quantityInDays) {
		this.quantityInDays = quantityInDays;
	}

	public ZonedDateTime getStartDate() {
		return startDate;
	}

	public ZonedDateTime getEndDate() {
		return endDate;
	}

	public String getTimeType() {
		return timeType;
	}

	public void setTimeType(final String timeType) {
		this.timeType = timeType;
	}

	public String getQuantityInHours() {
		return quantityInHours;
	}

	public void setQuantityInHours(final String quantityInHours) {
		this.quantityInHours = quantityInHours;
	}

	public UserIdNav getUserIdNav() {
		return userIdNav;
	}

	public TimeTypeNav getTimeTypeNav() {
		return timeTypeNav;
	}

	public void setTimeTypeNav(final TimeTypeNav timeTypeNav) {
		this.timeTypeNav = timeTypeNav;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(final String comment) {
		this.comment = comment;
	}

	public ApprovalStatus getApprovalStatus() {
		return approvalStatus;
	}

	public void setApprovalStatus(final ApprovalStatus approvalStatus) {
		this.approvalStatus = approvalStatus;
	}

	public ZonedDateTime getCreatedDateTime() {
		return createdDateTime;
	}

	public ZonedDateTime getLastModifiedDateTime() {
		return lastModifiedDateTime;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(final Action action) {
		this.action = action;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public enum ApprovalStatus {
		PENDING, APPROVED, REJECTED, CANCELLED, PENDING_CANCELLATION
	}

	public boolean isUpdated() {
		final long between = ChronoUnit.MILLIS.between(getCreatedDateTime(), getLastModifiedDateTime());

		return between > 0;

	}
}
