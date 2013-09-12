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

import com.google.cloud.solutions.sampleapps.orchestration.common.GceStats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class to get statuses of Compute Engine instances.
 */
@SuppressWarnings("serial")
public class InstanceStatusServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(OrchestratorServlet.class.getName());

  private Orchestrator orchestrator;

  @Override
  public void init(ServletConfig config) throws ServletException {
    orchestrator = new Orchestrator();
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    JsonArray jsonAr = new JsonArray();
    Map<String, String> gceIpsAndInstanceNames = orchestrator.getGceIpsAndInstanceNames();
    resp.setContentType("application/json");
    resp.setHeader("Cache-Control", "no-cache");
    for (String gceIp : gceIpsAndInstanceNames.keySet()) {
      try {
        // Query each instance for its status/load.
        URL url = new URL("http://" + gceIp + ":8080/StatusPublisher/status");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        GceStats gceStats = gson.fromJson(reader, GceStats.class);
        reader.close();
        JsonObject instanceElem = new JsonObject();
        instanceElem.addProperty("instanceName", gceIpsAndInstanceNames.get(gceIp));
        instanceElem.addProperty("status", gson.toJson(gceStats));
        jsonAr.add(instanceElem);
      } catch (MalformedURLException e) {
        logger.warning("Malformed URL: " + e);
      } catch (IOException e) {
        logger.warning("Can't open stream: " + e);
      }
    }
    PrintWriter writer = resp.getWriter();
    writer.println(gson.toJson(jsonAr));
  }
}
