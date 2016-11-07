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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sap.cloud.sfsf.timeoff.jackson.EmpJobDeserializer;

@JsonDeserialize(using = EmpJobDeserializer.class)
public class EmpJob {

	private UserIdNav manager;
	private String timezone;

	public UserIdNav getManager() {
		return manager;
	}
	public EmpJob setManager(final UserIdNav manager) {
		this.manager = manager;
		return this;
	}
	public String getTimezone() {
		return timezone;
	}
	public EmpJob setTimezone(final String timezone) {
		this.timezone = timezone;
		return this;
	}

}
