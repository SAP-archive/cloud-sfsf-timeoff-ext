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
package com.sap.cloud.sfsf.timeoff;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.naming.ConfigurationException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.commons.http.HttpConnector;
import com.sap.cloud.commons.http.HttpUrlConnectionConnector;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTime;
import com.sap.cloud.sfsf.timeoff.entity.SFSFEmployeeTimeList;
import com.sap.cloud.sfsf.timeoff.jackson.DefaultMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SFSFEmployeeTimeService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SFSFEmployeeTimeService.class);

  private static final String SAP_HCMCLOUD_CORE_ODATA_DESTINATION_NAME = "sap_hcmcloud_core_odata";

  private final UriComponentsBuilder SFSF_SVC_API_EMPLOYEETIME_ENTITY = UriComponentsBuilder.newInstance()
			 // employee path
	        .path("/EmployeeTime('{entityId}')")
	        // expand
	        .queryParam("$expand",
			"userIdNav/empInfo/jobInfoNav/managerUserNav")
	        .queryParam("$select",
	            "externalCode,approvalStatus,comment,createdDateTime,lastModifiedDateTime,startDate,endDate,timeType,userId,"
		      + "userIdNav/firstName,userIdNav/lastName,userIdNav/email,userIdNav/timeZone,"
		      + "userIdNav/empInfo/jobInfoNav/managerUserNav/firstName,userIdNav/empInfo/jobInfoNav/managerUserNav/lastName,userIdNav/empInfo/jobInfoNav/managerUserNav/email,"
		      + "userIdNav/empInfo/jobInfoNav/timezone")
	        // order
	        .query("$orderby=startDate,userIdNav/empInfo/jobInfoNav/startDate,seqNumber desc")
	        // only 1
	        .query("$top=1");

  private final UriComponentsBuilder SFSF_SVC_API_EMPLOYEETIME_ENTITY_COLLECTION = UriComponentsBuilder.newInstance()
	        // employee path
	        .path("/EmployeeTime")
	        .queryParam("$select",
		            "externalCode,approvalStatus,comment,createdDateTime,lastModifiedDateTime,startDate,endDate,timeType,userId,"
			      + "userIdNav/firstName,userIdNav/lastName,userIdNav/email,userIdNav/timeZone,"
			      + "userIdNav/empInfo/jobInfoNav/managerUserNav/firstName,userIdNav/empInfo/jobInfoNav/managerUserNav/lastName,userIdNav/empInfo/jobInfoNav/managerUserNav/email,"
			      + "userIdNav/empInfo/jobInfoNav/timezone")
	        // filter
	        .queryParam("$filter",
	            "userId eq '{userId}' and approvalStatus eq 'APPROVED' and endDate gt datetime'{dateTime}'")
	        // order
	        .query("$orderby=startDate desc")
	        // expand
	        .query("$expand=userIdNav/empInfo/jobInfoNav/managerUserNav")
	        // only first 5
	        .query("$top=5");

  private final HttpConnector httpConnector = HttpUrlConnectionConnector.forDestinationName(SAP_HCMCLOUD_CORE_ODATA_DESTINATION_NAME);

  private final ObjectMapper unwrappingMapper = DefaultMapper.newMapper()
      .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);

  public SFSFEmployeeTime getTimeoffEvent(final String entityId) throws IOException {

	final UriComponents uriComponents = SFSF_SVC_API_EMPLOYEETIME_ENTITY.buildAndExpand(entityId).encode();

    final String requestUriString = uriComponents.toUriString();
    final String employeeTimeJson = httpConnector.get(requestUriString);
    LOGGER.debug("Got data for EmployeeTime('{}'): {}", entityId, employeeTimeJson);

    return unwrappingMapper.readValue(employeeTimeJson, SFSFEmployeeTime.class);

  }

  public SFSFEmployeeTimeList getLatestTimeOffEventForUser(final String userId) throws JsonParseException, JsonMappingException, IOException, ConfigurationException{
	final String zonedNowInstantString = OffsetDateTime.now(ZoneOffset.UTC).toString();
    final UriComponents requestUriString = SFSF_SVC_API_EMPLOYEETIME_ENTITY_COLLECTION.buildAndExpand(userId, zonedNowInstantString).encode();

    final String employTimeEntitiesJson = httpConnector.get(requestUriString.toUriString());
    LOGGER.debug("Got data for EmployeeTime: {}", employTimeEntitiesJson);

    return unwrappingMapper.readValue(employTimeEntitiesJson, SFSFEmployeeTimeList.class);
  }
}
