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

package com.google.cloud.solutions.sampleapps.orchestration.common;

import com.google.cloud.solutions.sampleapps.orchestration.common.GceCurrentStatus.Status;

/**
 * Class that encapsulates a Compute Engine instance statistics for auto-scaling.
 */
public class GceStats {
  private Status currentStatus;
  private GceSystemStats systemStats;
  private GceApplicationStats applicationStats;

  @SuppressWarnings("unused")
  private GceStats() {}

  /**
   * Creates a GceStats object with a given status.
   *
   * @param status the status (running, preparing to shut down, or ready to shut down).
   */
  public GceStats(Status status) {
    currentStatus = status;
    this.systemStats = new GceSystemStats();
    this.applicationStats = new GceApplicationStats();
  }

  /**
   * Creates a GceStats object with passed-in status, systemStats, and applicationStats.
   *
   * @param status the status (running, preparing to shut down, or ready to shut down).
   * @param systemStats the system status.
   * @param applicationStats the application status.
   */
  public GceStats(Status status, GceSystemStats systemStats, GceApplicationStats applicationStats) {
    currentStatus = status;
    this.systemStats = systemStats;
    this.applicationStats = applicationStats;
  }

  /**
   * Gets the current status, e.g., preparing to shut down.
   *
   * @return the current status.
   */
  public Status getCurrentStatus() {
    return currentStatus;
  }

  /**
   * Gets the system statistics, e.g., the number of processors.
   *
   * @return the system statistics.
   */
  public GceSystemStats getSystemStats() {
    return systemStats;
  }

  /**
   * Gets the application statistics, e.g., how many tasks were processed in the last minute.
   *
   * @return the application statistics.
   */
  public GceApplicationStats getApplicationStats() {
    return applicationStats;
  }
}
