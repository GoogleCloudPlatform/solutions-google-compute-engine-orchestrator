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

import com.google.cloud.solutions.sampleapps.orchestration.common.GceCurrentStatus;
import com.google.cloud.solutions.sampleapps.orchestration.common.GceCurrentStatus.Status;
import com.google.cloud.solutions.sampleapps.orchestration.common.GceStats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handler for a request for the status of the instance.
 */
public class StatusPublisher extends HttpServlet {
  private static final Logger logger = Logger.getLogger(StatusPublisher.class.getName());

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    doGet(req, res);
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    logger.info("Getting status");
    res.setContentType("application/json");
    res.setHeader("Cache-Control", "no-cache");
    PrintWriter out = res.getWriter();
    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    /*
     * 1. Check the current status. If the current status is PREPARING_FOR_SHUTDOWN, check if there
     * are any more leased tasks. If no, then change the status to READY_FOR_SHUTDOWN.
     */
    if (GceCurrentStatus.getCurrentStatus() == Status.READY_TO_SHUT_DOWN) {
      out.println(gson.toJson(new GceStats(Status.READY_TO_SHUT_DOWN)));
    } else if (GceCurrentStatus.getCurrentStatus() == Status.PREPARING_TO_SHUT_DOWN) {
      if (checkWhetherReadyToShutDown()) {
        GceCurrentStatus.setCurrentStatus(Status.READY_TO_SHUT_DOWN);
        out.println(gson.toJson(new GceStats(Status.READY_TO_SHUT_DOWN)));
      } else {
        out.println(gson.toJson(new GceStats(Status.PREPARING_TO_SHUT_DOWN)));
      }
    } else {
      SystemStatsReporter reporter = (SystemStatsReporter) getServletContext()
          .getAttribute(StatusPublisherServletContext.SYSTEM_STATS_REPORTER);
      out.println(gson.toJson(reporter.getStats()));
    }
  }

  private boolean checkWhetherReadyToShutDown() {
    // This is a simplified implementation. In a production setting,
    // one could for instance check the number of currently leased tasks.
    return true;
  }
}
