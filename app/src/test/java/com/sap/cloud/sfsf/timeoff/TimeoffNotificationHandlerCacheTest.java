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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.OffsetDateTime;

import com.sap.cloud.sfsf.timeoff.EmployeeTimeEventHandler;
import com.sap.cloud.sfsf.timeoff.SFSFEmployeeTimeService;
import com.sap.cloud.sfsf.timeoff.TimeoffNotificationHandler;
import com.sap.cloud.sfsf.timeoff.entity.EmpJob;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTimeList;
import com.sap.cloud.sfsf.timeoff.entity.UserIdNav;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime.Action;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TimeoffNotificationHandlerCacheTest {


  private static final String TIMEOFF_ALIAS = "timeoff";
  private static Cache<String, SFSFEmployeeTimeList> cache;
  private static CacheManager cacheManager;

  private SFSFEmployeeTime employeeTime;
  private SFSFEmployeeTimeList employeeTimeList;

  @Mock
  private EmployeeTimeEventHandler eventHandlerMock;
  @Mock
  private SFSFEmployeeTimeService timeOffClientMock;

  private TimeoffNotificationHandler handler;

  @BeforeClass
  public static void beforeClass() {
    cacheManager =
        CacheManagerBuilder.newCacheManagerBuilder()
            .withCache(TIMEOFF_ALIAS,
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, SFSFEmployeeTimeList.class, ResourcePoolsBuilder.heap(20).build()))
            .build();
    cacheManager.init();
    cache = cacheManager.getCache(TIMEOFF_ALIAS, String.class, SFSFEmployeeTimeList.class);
  }

  @AfterClass
  public static void afterClass() {
    cacheManager.close();
  }

  @Before
  public void before() {
    final UserIdNav userIdNav = new UserIdNav().setJob(new EmpJob().setTimezone("US/Eastern"));
    final OffsetDateTime dummyDate = OffsetDateTime.now();
    employeeTime = new SFSFEmployeeTime(dummyDate, dummyDate, userIdNav, dummyDate, dummyDate);
    employeeTimeList = new SFSFEmployeeTimeList();
    employeeTime.setUserId("123456789");
    handler = new TimeoffNotificationHandler(eventHandlerMock, timeOffClientMock, cache);
  }

  @After
  public void after() {
    cache.clear();
  }


  @Test
  public void testNullAction() {
    // when
    final Throwable thrown = catchThrowable(() -> handler.updateCache(employeeTime));

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("Action cannot be null");

  }

  @Test
  public void testAddSameContentOnlyOnceAddCache() {
    testSuccessFull(Action.CREATE);
  }

  @Test
  public void testAddSameContentUpdateOnlyOnceAddCache() {
    testSuccessFull(Action.UPDATE);

  }

  private void testSuccessFull(final Action action) {
    // given
    employeeTime.setAction(action);
    employeeTimeList.addEmployeeTime(employeeTime);
    employeeTimeList.addEmployeeTime(employeeTime);
    cache.put("123456789", employeeTimeList);

    // when
    handler.updateCache(employeeTime);

    // that
    final SFSFEmployeeTimeList result = cache.get("123456789");
    assertThat(result.getResults()).containsExactly(employeeTime);
  }


  @Test
  public void testNothingInCachedCacheCreated() {
    testNothingInCachedCache(Action.CREATE);
  }

  @Test
  public void testNothingInCachedCacheUpdated() {
    testNothingInCachedCache(Action.UPDATE);
  }

  private void testNothingInCachedCache(final Action action) {
    // given
    employeeTime.setAction(action);

    // when
    handler.updateCache(employeeTime);

    // that
    final SFSFEmployeeTimeList result = cache.get("123456789");
    assertThat(result.getResults()).containsExactly(employeeTime);

  }

  @Test
  public void testDeleteNothingInCache() {
    // given
    employeeTime.setAction(Action.DELETE);

    // when
    handler.updateCache(employeeTime);

    assertThat(cache.iterator()).describedAs("Cache should be empty").isEmpty();

  }

  @Test
  public void testDeleteSuccessful() {
    // given
    employeeTime.setAction(Action.DELETE);
    employeeTimeList.addEmployeeTime(employeeTime);
    cache.put("123456789", employeeTimeList);
    // when
    handler.updateCache(employeeTime);

    assertThat(cache.get("123456789").getResults()).describedAs("Cache should be empty after deletion").isEmpty();

  }
}
