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

import java.util.concurrent.TimeUnit;

import com.sap.cloud.sfsf.timeoff.entity.CalendarEventsList;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTimeList;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import rx.Observable;

@Configuration
public class TimeoffAppConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(TimeoffAppConfig.class);

  @Bean
  @ConditionalOnMissingBean
  CalendarServiceProvider calendarServiceProvider() {
    return new DummyCalendarServiceProvider();
  }

  @Bean
  Cache<String, SFSFEmployeeTimeList> employeeTimeRequestsPerUserCache() {
    final CacheConfiguration<String, SFSFEmployeeTimeList> cacheConfiguration = CacheConfigurationBuilder
        .newCacheConfigurationBuilder(String.class, SFSFEmployeeTimeList.class, ResourcePoolsBuilder.heap(10).build())
        .withExpiry(Expirations.timeToLiveExpiration(new Duration(30, TimeUnit.MINUTES))).build();
    final String cacheAlias = "timeoff";
    final CacheManager cacheManager =
        CacheManagerBuilder.newCacheManagerBuilder().withCache(cacheAlias, cacheConfiguration).build();
    cacheManager.init();
    final Cache<String, SFSFEmployeeTimeList> cache =
        cacheManager.getCache(cacheAlias, String.class, SFSFEmployeeTimeList.class);
    LOGGER.trace(cacheAlias + " cache created");
    return cache;
  }



  class DummyCalendarServiceProvider implements CalendarServiceProvider {

    @Override
    public Observable<Void> update(final SFSFEmployeeTime employeeTime, final String requestId) {
      return Observable.empty();
    }

    @Override
    public Observable<CalendarEventsList> getConflicting(final SFSFEmployeeTimeList employeeTime) {
      return Observable.empty();
    }

    @Override
    public Observable<Void> create(final SFSFEmployeeTime employeeTime, final String requestId) {
      return Observable.empty();
    }

    @Override
    public Observable<Void> cancel(final SFSFEmployeeTime employeeTime, final String requestId) {
      return Observable.empty();
    }
  };
}
