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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.sap.cloud.sfsf.timeoff.entity.EmpJob;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.UserIdNav;

import org.junit.Before;
import org.junit.Test;

public class SFSFEmployeeTimeTest {


  private SFSFEmployeeTime empTime;
  private EmpJob empJob;
  private UserIdNav userIdNav;

  @Before
  public void before() {
    empJob = new EmpJob();
    userIdNav = new UserIdNav().setJob(empJob);

  }

  @Test
  public void oneDayIsAddedToEndTime() {
    // given
    final OffsetDateTime endDT = OffsetDateTime.parse("2016-05-30T00:00:00Z");

    // when
    empTime = new SFSFEmployeeTime(OffsetDateTime.parse("2016-05-20T00:00:00Z"), endDT, userIdNav, endDT, endDT);

    // then
    assertThat(empTime.getEndDate()).describedAs("One day needs to be added to endDate to compensate for SFSF issue")
        .isEqualTo(ZonedDateTime.parse("2016-05-31T00:00:00Z[UTC]"));
  }

  @Test
  public void zonedStartDateIsNotChanged() throws ParseException {
    // given
    final OffsetDateTime startDate = OffsetDateTime.parse("2016-05-30T00:00:00Z");
    empJob.setTimezone("PST");

    // when
    empTime = new SFSFEmployeeTime(startDate, startDate, userIdNav, startDate, startDate);

    // then
    final ZonedDateTime result = empTime.getStartDate();
    assertThat(result).describedAs("Only the zone should be added. The time should not be changed")
        .isEqualTo(ZonedDateTime.parse("2016-05-30T00:00-07:00[America/Los_Angeles]"));
  }

  @Test
  public void zonedEndDateOneDayAdded() throws ParseException {
    // given
    final OffsetDateTime endDate = OffsetDateTime.parse("2016-05-30T00:00:00Z");
    empJob.setTimezone("PST");

    // when
    empTime = new SFSFEmployeeTime(endDate, endDate, userIdNav, endDate, endDate);

    // then
    final ZonedDateTime result = empTime.getEndDate();
    assertThat(result).describedAs("Only the zone should be added. The time should not be changed")
        .isEqualTo(ZonedDateTime.parse("2016-05-31T00:00-07:00[America/Los_Angeles]"));
  }

  @Test
  public void testSerlialization() throws JsonParseException, JsonMappingException, IOException {
    // given
    final ObjectMapper mapper =
        new ObjectMapper().registerModules(new ParameterNamesModule(Mode.PROPERTIES), new JavaTimeModule())
            .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
    final URL jsonFile = Thread.currentThread().getContextClassLoader().getResource("employee_time_data.json");

    // when
    final SFSFEmployeeTime result = mapper.readValue(jsonFile, SFSFEmployeeTime.class);

    // then
    assertThat(result.getStartDate()).isEqualTo(ZonedDateTime.parse("2016-05-30T00:00:00-04:00[US/Eastern]"));
    assertThat(result.getEndDate()).isEqualTo(ZonedDateTime.parse("2016-06-01T00:00:00-04:00[US/Eastern]"));
    assertThat(result.getCreatedDateTime()).isEqualTo(ZonedDateTime.parse("2016-05-11T09:40:14Z[US/Eastern]"));
    assertThat(result.getLastModifiedDateTime()).isEqualTo(ZonedDateTime.parse("2016-05-11T09:40:14Z[US/Eastern]"));
  }
}
