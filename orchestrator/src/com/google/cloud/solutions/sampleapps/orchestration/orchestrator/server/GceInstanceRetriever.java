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

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class to retrieve a list of IPs of running instances on the GCE project.
 */
public class GceInstanceRetriever {

  private static final Logger logger = Logger.getLogger(GceInstanceRetriever.class.getName());

  /**
   * Query for a list of running VMs. Returns a map from IPs to instance names.
   *
   * @return a map containing IPs and instance names.
   */
  public static Map<String, String> getGceIpsAndInstanceNames() {
    Map<String, String> gceIpsAndInstanceNames = new HashMap<String, String>();
    String prefix = ConfigProperties.getInstance().getGceConfigProperties().get("instancePrefix");
    logger.info("Looking for instances with prefix:|" + prefix + "|");

    try {
      String accessToken = GceApiUtils.getAccessTokenForComputeScope();
      String url = ConfigProperties.urlPrefixWithProjectAndZone + "/instances?key="
          + ConfigProperties.getInstance().getGceConfigProperties().get("projectApiKey");
      HTTPResponse httpResponse = GceApiUtils.makeHttpRequest(accessToken, url, "", HTTPMethod.GET);
      int responseCode = httpResponse.getResponseCode();
      if ((responseCode == 200) || (responseCode == 204)) {
        String contentStr = new String(httpResponse.getContent());
        return extractIpsAndInstanceNames(contentStr, prefix);
      } else {
        logger.warning("Failed. Response code " + responseCode + " Reason: "
            + new String(httpResponse.getContent()));
      }
    } catch (MalformedURLException e) {
      logger.warning("Malformed URL: " + e);
    } catch (IOException e) {
      logger.warning("Can't open stream: " + e);
    }
    return gceIpsAndInstanceNames;
  }

  /**
   * Parses the response, which contains all information about the running VMs. Extracts only the
   * IPs and names, as they are all that the orchstrator needs. Adjust this implementation to suit
   * your needs if necessary.
   *
   * @param instanceResponse the string containing the response from queriying "instances" for the
   *        given project.
   * @param instancePrefix returns only instances with name that starts with the prefix as specified
   *        in the configuration properties.
   * @return the extracted IP, or null if it can't be found.
   */
  static Map<String, String> extractIpsAndInstanceNames(String instanceResponse,
         String instancePrefix) {
    JsonParser parser = new JsonParser();
    JsonObject jsonObject = (JsonObject) parser.parse(instanceResponse);
    Map<String, String> gceIpsAndInstanceNames = new HashMap<String, String>();
    JsonArray items = jsonObject.getAsJsonArray("items");
    if (items != null) {
      for (int i = 0; i < items.size(); i++) {
        String natIp = items.get(i)
            .getAsJsonObject()
            .getAsJsonArray("networkInterfaces")
            .get(0)
            .getAsJsonObject()
            .getAsJsonArray("accessConfigs")
            .get(0)
            .getAsJsonObject()
            .get("natIP")
            .getAsString();
        String name = items.get(i).getAsJsonObject().get("name").getAsString();
        if (name.startsWith(instancePrefix)) {
          gceIpsAndInstanceNames.put(natIp, name);
        }
      }
    }
    return gceIpsAndInstanceNames;
  }
}
