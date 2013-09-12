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

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for using the GCE API.
 */
public class GceApiUtils {

  private static List<String> computeScope = new ArrayList<String>();

  static {
    computeScope.add(ConfigProperties.COMPUTE_SCOPE);
  }

  /**
   * Creates an HTTPRequest with the information passed in.
   *
   * @param accessToken the access token necessary to authorize the request.
   * @param url the url to query.
   * @param payload the payload for the request.
   * @return the created HTTP request.
   * @throws IOException
   */
  public static HTTPResponse makeHttpRequest(
      String accessToken, final String url, String payload, HTTPMethod method) throws IOException {

    // Create HTTPRequest and set headers
    HTTPRequest httpRequest = new HTTPRequest(new URL(url.toString()), method);
    httpRequest.addHeader(new HTTPHeader("Authorization", "OAuth " + accessToken));
    httpRequest.addHeader(new HTTPHeader("Host", "www.googleapis.com"));
    httpRequest.addHeader(new HTTPHeader("Content-Length", Integer.toString(payload.length())));
    httpRequest.addHeader(new HTTPHeader("Content-Type", "application/json"));
    httpRequest.addHeader(new HTTPHeader("User-Agent", "google-api-java-client/1.0"));
    httpRequest.setPayload(payload.getBytes());

    URLFetchService fetcher = URLFetchServiceFactory.getURLFetchService();
    HTTPResponse httpResponse = fetcher.fetch(httpRequest);
    return httpResponse;
  }

  /**
   * Gets the access token for the compute scope (https://www.googleapis.com/auth/compute). This
   * will likely be the scope most commonly used by the orchestrator.
   *
   * @return the access token
   */
  public static String getAccessTokenForComputeScope() {
    return getAccessToken(computeScope);
  }

  /**
   * Get the access token for a list of scopes.
   *
   * @param scopes the scopes to get the access tokens for.
   * @return the access token.
   */
  public static String getAccessToken(List<String> scopes) {
    final AppIdentityService appIdService = AppIdentityServiceFactory.getAppIdentityService();
    AppIdentityService.GetAccessTokenResult result = appIdService.getAccessToken(scopes);
    return result.getAccessToken();
  }

  /**
   * Deletes a disk.
   *
   * @param diskName the name of the disk to delete.
   * @throws IOException thrown by makeHttpRequest if the Compute Engine API could not be contacted.
   * @throws OrchestratorException if the REST API call failed to delete the disk.
   */
  public static void deleteDisk(String diskName, String accessToken, String url)
      throws IOException, OrchestratorException {
    HTTPResponse httpResponse =
        GceApiUtils.makeHttpRequest(accessToken, url, "", HTTPMethod.DELETE);
    int responseCode = httpResponse.getResponseCode();
    if (!(responseCode == 200 || responseCode == 204)) {
      throw new OrchestratorException("Delete Disk failed. Response code " + responseCode
          + " Reason: " + new String(httpResponse.getContent()));
    }
  }

  public static String composeDiskApiUrl(
      String urlPrefixWithProjectAndZone, String diskName, String apiKey) {
    return urlPrefixWithProjectAndZone + "/disks/" + diskName + "?key=" + apiKey;
  }

  public static String composeDiskApiUrl(String urlPrefixWithProjectAndZone, String apiKey) {
    return urlPrefixWithProjectAndZone + "/disks/?key=" + apiKey;
  }

  public static String composeInstanceApiUrl(
      String urlPrefixWithProjectAndZone, String name, String apiKey) {
    return urlPrefixWithProjectAndZone + "/instances/" + name + "?key=" + apiKey;
  }

  public static String composeInstanceApiUrl(String urlPrefixWithProjectAndZone, String apiKey) {
    return urlPrefixWithProjectAndZone + "/instances/?key=" + apiKey;
  }
}
