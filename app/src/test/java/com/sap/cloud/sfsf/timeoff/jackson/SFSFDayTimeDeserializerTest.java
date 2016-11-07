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
package com.sap.cloud.sfsf.timeoff.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.sap.cloud.sfsf.timeoff.jackson.SFSFDayTimeDeserializer;

import org.junit.Before;
import org.junit.Test;

public class SFSFDayTimeDeserializerTest {

  private static final String UTC_DATE = "2016-04-29T14:32:48Z";

  private ObjectMapper mapper;

  @Before
  public void before() {
    mapper = new ObjectMapper().registerModules(new ParameterNamesModule(Mode.PROPERTIES), new JavaTimeModule());
  }

  @Test
  public void testDeserializeWithTimezone() throws JsonParseException, JsonMappingException, IOException {
    // given
    final String jsonUTCDate = "{\"zdt\":\"/Date(1461940368000+0000)/\"}";

    // when
    final DateTimeSerializerTest result = mapper.readValue(jsonUTCDate, DateTimeSerializerTest.class);

    // then
    assertThat(result.getZdt()).isEqualTo(OffsetDateTime.parse(UTC_DATE));
  }

  @Test
  public void testDeserializeWithoutTimezone() throws JsonParseException, JsonMappingException, IOException {
    // given
    final String jsonUTCDate = "{\"zdt\":\"/Date(1461940368000)/\"}";

    // when
    final DateTimeSerializerTest result = mapper.readValue(jsonUTCDate, DateTimeSerializerTest.class);

    // then
    assertThat(result.getZdt()).isEqualTo(OffsetDateTime.parse(UTC_DATE));
  }

  @Test
  public void testMalformedDateParseException() throws JsonParseException, JsonMappingException, IOException {
    // given
    final String jsonMalformed = "{\"zdt\":\"/malformed/\"}";

    // when
    final Throwable thrown = catchThrowable(() -> mapper.readValue(jsonMalformed, DateTimeSerializerTest.class));

    // then
    assertThat(thrown).isInstanceOf(IOException.class).hasMessage("Invalid DateTime format /malformed/");
  }
}


class DateTimeSerializerTest {

  @JsonDeserialize(using = SFSFDayTimeDeserializer.class)
  private final OffsetDateTime zdt;

  public DateTimeSerializerTest(final OffsetDateTime zdt) {
    this.zdt = zdt;
  }

  public OffsetDateTime getZdt() {
    return zdt;
  }


}
