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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that encapsulates performance statistics about the GCE instance.
 */
public class GceSystemStats {
  private static final Logger logger = Logger.getLogger(GceSystemStats.class.getName());

  private double numProcessors;
  private double cumSystemLoad;
  private double aveSystemLoad;
  private float memUsedRatio;

  public GceSystemStats() {
    numProcessors = -1.0;
    cumSystemLoad = -1.0;
    aveSystemLoad = -1.0;
    memUsedRatio = -1.0f;
  }

  public GceSystemStats(double numProcessors, double cumSystemLoad, float memUsedRatio) {
    this.numProcessors = numProcessors;
    this.cumSystemLoad = cumSystemLoad;
    this.memUsedRatio = memUsedRatio;
    if (numProcessors > 0) {
      this.aveSystemLoad = cumSystemLoad / numProcessors;
    } else {
      logger.log(Level.WARNING, "numProcessors is incorrect " + numProcessors);
    }
  }

  /**
   * @return the number of processors.
   */
  public double getNumProcessors() {
    return numProcessors;
  }

  /**
   * @return the cumulative system load average.
   */
  public double getCumSystemLoad() {
    return cumSystemLoad;
  }

  /**
   * @return the system's average load.
   */
  public double getAveSystemLoad() {
    return aveSystemLoad;
  }

  /**
   * @return the memory used / memory total ratio.
   */
  public float getMemUsedRatio() {
    return memUsedRatio;
  }
}
