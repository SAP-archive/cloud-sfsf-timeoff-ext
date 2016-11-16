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

import java.text.MessageFormat;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

@Component
public class Messages {
	private static final String AUTOREPLY_TEXT_TEMPLATE = "<p>Dear Sender,</p>"
			+ "<p>Please note that I will be on leave from {0} to {1}. During this time, I will have limited access to my email.</p>"
			+ "<p>For any urgent issue please contact my manager {2} {3}.</p>" + "Regards,<br>" + "{4} {5}";

	public String getAutoReplyMessage(SFSFEmployeeTime employeeTime) {
		final ZonedDateTime start = employeeTime.getStartDate();
		final ZonedDateTime end = employeeTime.getEndDate();
		final UserIdNav employeeUser = employeeTime.getUserIdNav();
		final UserIdNav managerUser = employeeTime.getUserIdNav().getJob().getManager();
		return MessageFormat.format(AUTOREPLY_TEXT_TEMPLATE, start.toLocalDate().toString(),
				end.toLocalDate().toString(), managerUser.getFirstName(), managerUser.getLastName(),
				employeeUser.getFirstName(), employeeUser.getLastName());
	}
}
