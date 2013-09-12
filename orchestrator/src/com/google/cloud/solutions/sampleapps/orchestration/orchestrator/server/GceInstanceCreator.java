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
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class used for creating GCE instances.
 */
public class GceInstanceCreator {

  private static final Logger logger = Logger.getLogger(GceInstanceCreator.class.getName());

  /**
   * Creates one or more new instances.
   *
   * @param numExistingInstances the number of existing instances.
   * @param numInstancesToCreate the number of new instances to create.
   * @param existingInstanceNames the map from IPs to instance names
   * @throws OrchestratorException if create new instances failed.
   */
  public boolean createNewInstances(int numExistingInstances, int numInstancesToCreate,
      long maxTimeout, Collection<String> existingInstanceNames) throws OrchestratorException {
    long timeout = System.currentTimeMillis() + maxTimeout;
    int curSuffix = 0;
    for (int i = 0; i < numInstancesToCreate; i++) {
      try {
        Map<String, String> configProperties =
            ConfigProperties.getInstance().getGceConfigProperties();
        String prefix = configProperties.get("instancePrefix");
        curSuffix = findNextAvailableSuffix(curSuffix++, existingInstanceNames, prefix);
        String instanceName = prefix + Integer.toString(curSuffix);
        String diskName = configProperties.get("diskName") + instanceName;
        String projectApiKey = configProperties.get("projectApiKey");
        String accessToken = GceApiUtils.getAccessTokenForComputeScope();

        // 1. Create new disk.
        createDisk(diskName, accessToken, projectApiKey, configProperties);

        // 2. Poll to wait until the disk is ready or until we time out.
        boolean diskCreated = false;
        String url = GceApiUtils.composeDiskApiUrl(
            ConfigProperties.urlPrefixWithProjectAndZone, diskName, projectApiKey);

        while (!diskCreated && System.currentTimeMillis() < timeout) {
          if (checkDiskOrInstance(accessToken, url)) {
            diskCreated = true;
            logger.info("Disk is ready.");
          } else {
            logger.info("Disk is not ready. Sleeping for two seconds ...");
            // Wait for 2 seconds.
            Thread.sleep(2000);
          }
        }
        if (!diskCreated) {
          logger.warning("Timed out. Giving up.");
          return false;
        }

        boolean instanceCreated = false;
        try {
          // 3. Create instance
          createInstance(instanceName, diskName, projectApiKey, accessToken, configProperties);

          // 4. Poll to wait until the instance is ready or until we time out.
          url = GceApiUtils.composeInstanceApiUrl(
              ConfigProperties.urlPrefixWithProjectAndZone, instanceName, projectApiKey);
          while (!instanceCreated && System.currentTimeMillis() < timeout) {
            if (checkDiskOrInstance(accessToken, url)) {
              instanceCreated = true;
              logger.info("Instance is ready.");
            } else {
              logger.info("Instance is not ready. Sleeping for two seconds ...");
              // Wait for 2 seconds.
              Thread.sleep(2000);
            }
          }
        } catch (OrchestratorException e) {
          // We have to catch this exception so we can delete the disk if it has already been
          // created.
          logger.warning("Orchestrator Exception caught: " + e.getMessage());
        }
        if (!instanceCreated) {

          // Before returning, delete the disk that has already been created.
          String deleteUrl = GceApiUtils.composeDiskApiUrl(
              ConfigProperties.urlPrefixWithProjectAndZone, diskName, projectApiKey);
          GceApiUtils.deleteDisk(diskName, accessToken, deleteUrl);

          logger.warning("Timed out. Giving up.");
          return false;
        }
        numExistingInstances++;
      } catch (MalformedURLException e) {
        throw new OrchestratorException(e);
      } catch (IOException e) {
        throw new OrchestratorException(e);
      } catch (InterruptedException e) {
        throw new OrchestratorException(e);
      }
    }
    // All instances have been created within the timeout time frame.
    return true;
  }

  /**
   * @param seed the current seed, where the method will begin looking for the next available suffix
   * @param existingInstanceNames a full list of existing instance names before this orchestration
   *        decision
   * @param prefix the prefix for all instance names (set in the config.xml file)
   * @return the next available suffix
   * @throws OrchestratorException if no new suffix can be found
   */
  private int findNextAvailableSuffix(
      int seed, Collection<String> existingInstanceNames, String prefix)
          throws OrchestratorException {
    while (seed < Integer.MAX_VALUE) {
      String proposedNewInstanceName = prefix + Integer.toString(seed);
      if (!existingInstanceNames.contains(proposedNewInstanceName)) {
        return seed;
      }
      seed++;
    }
    throw new OrchestratorException(
        "Failed to find any available suffixes. Cannot create more instances.");
  }

  /**
   * Creates a new instance.
   *
   * @param instanceName the name of the instance to create.
   * @param bootDiskName the name of the disk to create the instance with.
   * @param projectApiKey the project API key.
   * @param accessToken the access token.
   * @param configProperties the configuration properties.
   * @throws MalformedURLException
   * @throws IOException
   * @throws OrchestratorException if the REST API call failed to create instance.
   */
  private void createInstance(String instanceName, String bootDiskName, String projectApiKey,
      String accessToken, Map<String, String> configProperties)
      throws MalformedURLException, IOException, OrchestratorException {
    String url = GceApiUtils.composeInstanceApiUrl(
        ConfigProperties.urlPrefixWithProjectAndZone, projectApiKey);
    String payload = createPayload_instance(instanceName, bootDiskName, configProperties);
    logger.info(
        "Calling " + url + " to create instance " + instanceName + "with payload " + payload);
    HTTPResponse httpResponse =
        GceApiUtils.makeHttpRequest(accessToken, url, payload, HTTPMethod.POST);
    int responseCode = httpResponse.getResponseCode();
    if (!(responseCode == 200 || responseCode == 204)) {
      throw new OrchestratorException("Failed to create GCE instance. " + instanceName
          + ". Response code " + responseCode + " Reason: "
          + new String(httpResponse.getContent()));
    }
  }

  /**
   * Creates a new disk.
   *
   * @param diskName the name of the disk to create.
   * @param accessToken the access token.
   * @param configProperties the configuration properties.
   * @throws MalformedURLException
   * @throws IOException
   * @throws OrchestratorException if the REST API call failed to create disk.
   */
  private void createDisk(String diskName, String accessToken, String projectApiKey,
      Map<String, String> configProperties)
      throws MalformedURLException, IOException, OrchestratorException {
    String url =
        GceApiUtils.composeDiskApiUrl(ConfigProperties.urlPrefixWithProjectAndZone, projectApiKey);
    String payload = createPayload_disk(diskName, configProperties);
    HTTPResponse httpResponse =
        GceApiUtils.makeHttpRequest(accessToken, url, payload, HTTPMethod.POST);
    int responseCode = httpResponse.getResponseCode();
    if (!(responseCode == 200 || responseCode == 204)) {
      throw new OrchestratorException("Failed to create Disk " + diskName + ". Response code "
          + responseCode + " Reason: " + new String(httpResponse.getContent()));
    }
  }

  /**
   * Checks whether the disk or instance is available.
   *
   * @param accessToken the access token.
   * @param url the URL to check whether the disk/instance has been created.
   * @return true if the disk/instance is available, false otherwise.
   * @throws MalformedURLException
   * @throws IOException
   */
  private boolean checkDiskOrInstance(String accessToken, String url)
      throws MalformedURLException, IOException {
    HTTPResponse httpResponse = GceApiUtils.makeHttpRequest(accessToken, url, "", HTTPMethod.GET);
    int responseCode = httpResponse.getResponseCode();
    if (!(responseCode == 200 || responseCode == 204)) {
      logger.fine("Disk/instance not ready. Response code " + responseCode + " Reason: "
          + new String(httpResponse.getContent()));
      return false;
    }
    // Check if the disk/instance is in status "READY".
    String contentStr = new String(httpResponse.getContent());
    JsonParser parser = new JsonParser();
    JsonObject o = (JsonObject) parser.parse(contentStr);
    String status = o.get("status").getAsString();
    if (!status.equals("READY") && !status.equals("RUNNING")) {
      return false;
    }
    return true;
  }

  /**
   * Makes the payload for creating a new disk.
   *
   * @param diskName the name of the disk.
   * @return the payload for the POST request to create a new disk.
   */
  private String createPayload_disk(String diskName, Map<String, String> configProperties) {
    JsonObject json = new JsonObject();
    json.addProperty("kind", "compute#disk");
    json.addProperty("name", diskName);
    json.addProperty("zone", ConfigProperties.urlPrefixWithProjectAndZone);
    json.addProperty("description", "Persistent boot disk.");
    json.addProperty("sourceSnapshot", ConfigProperties.urlPrefixWithProject + "/global/snapshots/"
        + configProperties.get("snapshotName"));
    String payload = json.toString();
    return payload;
  }

  /**
   * Makes the payload for creating an instance.
   *
   * @param instanceName the name of the instance.
   * @return the payload for the POST request to create a new instance.
   */
  String createPayload_instance(
      String instanceName, String bootDiskName, Map<String, String> configProperties) {
    JsonObject json = new JsonObject();
    json.addProperty("kind", "compute#instance");
    json.addProperty("name", instanceName);
    json.addProperty("machineType", ConfigProperties.urlPrefixWithProjectAndZone + "/machineTypes/"
        + configProperties.get("machineType"));
    json.addProperty("kernel",
        ConfigProperties.URL_PREFIX + "google/global/kernels/" + configProperties.get("kernel"));

    JsonObject disksElem = new JsonObject();
    disksElem.addProperty("kind", "compute#attachedDisk");
    disksElem.addProperty("boot", true);
    disksElem.addProperty("type", "PERSISTENT");
    disksElem.addProperty("mode", "READ_WRITE");
    disksElem.addProperty("deviceName", bootDiskName);
    disksElem.addProperty("zone", ConfigProperties.urlPrefixWithProjectAndZone);
    disksElem.addProperty(
        "source", ConfigProperties.urlPrefixWithProjectAndZone + "/disks/" + bootDiskName);

    JsonArray jsonAr = new JsonArray();
    jsonAr.add(disksElem);
    json.add("disks", jsonAr);

    JsonObject networkInterfacesObj = new JsonObject();
    networkInterfacesObj.addProperty("kind", "compute#instanceNetworkInterface");
    networkInterfacesObj.addProperty(
        "network", ConfigProperties.urlPrefixWithProject + "/global/networks/default");

    JsonObject accessConfigsObj = new JsonObject();
    accessConfigsObj.addProperty("name", "External NAT");
    accessConfigsObj.addProperty("type", "ONE_TO_ONE_NAT");
    JsonArray accessConfigsAr = new JsonArray();
    accessConfigsAr.add(accessConfigsObj);
    networkInterfacesObj.add("accessConfigs", accessConfigsAr);

    JsonArray networkInterfacesAr = new JsonArray();
    networkInterfacesAr.add(networkInterfacesObj);
    json.add("networkInterfaces", networkInterfacesAr);

    JsonObject serviceAccountsObj = new JsonObject();
    serviceAccountsObj.addProperty("kind", "compute#serviceAccount");
    serviceAccountsObj.addProperty("email", "default");
    JsonArray scopesAr = new JsonArray();
    scopesAr.add(new JsonPrimitive("https://www.googleapis.com/auth/userinfo.email"));
    scopesAr.add(new JsonPrimitive("https://www.googleapis.com/auth/compute"));
    scopesAr.add(new JsonPrimitive("https://www.googleapis.com/auth/devstorage.full_control"));
    serviceAccountsObj.add("scopes", scopesAr);
    JsonArray serviceAccountsAr = new JsonArray();
    serviceAccountsAr.add(serviceAccountsObj);
    json.add("serviceAccounts", serviceAccountsAr);

    JsonObject metadataObj = new JsonObject();

    JsonArray mdItemsAr = new JsonArray();
    JsonObject mdItemsObj = new JsonObject();
    mdItemsObj.addProperty("key", "startup-script-url");
    mdItemsObj.addProperty("value", configProperties.get("startupScript"));
    mdItemsAr.add(mdItemsObj);
    metadataObj.add("items", mdItemsAr);
    json.add("metadata", metadataObj);
    String payload = json.toString();
    return payload;
  }
}
