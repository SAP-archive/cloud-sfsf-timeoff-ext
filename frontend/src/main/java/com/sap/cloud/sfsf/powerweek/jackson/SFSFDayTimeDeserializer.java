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
package com.sap.cloud.sfsf.powerweek.jackson;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class SFSFDayTimeDeserializer extends JsonDeserializer<OffsetDateTime>{

  private static final Pattern pattern = Pattern.compile("\\/Date\\((\\d+)([-+]\\d+)?\\)\\/");

  @Override
  public OffsetDateTime deserialize(final JsonParser p, final DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    final String jsonDate = ctxt.readValue(p, String.class);
    final Matcher matcher = pattern.matcher(jsonDate);
    if (!matcher.find()) {
      throw new IOException("Invalid DateTime format " + jsonDate);
    }
    final long millis = Long.parseLong(matcher.group(1));
    String offset = matcher.group(2);
    // default in case no time zone
    if (offset == null) {
      offset = "+0000";
    }

    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.of(offset));
  }

}
