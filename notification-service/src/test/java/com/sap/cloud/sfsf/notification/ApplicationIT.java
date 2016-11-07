/*
 * Copyright 2014-2015 the original author or authors.
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

package com.sap.cloud.sfsf.notification;

import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.bind.JAXBElement;

import com.sap.cloud.sfsf.notification.ExternalEvent;
import com.sap.cloud.sfsf.notification.ExternalEventResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.MessageDispatcherServlet;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationIT {

  private final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();

  @LocalServerPort
  private int port;

  @Before
  public void init() throws Exception {
    marshaller.setPackagesToScan("com.sap.cloud.sfsf.notification");
    marshaller.afterPropertiesSet();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSendAndReceive() {
    final ExternalEvent request = new ExternalEvent();
    final JAXBElement<ExternalEventResponse> result = (JAXBElement<ExternalEventResponse>) new WebServiceTemplate(marshaller).marshalSendAndReceive("http://localhost:" + port + "/test/", request);
    assertThat(result.getValue()).describedAs("WS should return a result").isNotNull();
  }
}


@SpringBootApplication
class TestConfig {

  @Bean
  public ServletRegistrationBean messageDispatcherServlet(final ApplicationContext applicationContext) {
    final MessageDispatcherServlet servlet = new MessageDispatcherServlet();
    servlet.setApplicationContext(applicationContext);
    servlet.setTransformWsdlLocations(true);
    return new ServletRegistrationBean(servlet, "/test/*");
  }
}
