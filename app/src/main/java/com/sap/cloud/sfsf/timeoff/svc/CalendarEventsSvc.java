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

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sap.cloud.sfsf.timeoff.CalendarServiceProvider;
import com.sap.cloud.sfsf.timeoff.SFSFEmployeeTimeService;
import com.sap.cloud.sfsf.timeoff.entity.CalendarEvent;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTimeList;
import com.sap.cloud.sfsf.timeoff.entity.CalendarEvent.EventStatus;

import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import rx.Observable;


@RestController
@RequestMapping("/api/v1/conflicting")
public class CalendarEventsSvc {

  private static final Logger LOGGER = LoggerFactory.getLogger(CalendarEventsSvc.class);

  private final SFSFEmployeeTimeService sfsfEmployeeTimeSvc;
  private final Observable<CalendarServiceProvider> calendarSvc;
  private final Cache<String, SFSFEmployeeTimeList> cache;

  @Autowired
  public CalendarEventsSvc(final SFSFEmployeeTimeService sfsfEmployeeTimeSvc,
      final List<CalendarServiceProvider> calendarSvc,
      final Cache<String, SFSFEmployeeTimeList> employeeTimeRequestsPerUserCache) {
    this.sfsfEmployeeTimeSvc = sfsfEmployeeTimeSvc;
    this.calendarSvc = Observable.from(calendarSvc);
    cache = employeeTimeRequestsPerUserCache;
  }

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<CalendarEvent> getConflictingEvents(final Principal principal) {
    final String userId = principal.getName();

    LOGGER.debug("Getting employee time for user {}", userId);
    return getEmpoyeeTime(userId).filter(e -> {
      return !Objects.isNull(e);
    }).filter(e -> {
      return !e.getResults().isEmpty();
    }).doOnNext(event -> {
      cache.put(userId, event);
    }).flatMap(event -> {
      return calendarSvc.flatMap(service -> {
        return service.getConflicting(event);
      });
    }).flatMapIterable(dto -> {
      return dto.getValue();
    }).filter(event -> {
      return EventStatus.oof != event.getShowAs();
    }).toList().doOnError(e -> {
      throw new ConflictingEventsException(e.getMessage());
    }).toBlocking().firstOrDefault(new ArrayList<>());
  }

  private Observable<SFSFEmployeeTimeList> getEmpoyeeTime(final String userId) {
    // enable this later on
    // return getEmpoyeeTimeCache(userId).concatWith(getEmpoyeeTimeIO(userId)).take(1);
    return getEmpoyeeTimeIO(userId);
    // return getEmpoyeeTimeCache(userId);

  }

  private Observable<SFSFEmployeeTimeList> getEmpoyeeTimeCache(final String userId) {
    final SFSFEmployeeTimeList event = cache.get(userId);
    if (event == null) {
      return Observable.empty();
    }
    return Observable.just(event);
  }

  private Observable<SFSFEmployeeTimeList> getEmpoyeeTimeIO(final String userId) {
    return Observable.fromCallable(() -> sfsfEmployeeTimeSvc.getLatestTimeOffEventForUser(userId))
        /* .subscribeOn(Schedulers.io()) */.doOnError(e -> LOGGER.error("Getting latest event failed ", e));
  }

}
