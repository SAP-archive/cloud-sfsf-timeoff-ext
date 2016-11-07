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

import java.time.OffsetDateTime;

import com.sap.cloud.sfsf.timeoff.TimeoffNotificationHandler;
import com.sap.cloud.sfsf.timeoff.entity.EmpJob;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.UserIdNav;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime.ApprovalStatus;

import org.junit.Before;
import org.junit.Test;

public class TimeoffNotificationHandlerPredicatesTest {

  private SFSFEmployeeTime timeOffEvent;
  private TimeoffNotificationHandler tnh;

  @Before
  public void before() {
    final UserIdNav userIdNav = new UserIdNav().setJob(new EmpJob().setTimezone("US/Eastern"));
    final OffsetDateTime dummyDate = OffsetDateTime.now();
    timeOffEvent = new SFSFEmployeeTime(dummyDate, dummyDate, userIdNav, dummyDate, dummyDate);
    tnh = new TimeoffNotificationHandler(null, null, null);
  }

  @Test
  public void testApproved() {
    // given
    timeOffEvent.setApprovalStatus(ApprovalStatus.APPROVED);

    // when
    final boolean isApproved = tnh.acceptEventStatus(timeOffEvent);

    // that
    assertThat(isApproved).describedAs("APPROVED is acceptable approved status").isTrue();
  }

  @Test
  public void testCanceled() {
    // given
    timeOffEvent.setApprovalStatus(ApprovalStatus.CANCELLED);

    // when
    final boolean isApproved = tnh.acceptEventStatus(timeOffEvent);

    // that
    assertThat(isApproved).describedAs("CANCELED is acceptable approved status").isTrue();
  }

  @Test
  public void testPending() {
    // given
    timeOffEvent.setApprovalStatus(ApprovalStatus.PENDING);

    // when
    final boolean isApproved = tnh.acceptEventStatus(timeOffEvent);

    // that
    assertThat(isApproved).describedAs("PENDING is not acceptable approved status").isFalse();
  }

  @Test
  public void testPendingCancellation() {
    // given
    timeOffEvent.setApprovalStatus(ApprovalStatus.PENDING_CANCELLATION);

    // when
    final boolean isApproved = tnh.acceptEventStatus(timeOffEvent);

    // that
    assertThat(isApproved).describedAs("PENDING_CANCELLATION is not acceptable approved status").isFalse();
  }

  @Test
  public void testRejected() {
    // given
    timeOffEvent.setApprovalStatus(ApprovalStatus.REJECTED);

    // when
    final boolean isApproved = tnh.acceptEventStatus(timeOffEvent);

    // that
    assertThat(isApproved).describedAs("REJECTED is not acceptable approved status").isFalse();
  }

}
