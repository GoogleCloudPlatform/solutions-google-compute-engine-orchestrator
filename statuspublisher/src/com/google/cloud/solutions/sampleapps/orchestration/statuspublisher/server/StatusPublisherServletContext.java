/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.google.cloud.solutions.sampleapps.orchestration.statuspublisher.server;

import java.lang.management.ManagementFactory;
import java.util.Timer;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Class for handling notification events about ServletContext lifecycle change.
 */
public class StatusPublisherServletContext implements ServletContextListener {
  private static final Logger logger = Logger.getLogger(StatusPublisherServletContext.class
      .getName());

  public static final String SYSTEM_STATS_REPORTER = "systemStatsReporter";

  private static final String START_DELAY_PARAM = "startCheckingDelay";
  private static final String INTERVAL_PARAM = "statusCheckInterval";
  private static final String MODE_PARAM = "mode";
  private static final String POLL_MODE = "poll";

  private Timer timer;

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    logger.info("Servlet context destroyed");
    if (timer != null) {
      logger.info("Cancelling the timer");
      timer.cancel();
    }
  }

  @Override
  public void contextInitialized(ServletContextEvent event) {
    logger.info("Servlet context initialized");
    String configFile = event.getServletContext().getRealPath("WEB-INF/config.xml");
    ConfigProperties configProperties = new ConfigProperties(configFile);

    SystemStatsReporter reporter = new SystemStatsReporter(
        ManagementFactory.getOperatingSystemMXBean(), new MemStatsReporter(configProperties));
    event.getServletContext().setAttribute(SYSTEM_STATS_REPORTER, reporter);

    startPolling(configProperties, reporter);
  }

  /** If Poll mode is specified, the status checker calls the application at a regular interval
      to get the application status.
      @param configProperties configuration properties
      @param SystemStatsReporter reporter for the timer task to report stats
   */
  void startPolling(ConfigProperties configProperties, SystemStatsReporter reporter) {
    if (configProperties.getConfigProperties().get(MODE_PARAM).equalsIgnoreCase(POLL_MODE)) {
      int startDelay = Integer.valueOf(
          configProperties.getConfigProperties().get(START_DELAY_PARAM));
      int interval = Integer.valueOf(configProperties.getConfigProperties().get(INTERVAL_PARAM));
      logger.info("Starting the timer with delay " + startDelay + " and interval " + interval);
      timer = new Timer();
      timer.scheduleAtFixedRate(new StatusPublisherAppStatusChecker(reporter, configProperties),
          startDelay, interval);
    }
  }

  /**
    For Testing Only.
   */
  Timer getTimer() {
    return timer;
  }
}
