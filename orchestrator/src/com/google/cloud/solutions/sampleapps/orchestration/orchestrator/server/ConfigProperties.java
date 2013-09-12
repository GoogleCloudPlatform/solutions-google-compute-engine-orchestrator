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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Utility class to read properties from config file and make them accessible to the orchestrator
 * code.
 */
public class ConfigProperties {

  private static ConfigProperties instance;

  public static ConfigProperties getInstance() {
    if (instance == null) {
      instance = new ConfigProperties();
    }
    return instance;
  }

  /**
   * Non-public constructor. Will normally only be used to create the instance (or by tests).
   */
  private ConfigProperties() {
    setupProperties();
  }

  private static final Logger logger = Logger.getLogger(ConfigProperties.class.getName());

  public static final String URL_PREFIX = "https://www.googleapis.com/compute/v1beta15/projects/";
  public static final String COMPUTE_SCOPE = "https://www.googleapis.com/auth/compute";

  public static String urlPrefixWithProjectAndZone;
  public static String urlPrefixWithProject;

  private static Map<String, String> gceConfigProperties;
  private static Map<String, Integer> orchestratorConfigProperties;
  private static final int DEFAULT_MAX_SCALE_OUT_TIMEOUT_MSEC = 5 * 60 * 1000;

  /**
   * Gets the configuration properties pertaining to the Compute Engine instances (snapshot, machine
   * type, etc.).
   *
   * @return a map of configuration properties pertaining to Compute Engine and their values.
   */
  public Map<String, String> getGceConfigProperties() {
    return gceConfigProperties;
  }

  /**
   * Gets the configuration properties pertaining to the orchestrator (e.g., max load before
   * spinning up a new instance).
   *
   * @return a map of configuration properties pertaining to the orchestrator and their values.
   */
  public Map<String, Integer> getOrchestratorConfigProperties() {
    return orchestratorConfigProperties;
  }

  /**
   * Reads and parses a configuration file (WEB-INF/config.xml) and builds up maps for both GCE
   * properties and orchestrator properties.
   */
  protected void setupProperties() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setIgnoringComments(true);
    gceConfigProperties = new HashMap<String, String>();
    orchestratorConfigProperties = new HashMap<String, Integer>();
    DocumentBuilder parser;
    try {
      parser = factory.newDocumentBuilder();
      FileInputStream fis = new FileInputStream("WEB-INF/config.xml");
      Document document;
      document = parser.parse(fis);
      setUpGceConfigProperties(document);
      setUpOrchestratorConfigProperties(document);
    } catch (SAXException e) {
      logger.severe("Could not parse config file:" + e.getMessage());
    } catch (IOException e) {
      logger.severe("Could not parse config file:" + e.getMessage());
    } catch (ParserConfigurationException e) {
      logger.severe("Could not create parser for config file:" + e.getMessage());
    }
    urlPrefixWithProjectAndZone = URL_PREFIX + ConfigProperties.gceConfigProperties.get("projectId")
        + "/zones/" + ConfigProperties.gceConfigProperties.get("zone");
    logger.info("urlPrefixWithProjectAndZone (will be used for most API calls):"
        + urlPrefixWithProjectAndZone);

    urlPrefixWithProject = URL_PREFIX + ConfigProperties.gceConfigProperties.get("projectId");
  }

  /**
   * Sets up the configuration properties relating to Compute Engine (e.g., location of startup
   * script).
   *
   * @param configFile the configuration file.
   */
  private void setUpGceConfigProperties(Document configFile) {
    NodeList sections = configFile.getElementsByTagName("gce-config");
    int numSections = sections.getLength();
    for (int i = 0; i < numSections; i++) {
      Element section = (Element) sections.item(i);
      Node configProperty = section.getFirstChild();
      while ((configProperty != null)
          && ((configProperty = configProperty.getNextSibling()) != null)) {
        if (configProperty != null && configProperty.getFirstChild() != null
            && (configProperty.getNodeType() == Node.ELEMENT_NODE)) {
          gceConfigProperties.put(
              configProperty.getNodeName(), configProperty.getFirstChild().getNodeValue());
        }
      }
    }
  }

  /**
   * Sets up the configuration properties relating to the orchestrator (e.g., number of idle
   * instances).
   *
   * @param configFile the configuration file.
   */
  private void setUpOrchestratorConfigProperties(Document configFile) {
    NodeList sections;
    int numSections;
    sections = configFile.getElementsByTagName("orchestrator-config");
    numSections = sections.getLength();
    for (int i = 0; i < numSections; i++) {
      Element section = (Element) sections.item(i);
      Node configProperty = section.getFirstChild();
      while ((configProperty != null)
          && ((configProperty = configProperty.getNextSibling()) != null)) {
        if (configProperty != null && configProperty.getFirstChild() != null
            && (configProperty.getNodeType() == Node.ELEMENT_NODE)) {
          orchestratorConfigProperties.put(configProperty.getNodeName(),
              Integer.parseInt(configProperty.getFirstChild().getNodeValue()));
        }
      }
    }
    if (!orchestratorConfigProperties.containsKey("num-instances-to-create")) {
      orchestratorConfigProperties.put("num-instances-to-create", 1);
    }
    if (!orchestratorConfigProperties.containsKey("num-instances-to-shut-down")) {
      orchestratorConfigProperties.put("num-instances-to-shut-down", 1);
    }
    if (!orchestratorConfigProperties.containsKey("maxScaleOutTimeout")) {
      orchestratorConfigProperties.put("maxScaleOutTimeout", DEFAULT_MAX_SCALE_OUT_TIMEOUT_MSEC);
    }
  }
}
