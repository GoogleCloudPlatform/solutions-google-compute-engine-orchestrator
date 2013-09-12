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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrator tool for GCE instances. Receives information about average load from the GCE
 * instances. Decides whether to spin up new instances or give one or more instances a notice to get
 * ready to be deleted. The instance will usually be ready to be deleted when the current work is
 * completed.
 */
public class Orchestrator {

  /**
   * Public constructor that will create a GceInstanceCreator and GceInstanceDestroyer and get the
   * configuration properties.
   */
  public Orchestrator() {
    this.orchestratorConfigProperties =
        ConfigProperties.getInstance().getOrchestratorConfigProperties();
    this.gceInstanceCreator = new GceInstanceCreator();
    this.gceInstanceDestroyer = new GceInstanceDestroyer();
  }

  /**
   * Non-public constructor. Normally, this should only be called from tests.
   *
   * @param orchestratorConfigProperties the configuration properties.
   * @param gceInstanceCreator an instance of GceInstanceCreator.
   * @param gceInstanceDestroyer an instance of GceInstanceDestroyer.
   */
  protected Orchestrator(Map<String, Integer> orchestratorConfigProperties,
      GceInstanceCreator gceInstanceCreator, GceInstanceDestroyer gceInstanceDestroyer,
      Map<String, String> gceIpsAndInstanceNames) {
    this.orchestratorConfigProperties = orchestratorConfigProperties;
    this.gceInstanceCreator = gceInstanceCreator;
    this.gceInstanceDestroyer = gceInstanceDestroyer;
    this.gceIpsAndInstanceNames = gceIpsAndInstanceNames;
  }

  private static final Logger logger = Logger.getLogger(Orchestrator.class.getName());
  private GceInstanceCreator gceInstanceCreator;
  private GceInstanceDestroyer gceInstanceDestroyer;
  private Map<String, Integer> orchestratorConfigProperties;
  private Map<String, String> gceIpsAndInstanceNames = new HashMap<String, String>();

  private enum State {
    /**
     * The orchestrator is waiting for its previous operation to finish.
     */
    WAITING,
    /**
     * The orchestrator is ready.
     */
    READY
  }

  /* --- Orchestration settings --- */
  /*
   * Orchestration settings are computed from the running VMs, and all orchestration decisions are
   * based on them. When expanding the current set of properties, make sure that the defaults are
   * set such that if the configuration file does not specify these properties, they will not affect
   * orchestration.
   */
  protected int cumCpuLoad = -1;
  protected int cumMemoryLoad = -1;
  protected int cumNumLeasedTasks = -1;

  /*
   * Captures the current state of the orchestrator.
   */
  private int numDesiredInstances;
  private State state = State.READY;

  /* --- Orchestration settings --- */

  /**
   * Whether or not the orchestrator is ready, indicating that this is the first orchestration call
   * or that the last orchestration decision has finished.
   *
   * @return true of the orchestrator is ready, false otherwise.
   */
  public boolean isReady() {
    if (state == State.READY) {
      return true;
    } else {
      if (getGceIpsAndInstanceNames().size() == numDesiredInstances) {
        state = State.READY;
        return true;
      }
      return false;
    }
  }

  /**
   * Gets a map from IPs to instance names for all currently running VMs.
   *
   * @return a map from IPs to instance names.
   */
  public Map<String, String> getGceIpsAndInstanceNames() {
    // Simply retrieve all gceIPs from the project.
    gceIpsAndInstanceNames = GceInstanceRetriever.getGceIpsAndInstanceNames();
    return gceIpsAndInstanceNames;
  }

  /**
   * Makes an orchestration decision based on the current load of the running VMs. The decision can
   * be one of:
   * <ul>
   * <li>do nothing
   * <li>create a new instance
   * <li>send a "prepare to shut down" signal to an instance
   * <li>delete an instance.
   * </ul>
   */
  public OrchestratorDecision orchestrate() {
    logger.info("Cumulative load is:" + cumCpuLoad + ". Current number of instances: "
        + gceIpsAndInstanceNames.size());
    if (gceIpsAndInstanceNames.size() == 0) {
      logger.log(Level.WARNING, "No instances running.");
      return OrchestratorDecision.ERROR;
    }
    try {
      if ((shouldScaleOut() && shouldScaleBack()) || (!shouldScaleOut() && !shouldScaleBack())) {
        /*
         * Do nothing. The configuration settings can lead to conflicting decisions. For instance,
         * based on memory usage, a decision might be made to scale out, while based on number of
         * leased tasks, a decision might be made to scale back. In such a case, the orchestrator
         * currently does nothing. Users should change this implementation to suit their
         * application's needs.
         */
        return OrchestratorDecision.DO_NOTHING;
      } else if (shouldScaleOut()) {
        int numNewInstances = orchestratorConfigProperties.get("num-instances-to-create");
        state = State.WAITING;
        numDesiredInstances = gceIpsAndInstanceNames.size() + numNewInstances;
        gceInstanceCreator.createNewInstances(gceIpsAndInstanceNames.size(), numNewInstances,
            orchestratorConfigProperties.get("maxScaleOutTimeout"),
            gceIpsAndInstanceNames.values());
        state = State.READY;
        return OrchestratorDecision.SCALE_OUT;
      } else { // only shouldScaleBack is true
        /*
         * One VM can be shut down, so notify one VM to get ready to be shut down. Customize this to
         * your needs (i.e., the instance with the fewest currently leased tasks).
         */
        int numObsoleteInstances = orchestratorConfigProperties.get("num-instances-to-shut-down");
        state = State.WAITING;
        numDesiredInstances = gceIpsAndInstanceNames.size() - numObsoleteInstances;
        gceInstanceDestroyer.sendShutdownSignal(numObsoleteInstances);
        return OrchestratorDecision.SCALE_BACK;
      }
    } catch (OrchestratorException e) {
      logger.log(Level.WARNING, "Orchestrator Exception caught: " + e.getMessage());
      // An error will be logged, but the orchestrator should not be stuck.
      state = State.READY;
      return OrchestratorDecision.ERROR;
    }
  }

  /**
   * The main method to determine whether the number of instances should scale out or not. The
   * decision will be based on all scale-out parameters defined in the config file. Currently, these
   * are maximum-ave-cpu-load, maximum-ave-memory-usage, and maximum-number-leased-tasks. Some of
   * these could be undefined.
   *
   * @return true if one or more instances should be shut down, false otherwise.
   */
  protected boolean shouldScaleOut() {
    if (orchestratorConfigProperties.containsKey("maximum-number-instances") &&
        gceIpsAndInstanceNames.size()
        >= orchestratorConfigProperties.get("maximum-number-instances")) {
      return false;
    }
    double averageCpuLoad = (double) cumCpuLoad / (double) gceIpsAndInstanceNames.size();
    double averageMemoryLoad = (double) cumMemoryLoad / (double) gceIpsAndInstanceNames.size();
    double averageNumLeasedTasks =
        (double) cumNumLeasedTasks / (double) gceIpsAndInstanceNames.size();
    return ((orchestratorConfigProperties.containsKey("maximum-ave-cpu-load")
        && averageCpuLoad != -1
        && averageCpuLoad > orchestratorConfigProperties.get("maximum-ave-cpu-load")) || (
        orchestratorConfigProperties.containsKey("maximum-ave-memory-usage")
        && averageMemoryLoad != -1
        && averageMemoryLoad > orchestratorConfigProperties.get("maximum-ave-memory-usage")) || (
        orchestratorConfigProperties.containsKey("maximum-number-leased-tasks")
        && averageNumLeasedTasks != -1 && averageNumLeasedTasks
            > orchestratorConfigProperties.get("maximum-number-leased-tasks")));
  }

  /**
   * The main method to determine whether the number of instances should scale back or not. The
   * decision will be based on all scale-out parameters defined in the config file. Currently, these
   * are minimum-ave-cpu-load, minimum-ave-memory-usage, and minimum-number-leased-tasks. Some of
   * these could be undefined. Furthermore, scaling back will not happen if it would delete the only
   * instance. If num-idle-instances is set, scaling back will not happen if the current number of
   * instances is smaller than num-idle-instances.
   *
   * @return true if one or more instances should be created, false otherwise.
   */
  protected boolean shouldScaleBack() {
    double averageCpuLoad = (double) cumCpuLoad / (double) gceIpsAndInstanceNames.size();
    double averageMemoryLoad = (double) cumMemoryLoad / (double) gceIpsAndInstanceNames.size();
    double averageNumLeasedTasks =
        (double) cumNumLeasedTasks / (double) gceIpsAndInstanceNames.size();
    int min = 1; // don't shut down the last instance
    if (orchestratorConfigProperties.containsKey("num-idle-instances")
        && (orchestratorConfigProperties.get("num-idle-instances") > min)) {
      min = orchestratorConfigProperties.get("num-idle-instances");
    }
    if (gceIpsAndInstanceNames.size() <= min) {
      return false;
    }
    return (orchestratorConfigProperties.containsKey("minimum-ave-cpu-load") && averageCpuLoad != -1
        && averageCpuLoad < orchestratorConfigProperties.get("minimum-ave-cpu-load")) || (
        orchestratorConfigProperties.containsKey("minimum-ave-memory-usage")
        && averageMemoryLoad != -1
        && averageMemoryLoad < orchestratorConfigProperties.get("minimum-ave-memory-usage")) || (
        orchestratorConfigProperties.containsKey("minimum-number-leased-tasks")
        && averageNumLeasedTasks != -1
        && averageNumLeasedTasks < orchestratorConfigProperties.get("minimum-number-leased-tasks"));
  }

  /**
   * Resets the CPU and memory loads as well as the count for the number of leased tasks.
   */
  protected void resetLoads() {
    cumCpuLoad = -1;
    cumMemoryLoad = -1;
    cumNumLeasedTasks = -1;
  }

  /**
   * Increases the cumulative CPU load.
   *
   * @param load the amount to increase the cumulative CPU load by.
   */
  public void addToCumCpuLoad(int load) {
    if (cumCpuLoad == -1) {
      cumCpuLoad = 0;
    }
    cumCpuLoad += load;
  }

  /**
   * Increases the cumulative memory load.
   *
   * @param load the amount to increase the cumulative memory load by.
   */
  public void addToCumMemoryLoad(int load) {
    if (cumMemoryLoad == -1) {
      cumMemoryLoad = 0;
    }
    cumMemoryLoad += load;
  }

  /**
   * Increases the cumulative number of leased tasks.
   *
   * @param numLeasedTasks the number to increase the cumulative number of leased tasks by.
   */
  public void addToCumNumLeasedTasks(int numLeasedTasks) {
    if (cumNumLeasedTasks == -1) {
      cumNumLeasedTasks = 0;
    }
    cumNumLeasedTasks += numLeasedTasks;
  }

  /**
   * Deletes an instance, identified by its name.
   *
   * @param name the name of the instance to delete.
   */
  public void deleteInstance(String name) {
    try {
      gceInstanceDestroyer.deleteIdleInstance(name);
    } catch (OrchestratorException e) {
      logger.log(Level.WARNING, "Failed to delete: " + name + " " + e.getMessage());
    }
  }
}
