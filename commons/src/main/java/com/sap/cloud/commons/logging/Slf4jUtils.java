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
package com.sap.cloud.commons.logging;


import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;

public class Slf4jUtils {

  public static final String LOGGER_CTX_REQUEST_ID = "request_id";

  public static void setup() {
    final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    final LoggerContext loggerContext = root.getLoggerContext();

    final Appender<ILoggingEvent> stdoutApp = root.getAppender("STDOUT");
    if (stdoutApp != null) {
      final ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) stdoutApp;
      final PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
      consoleEncoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{50} - [%X{request_id}] %msg%n");
      consoleEncoder.setContext(loggerContext);
      consoleEncoder.start();
      consoleAppender.setEncoder(consoleEncoder);
      consoleAppender.start();
    }

    final Appender<ILoggingEvent> logApp = root.getAppender("LOGFILE");
    if (logApp != null) {
      final RollingFileAppender<ILoggingEvent> logFileAppender = (RollingFileAppender<ILoggingEvent>) logApp;
      final PatternLayoutEncoder filePatternLayout = new PatternLayoutEncoder();
      filePatternLayout
          .setPattern("%1d{yyyy MM dd HH:mm:ss}#%o#%p#%c#%a#%u#%t#%b#%z#%X{tenant_alias}#[%X{request_id}] %m%ex|%n");
      filePatternLayout.setContext(loggerContext);
      filePatternLayout.start();
      logFileAppender.setEncoder(filePatternLayout);
      logFileAppender.start();

    }
  }

}
