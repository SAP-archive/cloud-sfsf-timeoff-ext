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

import static org.assertj.core.api.Assertions.assertThat;

import com.sap.cloud.sfsf.notification.EenAlertResponsePayload;
import com.sap.cloud.sfsf.notification.Events;
import com.sap.cloud.sfsf.notification.autoconfigure.NotificationConfiguration;
import com.sap.cloud.sfsf.notification.handler.NotificationHandler;

import org.junit.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import rx.Observable;

public class NotificationConfigurationTest {


  private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  @Test
  public void defaultNoticationConfiguration() throws Exception {
    registerAndRefresh(NotificationConfiguration.class);

    assertThat(context.getBean(NotificationHandler.class)).isNotNull();
  }


  @Test
  public void customNotificationHandler() {
    registerAndRefresh(CustomNotificationHandler.class, NotificationConfiguration.class);

    assertThat(context.getBean(NotificationHandler.class)).isInstanceOf(TestNotificationHandler.class);
  }


  @TestConfiguration
  protected static class CustomNotificationHandler {

    @Bean
    public NotificationHandler testHandler() {
      return new TestNotificationHandler();
    };

  }

  protected static class TestNotificationHandler implements NotificationHandler {

    @Override
    public Observable<EenAlertResponsePayload> onNotification(final Events events, final String requestId) {
      return null;
    }
  }

  private void registerAndRefresh(final Class<?>... annotatedClasses) {
    context.register(annotatedClasses);
    context.refresh();
  }
}
