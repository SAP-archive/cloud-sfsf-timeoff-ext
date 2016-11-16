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
package com.sap.cloud.sfsf.powerweek;

import java.io.FileNotFoundException;
import java.text.MessageFormat;

import com.sap.cloud.sfsf.notification.EenAlertResponsePayload;
import com.sap.cloud.sfsf.notification.Events;
import com.sap.cloud.sfsf.notification.handler.NotificationHandler;
import com.sap.cloud.sfsf.powerweek.entity.SFSFEmployeeTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import rx.Observable;
import rx.schedulers.Schedulers;

@Component
public class TimeoffNotificationHandler implements NotificationHandler {

  static final String ERROR_STATUS_CODE = "500";
  static final String ERROR_MESSAGE = "Error processing EmployeeTime event: %s";

  private static final Logger logger = LoggerFactory.getLogger(TimeoffNotificationHandler.class);

  private final SFSFEmployeeTimeService sfsfEmployeeTimeSvc;

  @Autowired
  public TimeoffNotificationHandler(final SFSFEmployeeTimeService sfsfEmployeeTimeSvc) {
    this.sfsfEmployeeTimeSvc = sfsfEmployeeTimeSvc;
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
        .flatMap(key -> getTimeOffEventEntity(key.getValue())
            .onErrorResumeNext(e -> onGetTimeOffError(e, key.getValue()))
            .doOnNext(
                timeOffEvent -> logger.debug("EmployeeTime entity for userId {} received", timeOffEvent.getUserId()))
            .flatMap(timeOffEvent -> handleTimeOff(timeOffEvent)))
        .onErrorReturn(e -> {
          logger.error(ERROR_MESSAGE, e);
          final EenAlertResponsePayload response = new EenAlertResponsePayload();
          final String message = String.format(ERROR_MESSAGE, e.getMessage());
          response.setErrorMessage(message);
          response.setErrorCode(ERROR_STATUS_CODE);
          return response;
        });
    return obs;
  }

  // we call SuccessFactors to get the real timeoff event.
  private Observable<SFSFEmployeeTime> getTimeOffEventEntity(final String id) {
    return Observable.fromCallable(() -> sfsfEmployeeTimeSvc.getTimeoffEvent(id)).subscribeOn(Schedulers.io());
  }

  // we handle the timeoff event
  private Observable<EenAlertResponsePayload> handleTimeOff(final SFSFEmployeeTime timeOff) {
    // TODO: Implement your logic here
    //
    //
    final EenAlertResponsePayload responsePayload = new EenAlertResponsePayload();
    responsePayload.setStatus(201);
    responsePayload.setStatusDetails("Timeoff Event Handled");
    return Observable.just(responsePayload);

  }

  Observable<SFSFEmployeeTime> onGetTimeOffError(final Throwable e, final String entityId) {
    if (e instanceof FileNotFoundException) {
      return Observable
          .error(new Exception(MessageFormat.format("EmployeeTime entity with id {0} is not found", entityId), e));
    } else {
      return Observable.error(e);
    }
  }
}
