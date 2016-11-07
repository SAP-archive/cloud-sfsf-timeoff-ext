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

import java.io.IOException;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.sap.cloud.sfsf.timeoff.jackson.DateTimeTimeZoneDeserializer;
import com.sap.cloud.sfsf.timeoff.jackson.DateTimeTimeZoneSerializer;

import org.junit.Before;
import org.junit.Test;

public class DateTimeTimeZoneSerializerDeserializerTest {

  private static final ZonedDateTime ZONED_DATE_TIME =
      ZonedDateTime.parse("2016-04-29T14:32:48-07:00[America/Los_Angeles]");
  private static final String JSON_UTC_DATE =
      "{\"zdt\":{\"dateTime\":\"2016-04-29T14:32:48\",\"timeZone\":\"America/Los_Angeles\"}}";
  private ObjectMapper mapper;

  @Before
  public void before() {
    mapper = new ObjectMapper().registerModules(new ParameterNamesModule(Mode.PROPERTIES), new JavaTimeModule());
  }

  @Test
  public void testDeserializeWithTimezone() throws JsonParseException, JsonMappingException, IOException {
    // when
    final DTTZDeserializer result = mapper.readValue(JSON_UTC_DATE, DTTZDeserializer.class);

    // then
    assertThat(result.getZdt()).isEqualTo(ZONED_DATE_TIME);
  }

  @Test
  public void testSerializeWithTimezone() throws JsonParseException, JsonMappingException, IOException {
    // given
    final DTTZDeserializer obj = new DTTZDeserializer(ZONED_DATE_TIME);

    // when
    final String result = mapper.writeValueAsString(obj);

    // then
    assertThat(result).isEqualTo(JSON_UTC_DATE);
  }

}


class DTTZDeserializer {

  @JsonDeserialize(using = DateTimeTimeZoneDeserializer.class)
  @JsonSerialize(using = DateTimeTimeZoneSerializer.class)
  private final ZonedDateTime zdt;

  public DTTZDeserializer(final ZonedDateTime zdt) {
    this.zdt = zdt;
  }

  public ZonedDateTime getZdt() {
    return zdt;
  }
}
