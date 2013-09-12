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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A timer task that get stats from the application and set it in the SystemStatsReporter.
 */
public class StatusPublisherAppStatusChecker extends TimerTask {
  private static final Logger logger = Logger.getLogger(
      StatusPublisherAppStatusChecker.class.getName());

  private SystemStatsReporter reporter;
  private ConfigProperties configProperties;

  public StatusPublisherAppStatusChecker(SystemStatsReporter reporter,
      ConfigProperties configProperties) {
    this.reporter = reporter;
    this.configProperties = configProperties;
  }

  @SuppressWarnings("unused")
  private StatusPublisherAppStatusChecker() {
  }

  private static final String APP_STATUS_URL_PARAM = "appStatusUrl";

  @Override
  public void run() {
    logger.info("StatusPublisherAppStatusChecker is checking app status");
    try {
      URL url = new URL(configProperties.getConfigProperties().get(APP_STATUS_URL_PARAM));
      BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
      reporter.setApplicationStats(reader);
      reader.close();
    } catch (MalformedURLException e) {
      logger.log(Level.SEVERE, "MalformedURLException encountered " + e.getMessage());
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to get status" + e.getMessage());
    }
  }
}
