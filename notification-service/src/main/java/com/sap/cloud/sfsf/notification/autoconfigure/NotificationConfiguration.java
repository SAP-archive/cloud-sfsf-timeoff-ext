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
package com.sap.cloud.sfsf.notification.autoconfigure;

import java.util.List;

import com.sap.cloud.sfsf.notification.endpoint.EventEndpoint;
import com.sap.cloud.sfsf.notification.handler.NotificationHandler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import rx.Observable;

@EnableWs
@Configuration
public class NotificationConfiguration {

  @ConditionalOnMissingBean(name="notification")
  @Bean(name = "notification")
  public DefaultWsdl11Definition defaultWsdl11Definition(final XsdSchema countriesSchema) {
      final DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
      wsdl11Definition.setPortTypeName("NotifyImplPort");
      wsdl11Definition.setLocationUri("/services");
      wsdl11Definition.setTargetNamespace("http://notification.event.successfactors.com");
      wsdl11Definition.setSchema(countriesSchema);
      return wsdl11Definition;
  }

  @ConditionalOnMissingBean
  @Bean
  public XsdSchema responseSchema() {
      return new SimpleXsdSchema(new ClassPathResource("response.xsd"));
  }

  @ConditionalOnMissingBean
  @Bean
  NotificationHandler notificationHandler() {
    return (events, requestId) -> Observable.empty();
  }

  @ConditionalOnMissingBean
  @Bean
  public EventEndpoint eventEndpoint(final List<NotificationHandler> notificationHandlers) {
    return new EventEndpoint(notificationHandlers);
  }

}
