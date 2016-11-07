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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sap.cloud.sfsf.timeoff.jackson.EmpJobDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserIdNav {

  private String email;
  private String firstName;
  private String lastName;
  private String timeZone;

  @JsonDeserialize(using = EmpJobDeserializer.class)
  @JsonProperty("empInfo")
  private EmpJob job;

  public UserIdNav() {}

  public String getEmail() {
    return email;
  }

  public UserIdNav setEmail(final String email) {
    this.email = email;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public UserIdNav setFirstName(final String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public UserIdNav setLastName(final String lastName) {
    this.lastName = lastName;
    return this;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public UserIdNav setTimeZone(final String timeZone) {
    this.timeZone = timeZone;
    return this;
  }

  public EmpJob getJob() {
    return job;
  }

  public UserIdNav setJob(final EmpJob job) {
    this.job = job;
    return this;
  }

}
