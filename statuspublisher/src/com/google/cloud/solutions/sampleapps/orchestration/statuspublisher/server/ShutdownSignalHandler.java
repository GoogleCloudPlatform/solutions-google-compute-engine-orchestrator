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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handler class for receiving a "shut down when possible" signal.
 */
public class ShutdownSignalHandler extends HttpServlet {

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    GceCurrentStatus.setCurrentStatus(Status.PREPARING_TO_SHUT_DOWN);
    res.setContentType("text/html");
    PrintWriter out = res.getWriter();
    out.println("PREPARING_TO_SHUT_DOWN");
    out.close();
  }
}
