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

import com.google.cloud.solutions.sampleapps.orchestration.common.GceCurrentStatus.Status;
import com.google.cloud.solutions.sampleapps.orchestration.common.GceStats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for handling orchestration requests, which are scheduled regularly via cron jobs or
 * another method.
 */
@SuppressWarnings("serial")
public class OrchestratorServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(OrchestratorServlet.class.getName());
  private static final Orchestrator orchestrator = new Orchestrator();

  /**
   * Responds to an orchestration request, such as sent by a cron job or other method. Queries VM
   * instances for load/status and gets an orchestration decision. The orchestration decision may be
   * one of:
   * <ul>
   * <li>do nothing
   * <li>create a new instance
   * <li>send a "prepare to shut down" signal to an instance
   * <li>delete an instance.
   * </ul>
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/plain");
    Map<String, String> gceIpsAndInstanceNames = orchestrator.getGceIpsAndInstanceNames();
    PrintWriter writer = resp.getWriter();
    if (gceIpsAndInstanceNames.size() == 0) {
      logger.info("No running instances with specified prefix found.");
    } else {
      for (String gceIp : gceIpsAndInstanceNames.keySet()) {
        writer.println("--- Instance " + gceIpsAndInstanceNames.get(gceIp) + " ---");
        logger.info("--- Instance " + gceIpsAndInstanceNames.get(gceIp) + " ---");
        try {
          // Query each instance for its status/load. This assumes that each instance is publishing
          // on this URL.
          URL url = new URL("http://" + gceIp + ":8080/StatusPublisher/status");
          BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
          Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
          GceStats gceStats = gson.fromJson(reader, GceStats.class);
          reader.close();
          if (gceStats.getCurrentStatus() == Status.READY_TO_SHUT_DOWN) {
            writer.println(
                gceIpsAndInstanceNames.get(gceIp) + " is ready to shut down. Now deleting.");
            logger.info(
                gceIpsAndInstanceNames.get(gceIp) + " is ready to be deleted. Now deleting.");
            orchestrator.deleteInstance(gceIpsAndInstanceNames.get(gceIp));
          } else if (gceStats.getCurrentStatus() == Status.PREPARING_TO_SHUT_DOWN) {
            // Wait until the instance is ready to be shut down.
            writer.println(gceIpsAndInstanceNames.get(gceIp) + " is preparing to be shut down");
            logger.info(gceIpsAndInstanceNames.get(gceIp) + " is preparing to shut down.");
          } else {
            double load = gceStats.getSystemStats().getAveSystemLoad();
            String msg = "most recent value: " + load;
            writer.println(gceIpsAndInstanceNames.get(gceIp) + " " + msg);
            logger.info(gceIpsAndInstanceNames.get(gceIp) + " " + msg);
            /*
             * This is a simplified implementation. In production settings, consider using
             * additional data for orchestration decisions.
             */
            int systemLoad = (int) Math.floor(load * 100);
            orchestrator.addToCumCpuLoad(systemLoad);
          }
        } catch (MalformedURLException e) {
          logger.warning("Malformed URL: " + e);
        } catch (IOException e) {
          logger.warning("Can't open stream: " + e);
        }
      }
      // If the orchestrator is the middle of scaling out or down, do not do anything until
      // the scaling is completed.
      if (orchestrator.isReady()) {
        OrchestratorDecision decision = orchestrator.orchestrate();
        writer.println("Orchestration decision:" + decision.toString());
        logger.info("Orchestration decision:" + decision.toString());
      } else {
        logger.info("orchestrator is not ready");
      }
      orchestrator.resetLoads();
    }
    writer.close();
  }
}
