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

import java.util.Calendar;
import java.util.Date;

/**
 * Class that encapsulates performance statistics about an application.
 */
public class GceApplicationStats {

  private int numTasksProcessing;
  private int numTasksProcessedLastMin;
  private Date lastLeasedDate;

  public GceApplicationStats() {
    numTasksProcessing = -1;
    numTasksProcessedLastMin = -1;

    // Initialize last leased date to a year from now.
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.YEAR, 1);
    lastLeasedDate = cal.getTime();
  }

  /**
   * @return the number of tasks currently being processed.
   */
  public int getNumTasksProcessing() {
    return numTasksProcessing;
  }

  /**
   * @return the number of tasks processed in the last minute.
   */
  public int getNumTasksProcessedLastMin() {
    return numTasksProcessedLastMin;
  }

  /**
   * @return the last time a task was leased.
   */
  public Date getLastLeasedDate() {
    return lastLeasedDate;
  }
}
