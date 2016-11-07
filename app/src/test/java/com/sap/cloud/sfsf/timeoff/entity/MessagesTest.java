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
package com.sap.cloud.sfsf.timeoff.entity;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;

import com.sap.cloud.sfsf.timeoff.entity.EmpJob;
import com.sap.cloud.sfsf.timeoff.entity.Messages;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.UserIdNav;

import org.junit.Test;

public class MessagesTest {

	private static final String SFSF_EMP_TIME_CREATED_STR = "2016-04-21T00:00-04:00";
	private static final String SFSF_EMP_TIME_START_STR = "2016-07-13T00:00:00-04:00";
	private static final String SFSF_EMP_TIME_END_STR = "2016-07-17T00:00:00-04:00";

	@Test
	public void autoReplyMessageTest() {

		// given
		EmpJob empJob = new EmpJob();
		empJob.setTimezone("US/Eastern");
		final UserIdNav manager = new UserIdNav();
		manager.setFirstName("managerFirst");
		manager.setLastName("managerLast");
		empJob.setManager(manager);
		UserIdNav userIdNav = new UserIdNav().setJob(empJob);
		userIdNav.setEmail("test@test.com");
		userIdNav.setFirstName("testFirst");
		userIdNav.setLastName("testLast");
		final OffsetDateTime dateUpdated = OffsetDateTime.parse(SFSF_EMP_TIME_CREATED_STR);
		final OffsetDateTime dateCreated = OffsetDateTime.parse(SFSF_EMP_TIME_CREATED_STR);
		final OffsetDateTime startDate = OffsetDateTime.parse(SFSF_EMP_TIME_START_STR);
		final OffsetDateTime endDate = OffsetDateTime.parse(SFSF_EMP_TIME_END_STR);
		SFSFEmployeeTime empTime = new SFSFEmployeeTime(startDate, endDate, userIdNav, dateCreated, dateUpdated);

		// when
		String result = new Messages().getAutoReplyMessage(empTime);

		// then
		assertEquals(
				"<p>Dear Sender,</p><p>Please note that I will be on leave from 2016-07-13 to 2016-07-18. During this time, I will have limited access to my email.</p><p>For any urgent issue please contact my manager managerFirst managerLast.</p>Regards,<br>testFirst testLast",
				result);
	}
}
