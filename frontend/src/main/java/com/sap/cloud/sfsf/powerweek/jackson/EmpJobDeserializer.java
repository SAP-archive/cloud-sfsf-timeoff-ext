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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.sfsf.powerweek.entity.EmpJob;
import com.sap.cloud.sfsf.powerweek.entity.UserIdNav;

public class EmpJobDeserializer extends JsonDeserializer<EmpJob> {

  private final ObjectMapper mapper = DefaultMapper.newMapper();

  public EmpJobDeserializer() {}

  @Override
  public EmpJob deserialize(final JsonParser p, final DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    final TreeNode root = mapper.readTree(p);

    final TreeNode jobInfo = root.get("jobInfoNav");
    if (jobInfo == null) {
      return null;
    }
    final TreeNode results = jobInfo.get("results");
    if (results == null || !results.isArray()) {
      return null;
    }
    final TreeNode singleResult = results.path(0);
    if (singleResult == null) {
      return null;
    }

    final EmpJob job = new EmpJob();

    final TreeNode managerNav = singleResult.get("managerUserNav");
    if (managerNav != null) {
      final UserIdNav manager = mapper.readValue(managerNav.traverse(), UserIdNav.class);
      job.setManager(manager);
    }

    final TreeNode timezoneNode = singleResult.get("timezone");
    if (timezoneNode != null) {
      final String timezone = mapper.readValue(timezoneNode.traverse(), String.class);
      job.setTimezone(timezone);
    }

    return job;
  }

}
