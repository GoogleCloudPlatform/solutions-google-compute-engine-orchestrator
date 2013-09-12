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

/**
 * The current status.
 */
public class GceCurrentStatus {
  private static Status currentStatus = Status.RUNNING;

  /**
   * Possible status the VM can be in.
   */
  public enum Status {
    /*
     * VM is running and leasing tasks.
     */
    RUNNING,
    /*
     * VM has received "shut down when ready" signal from orchestrator, but is still doing work.
     */
    PREPARING_TO_SHUT_DOWN,
    /*
     * VM can now be shut down.
     */
    READY_TO_SHUT_DOWN
  }

  /**
   * @return the current status.
   */
  public static Status getCurrentStatus() {
    return currentStatus;
  }

  /**
   * Sets the current status of the MV to a new status.
   *
   * @param newStatus the new status to set.
   */
  public static void setCurrentStatus(Status newStatus) {
    currentStatus = newStatus;
  }
}
