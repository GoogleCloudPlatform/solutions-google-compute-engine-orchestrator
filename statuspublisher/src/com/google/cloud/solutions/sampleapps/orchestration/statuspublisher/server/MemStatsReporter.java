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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to get memory usage information.
 */
public class MemStatsReporter {
  private static final Logger logger = Logger.getLogger(MemStatsReporter.class.getName());

  private static final Pattern MEM_TOTAL_PATTERN = Pattern.compile("^MemTotal:[ ]*([0-9]+) kB.*");
  private static final Pattern MEM_FREE_PATTERN = Pattern.compile("^MemFree:[ ]*([0-9]+) kB.*");
  private static final String PROC_MEM_FILE = "procMemFile";

  private ConfigProperties configProperties;
  private Pattern memTotalPattern;
  private Pattern memFreePattern;

  public MemStatsReporter(ConfigProperties configProperties) {
    this.configProperties = configProperties;
  }

  /**
   * Gets the amount of memory used.
   *
   * @return memory used in float between 0 and 1 and -1.0 if there is error getting the data.
   */
  public float getUsedMemoryRatio() {
    return parseMemInfo(readMemInfo());
  }

  float parseMemInfo(List<String> memInfo) {
    float totalMem = -1.0f;
    float freeMem = -1.0f;
    Matcher matcher;
    for (String line : memInfo) {
      matcher = MEM_TOTAL_PATTERN.matcher(line);
      if (matcher.find()) {
        totalMem = Float.valueOf(matcher.group(1));
        logger.info("TotaPattern " + matcher.group(1));
      } else {
        matcher = MEM_FREE_PATTERN.matcher(line);
        if (matcher.find()) {
          freeMem = Float.valueOf(matcher.group(1));
          logger.info("FreePattern " + matcher.group(1));
        }
      }
    }
    if (totalMem == -1.0f || freeMem == -1.0f) {
      logger.log(Level.WARNING, "Failed to get total or free memory.");
      return -1.0f;
    } else if (totalMem == 0.0f) {
      logger.log(Level.WARNING, "Total memory is 0.");
      return -1.0f;
    } else {
      return 1.0f - (freeMem / totalMem);
    }
  }

  private List<String> readMemInfo() {
    BufferedReader bufferReader = null;
    List<String> memInfo = new ArrayList<String>();
    try {
      String line;
      String procMemInfoFile = configProperties.getConfigProperties().get(PROC_MEM_FILE);
      bufferReader = new BufferedReader(new FileReader(procMemInfoFile));
      while ((line = bufferReader.readLine()) != null) {
        memInfo.add(line);
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to open meminfo file " + e.getMessage());
    } finally {
      try {
        if (bufferReader != null) {
          bufferReader.close();
        }
      } catch (IOException ex) {
        logger.log(Level.WARNING, "Failed to close meminfo file " + ex.getMessage());
      }
    }
    return memInfo;
  }
}
