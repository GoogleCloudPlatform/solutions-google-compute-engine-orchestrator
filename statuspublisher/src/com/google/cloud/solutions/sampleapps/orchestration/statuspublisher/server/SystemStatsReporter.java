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

import com.google.cloud.solutions.sampleapps.orchestration.common.GceApplicationStats;
import com.google.cloud.solutions.sampleapps.orchestration.common.GceCurrentStatus;
import com.google.cloud.solutions.sampleapps.orchestration.common.GceStats;
import com.google.cloud.solutions.sampleapps.orchestration.common.GceSystemStats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.lang.management.OperatingSystemMXBean;
import java.util.logging.Logger;

/**
 * Class for reporting the system statistics.
 */
public class SystemStatsReporter {
  private static final Logger logger = Logger.getLogger(SystemStatsReporter.class.getName());

  private GceApplicationStats applicationStats;
  private OperatingSystemMXBean operatingSystemMXBean;
  private MemStatsReporter memStatsReporter;

  public SystemStatsReporter(OperatingSystemMXBean operatingSystemMXBean,
      MemStatsReporter memStatsReporter) {
    this.operatingSystemMXBean = operatingSystemMXBean;
    this.memStatsReporter = memStatsReporter;
  }

  /**
   * @return a GceApplicationStats object
   */
  public GceStats getStats() {
    logger.info("Getting GCE instance stats");
    GceSystemStats systemStats = new GceSystemStats(operatingSystemMXBean.getAvailableProcessors(),
        operatingSystemMXBean.getSystemLoadAverage(), memStatsReporter.getUsedMemoryRatio());
    synchronized (this) {
      return new GceStats(GceCurrentStatus.getCurrentStatus(), systemStats, applicationStats);
    }
  }

  /**
   * Sets the application stats.
   *
   * @param stat a GceApplicationStats object
   */
  public synchronized void setApplicationStats(GceApplicationStats stat) {
    applicationStats = stat;
  }

  /**
   * Sets the application stats.
   *
   * @param reader input reader that contains the Json representation of GceApplication.
   *        Note: The data format is in yyyy-MM-dd HH:mm:ss.
   */
  public void setApplicationStats(BufferedReader reader) {
    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    setApplicationStats(gson.fromJson(reader, GceApplicationStats.class));
    logger.info("Setting application stats from BufferedReader " + gson.toJson(applicationStats));
  }

  /**
   * For unit testing only.
   */
  GceApplicationStats getGceApplicationStats() {
    return applicationStats;
  }
}
