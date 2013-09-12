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
 * Class to parse a configuration property file.
 */
public class ConfigProperties {
  private static final Logger logger = Logger.getLogger(ConfigProperties.class.getName());

  private Map<String, String> configProperties;

  /**
   * Constructor
   *
   * @param configFile the absolute path to the configuration file
   */
  public ConfigProperties(final String configFile) {
    setUpProperties(configFile);
  }

  /**
   * Gets the configuration properties pertaining to the status publisher such as poll/push mode to
   * get the application status etc)
   *
   * @return a map of configuration properties pertaining to the status publisher
   */
  public Map<String, String> getConfigProperties() {
    return configProperties;
  }

  private void setUpProperties(final String configFile) {
    logger.info("Setting up properties for the status publisher from " + configFile);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setIgnoringComments(true);

    configProperties = new HashMap<String, String>();
    DocumentBuilder parser;
    try {
      parser = factory.newDocumentBuilder();
      FileInputStream fis = new FileInputStream(configFile);
      Document document;
      document = parser.parse(fis);
      setUpStatusPublisherConfigProperties(document);
    } catch (SAXException e) {
      logger.severe("Could not parse config file:" + e.getMessage());
    } catch (IOException e) {
      logger.severe("Could not parse config file:" + e.getMessage());
    } catch (ParserConfigurationException e) {
      logger.severe("Could not create parser for config file:" + e.getMessage());
    }
  }

  /**
   * @param configFile the configuration file
   */
  private void setUpStatusPublisherConfigProperties(Document configFile) {
    NodeList sections = configFile.getElementsByTagName("status-publisher-config");
    int numSections = sections.getLength();
    for (int i = 0; i < numSections; i++) {
      Element section = (Element) sections.item(i);
      Node configProperty = section.getFirstChild();
      while ((configProperty != null)
          && ((configProperty = configProperty.getNextSibling()) != null)) {
        if (configProperty != null && configProperty.getFirstChild() != null
            && (configProperty.getNodeType() == Node.ELEMENT_NODE)) {
          configProperties.put(
              configProperty.getNodeName(), configProperty.getFirstChild().getNodeValue());
        }
      }
    }
  }
}
