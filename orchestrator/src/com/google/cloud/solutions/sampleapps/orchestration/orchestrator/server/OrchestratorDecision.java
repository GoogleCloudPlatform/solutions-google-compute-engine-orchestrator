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

package com.google.cloud.solutions.sampleapps.orchestration.orchestrator.server;

/**
 * Possible orchestration decisions.
 */
public enum OrchestratorDecision {
  /*
   * Error. No orchestration decision could be made, e.g., because no instances are running.
   */
  ERROR,
  /*
   * VM is running and leasing tasks.
   */
  DO_NOTHING,
  /*
   * VMs should be scaled back.
   */
  SCALE_BACK,
  /*
   * VMs should be scaled out.
   */
  SCALE_OUT;
  
  @Override
  public String toString() {
    return this.name().toLowerCase();
   }
}
