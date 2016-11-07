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

import java.io.FileNotFoundException;
import java.text.MessageFormat;

import com.sap.cloud.sfsf.notification.EenAlertResponsePayload;
import com.sap.cloud.sfsf.notification.Events;
import com.sap.cloud.sfsf.notification.handler.NotificationHandler;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTimeList;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime.Action;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime.ApprovalStatus;

import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import rx.Observable;
import rx.schedulers.Schedulers;

@Component
public class TimeoffNotificationHandler implements NotificationHandler {

  static final String ERROR_STATUS_CODE = "500";
  static final String ERROR_MESSAGE = "Error processing EmployeeTime event";

  private static final Logger logger = LoggerFactory.getLogger(TimeoffNotificationHandler.class);

  private final EmployeeTimeEventHandler employeeTimeEventHandler;
  private final SFSFEmployeeTimeService sfsfEmployeeTimeSvc;
  private final Cache<String, SFSFEmployeeTimeList> employeeTimeRequestsPerUserCache;

  @Autowired
  public TimeoffNotificationHandler(final EmployeeTimeEventHandler employeeTimeEventHandler,
      final SFSFEmployeeTimeService sfsfEmployeeTimeSvc,
      final Cache<String, SFSFEmployeeTimeList> employeeTimeRequestsPerUserCache) {
    this.employeeTimeEventHandler = employeeTimeEventHandler;
    this.sfsfEmployeeTimeSvc = sfsfEmployeeTimeSvc;
    this.employeeTimeRequestsPerUserCache = employeeTimeRequestsPerUserCache;
  }

  @Override
  public Observable<EenAlertResponsePayload> onNotification(final Events events, final String requestId) {
    logger.trace("Notification handler invoked on new EmployeeTime event");
    final Observable<EenAlertResponsePayload> obs = Observable.from(events.getEvent())
        // to entity key obs
        .flatMapIterable(event -> event.getEntityKeys().getEntityKey())
        .filter(key -> "externalCode".equals(key.getName()))
        .switchIfEmpty(Observable
            .error(new IllegalStateException("The externalCode property is missing from the event request payload")))
        // logging
        .doOnNext(key -> logger.debug("Get EmployeeTime entity {}", key.getValue()))
        // stuff
        .flatMap(key -> getTimeOffEventEntity(key.getValue())
            .onErrorResumeNext(e -> onGetTimeOffError(e, key.getValue()))
            .filter(timeOffEvent -> acceptEventStatus(timeOffEvent))
            // not supported status
            .switchIfEmpty(Observable.error(new IllegalArgumentException(
                "Unsupported EmployeeTime approval status. Supported statuses - APPROVED, CANCELLED")))
            .doOnNext(
                timeOffEvent -> logger.debug("EmployeeTime entity for userId {} received", timeOffEvent.getUserId()))
            .flatMap(timeOffEvent -> {
              return setEventAction(timeOffEvent);
            }).doOnNext(timeOffEvent -> updateCache(timeOffEvent)).flatMap(timeOffEvent -> {
              return handleEvent(timeOffEvent, requestId);
            }))
        .onErrorReturn(e -> {
          logger.error(ERROR_MESSAGE, e);
          final EenAlertResponsePayload response = new EenAlertResponsePayload();
          final String message = String.format(ERROR_MESSAGE + ": %s", e.getMessage());
          response.setErrorMessage(addRequestId(requestId, message));
          response.setErrorCode(ERROR_STATUS_CODE);
          return response;
        });
    return obs;
  }

  private Observable<EenAlertResponsePayload> handleEvent(final SFSFEmployeeTime timeOffEvent, final String requestId) {
    final Observable<SFSFEmployeeTime> employeeTimeObs = Observable.just(timeOffEvent);
    switch (timeOffEvent.getAction()) {
      case CREATE:
        return employeeTimeEventHandler.onCreateEvent(employeeTimeObs, requestId);
      case UPDATE:
        return employeeTimeEventHandler.onUpdateEvent(employeeTimeObs, requestId);
      case DELETE:
        return employeeTimeEventHandler.onCancelEvent(employeeTimeObs, requestId);
      default:
        return Observable.<EenAlertResponsePayload>never();
    }
  }

  private Observable<SFSFEmployeeTime> setEventAction(final SFSFEmployeeTime timeOffEvent) {
    // event was raised due to DELETE(CANCEL)
    if (ApprovalStatus.CANCELLED == timeOffEvent.getApprovalStatus()) {
      timeOffEvent.setAction(Action.DELETE);
    } else {
      if (timeOffEvent.isUpdated()) { // event was raised due to EDIT
        timeOffEvent.setAction(Action.UPDATE);
      } else { // event was raised due to CREATE
        timeOffEvent.setAction(Action.CREATE);
      }
    }
    logger.debug("EmployeeTime event action resolved to " + timeOffEvent.getAction().toString());
    return Observable.just(timeOffEvent);
  }

  private Observable<SFSFEmployeeTime> getTimeOffEventEntity(final String id) {
    return Observable.fromCallable(() -> sfsfEmployeeTimeSvc.getTimeoffEvent(id)).subscribeOn(Schedulers.io());
  }

  void updateCache(final SFSFEmployeeTime event) {
    final Action action = event.getAction();
    if (action == Action.CREATE || action == Action.UPDATE) {
      saveCachedEvent(event);
    } else if (action == Action.DELETE) {
      deleteCachedEvent(event);
    } else {
      throw new IllegalArgumentException("Action cannot be null");
    }
  }

  private void saveCachedEvent(final SFSFEmployeeTime event) {
    SFSFEmployeeTimeList cachedEventsForUser = employeeTimeRequestsPerUserCache.get(event.getUserId());
    if (cachedEventsForUser == null) {
      cachedEventsForUser = new SFSFEmployeeTimeList();
    }
    cachedEventsForUser.addEmployeeTime(event);
    employeeTimeRequestsPerUserCache.put(event.getUserId(), cachedEventsForUser);
  }

  private void deleteCachedEvent(final SFSFEmployeeTime event) {
    final SFSFEmployeeTimeList cachedEventsForUser = employeeTimeRequestsPerUserCache.get(event.getUserId());
    if (cachedEventsForUser != null) {
      cachedEventsForUser.removeEmployeeTime(event);
      employeeTimeRequestsPerUserCache.replace(event.getUserId(), cachedEventsForUser);
    }
  }

  Observable<SFSFEmployeeTime> onGetTimeOffError(final Throwable e, final String entityId) {
    if (e instanceof FileNotFoundException) {
      return Observable
          .error(new Exception(MessageFormat.format("EmployeeTime entity with id {0} is not found", entityId), e));
    } else {
      return Observable.error(e);
    }
  }

  boolean acceptEventStatus(final SFSFEmployeeTime timeOffEvent) {
    return ApprovalStatus.APPROVED == timeOffEvent.getApprovalStatus()
        || ApprovalStatus.CANCELLED == timeOffEvent.getApprovalStatus();
  }

  private String addRequestId(final String requestId, final String message) {
    return "[" + requestId + "] " + message;
  }
}
