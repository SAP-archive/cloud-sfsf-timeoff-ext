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

import java.text.MessageFormat;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.sap.cloud.commons.okhttp.OkHttpUtils;
import com.sap.cloud.sfsf.notification.EenAlertResponsePayload;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;

@Component
public class EmployeeTimeEventHandler {

	private static final Logger logger = LoggerFactory.getLogger(EmployeeTimeEventHandler.class);

	static final String ERROR_STATUS_CODE = "500";
	static final String TIMEOFF_EVENT_PROCESSED_OK = "EmployeeTime event {0} successfully";
	static final String TIMEOFF_EVENT_PROCESS_ERROR = "Error {0} out-of-office event";

	private final Observable<CalendarServiceProvider> calendarSvc;

	@Autowired
	public EmployeeTimeEventHandler(final List<CalendarServiceProvider> calendarSvc) {
		this.calendarSvc = Observable.from(calendarSvc);
	}

	public Observable<EenAlertResponsePayload> onCreateEvent(final Observable<SFSFEmployeeTime> employeeTimeObs, final String requestId) {

		return employeeTimeObs.flatMap(et ->
		  calendarSvc.flatMap(service ->
		    service.create(et, requestId)
		 ))
		.map(nothing -> {
			final EenAlertResponsePayload response = new EenAlertResponsePayload();
			response.setStatus(HttpServletResponse.SC_CREATED);
			response.setStatusDetails(
					addRequestId(requestId, MessageFormat.format(TIMEOFF_EVENT_PROCESSED_OK, "created")));
			return response;
		})
		.onErrorReturn( e -> onErrorGetResponse(requestId, e, MessageFormat.format(TIMEOFF_EVENT_PROCESS_ERROR, "creating")));
	}

	public Observable<EenAlertResponsePayload> onUpdateEvent(final Observable<SFSFEmployeeTime> employeeTimeObs,
			final String requestId) {
        return employeeTimeObs.flatMap(et ->
          calendarSvc.flatMap(service ->
            service.update(et, requestId)
         ))
		.map(nothing -> {
			final EenAlertResponsePayload response = new EenAlertResponsePayload();
			response.setStatus(HttpServletResponse.SC_OK);
			response.setStatusDetails(
					addRequestId(requestId, MessageFormat.format(TIMEOFF_EVENT_PROCESSED_OK, "updated")));
			return response;
		})
		.onErrorReturn(e -> onErrorGetResponse(requestId, e, MessageFormat.format(TIMEOFF_EVENT_PROCESS_ERROR, "updating")));
	}

	public Observable<EenAlertResponsePayload> onCancelEvent(final Observable<SFSFEmployeeTime> employeeTimeObs, final String requestId) {
          return employeeTimeObs.flatMap(et ->
          calendarSvc.flatMap(service ->
            service.cancel(et, requestId)
         ))
		.map(nothing -> {
			final EenAlertResponsePayload response = new EenAlertResponsePayload();
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			response.setStatusDetails(
					addRequestId(requestId, MessageFormat.format(TIMEOFF_EVENT_PROCESSED_OK, "deleted")));
			return response;
		})
		.onErrorReturn(e -> onErrorGetResponse(requestId, e, MessageFormat.format(TIMEOFF_EVENT_PROCESS_ERROR, "deleting")));
	}

	private EenAlertResponsePayload onErrorGetResponse(final String requestId, final Throwable e, final String errorMessage) {
		logger.error(errorMessage, e);
		final EenAlertResponsePayload response = new EenAlertResponsePayload();
		if (e instanceof HttpException) {
			final HttpException exc = (HttpException) e;
			try {
				response.setErrorCode(Integer.toString(exc.response().code()));

				final StringBuilder exceptionMessage = new StringBuilder(errorMessage).append("\n");
				final StringBuilder req = OkHttpUtils.parseRequest(exc.response().raw().request(), null);
				final StringBuilder resp = OkHttpUtils.parseResponse(exc.response().raw(), 0L, exc.response().errorBody());

				exceptionMessage.append(req.toString()).append("\n").append(resp.toString());

				response.setErrorMessage(addRequestId(requestId, exceptionMessage.toString()));
			} catch (final Exception boundedExc) {
				logger.error("Exception when parsing error response and request", boundedExc);
			}
		} else {
			logger.error(errorMessage, e);
			final String message = String.format(errorMessage + ": %s", e.getMessage());
			response.setErrorMessage(addRequestId(requestId, message));
			response.setErrorCode(ERROR_STATUS_CODE);
		}
		return response;
	}

	private String addRequestId(final String requestId, final String message) {
		return "[" + requestId + "] " + message;
	}

}
