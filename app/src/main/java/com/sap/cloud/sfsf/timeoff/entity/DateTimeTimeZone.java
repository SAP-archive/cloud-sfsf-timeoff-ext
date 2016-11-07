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

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class DateTimeTimeZone {

  @JsonSerialize(using = ToStringSerializer.class)
  private final LocalDateTime dateTime;

  private final ZoneId timeZone;

  public DateTimeTimeZone(final LocalDateTime dateTime, final ZoneId timeZone) {
    this.dateTime = dateTime;
    this.timeZone = timeZone;
  }

  public ZoneId getTimeZone() {
    return timeZone;
  }


  public LocalDateTime getDateTime() {
    return dateTime;
  }

}
