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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Class to send "shut down when ready" requests to GCE instances.
 */
public class GceInstanceDestroyer {

  private static final Logger logger = Logger.getLogger(GceInstanceDestroyer.class.getName());

  /**
   * Sends a "prepare to shut down" signal to one or more instances.
   *
   * @param numInstancesToShutDown the number of instances to shut down.
   */
  public void sendShutdownSignal(int numInstancesToShutDown) {
    /*
     * This is a simplified implementation. In a production system, consider adding some logic
     * determining which instance(s) to shut down. For instance, this might be the instance with the
     * lowest load.
     */
    Iterator<Entry<String, String>> iterator =
        GceInstanceRetriever.getGceIpsAndInstanceNames().entrySet().iterator();
    int i = 0;

    while (iterator.hasNext() && i < numInstancesToShutDown) {
      String ip = iterator.next().getKey();
      i++;
      try {
        logger.info("Sending 'prepare to shut down' signal to ip:" + ip);
        URL url = new URL("http://" + ip + ":8080/StatusPublisher/shutdown");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = reader.readLine()) != null) {
          logger.finest(line);
        }
        reader.close();
      } catch (MalformedURLException e) {
        logger.warning("Malformed URL: " + e);
      } catch (IOException e) {
        logger.warning("Can't open stream: " + e);
      }
    }
  }

  /**
   * Deletes an idle instance.
   *
   * @param name the name of the instance to delete.
   * @throws OrchestratorException if delete instance failed.
   */
  public void deleteIdleInstance(String name) throws OrchestratorException {
    String accessToken = GceApiUtils.getAccessTokenForComputeScope();
    ConfigProperties configProperties = ConfigProperties.getInstance();
    String projectApiKey =
        ConfigProperties.getInstance().getGceConfigProperties().get("projectApiKey");

    // 1. Delete instance.
    try {
      String instanceUrl = GceApiUtils.composeInstanceApiUrl(
          ConfigProperties.urlPrefixWithProjectAndZone, name, projectApiKey);
      deleteInstance(name, accessToken, instanceUrl);

      // 2. Poll to wait until instance is deleted before deleting the boot disk.
      // In a production system, need to safeguard against infinite loop here.
      while (!isInstanceDeleted(accessToken, instanceUrl)) {
        logger.info("Instance is shutting down. Sleep for two seconds ...");
        // Wait for 2 seconds.
        Thread.sleep(2000);
      }

      // 3. Delete boot disk.
      String diskName = configProperties.getGceConfigProperties().get("diskName") + name;
      String deleteUrl = GceApiUtils.composeDiskApiUrl(
          ConfigProperties.urlPrefixWithProjectAndZone, diskName, projectApiKey);
      GceApiUtils.deleteDisk(diskName, accessToken, deleteUrl);
    } catch (IOException e) {
      throw new OrchestratorException(e);
    } catch (InterruptedException e) {
      throw new OrchestratorException(e);
    }
  }

  /**
   * Deletes an instance.
   *
   * @param name the name of the instance to delete.
   * @throws OrchestratorException if delete failed.
   */
  private void deleteInstance(String name, String accessToken, String url)
      throws OrchestratorException {
    logger.info("Shutting down instance: " + name);
    HTTPResponse httpResponse;
    try {
      httpResponse = GceApiUtils.makeHttpRequest(accessToken, url, "", HTTPMethod.DELETE);
      int responseCode = httpResponse.getResponseCode();
      if (!(responseCode == 200 || responseCode == 204)) {
        throw new OrchestratorException("Delete Instance failed. Response code " + responseCode
            + " Reason: " + new String(httpResponse.getContent()));
      }
    } catch (IOException e) {
      throw new OrchestratorException(e);
    }
  }

  /**
   * Checks whether the instance is deleted.
   *
   * @param accessToken the access token.
   * @param url the URL to check whether the instance is still up.
   * @return true if the instance is not found, false otherwise.
   * @throws IOException thrown by makeHttpRequest if the Compute Engine API could not be contacted.
   * @throws OrchestratorException
   */
  private boolean isInstanceDeleted(String accessToken, String url)
      throws IOException, OrchestratorException {
    HTTPResponse httpResponse = GceApiUtils.makeHttpRequest(accessToken, url, "", HTTPMethod.GET);
    int responseCode = httpResponse.getResponseCode();
    if (responseCode == 404) { // Not Found.
      return true;
    } else if (responseCode == 200 || responseCode == 204) {
      return false;
    } else {
      throw new OrchestratorException("Failed to check if instance is deleted. Response code "
          + responseCode + " Reason: " + new String(httpResponse.getContent()));
    }
  }
}
