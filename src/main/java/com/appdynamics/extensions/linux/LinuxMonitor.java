/**
 * Copyright 2013 AppDynamics
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

package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.linux.config.Configuration;
import com.appdynamics.extensions.linux.config.MountedNFS;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class LinuxMonitor extends AManagedMonitor {

    private static Logger logger = Logger.getLogger(LinuxMonitor.class);
    private Cache<String, Long> prevMetricsMap;

    public LinuxMonitor() {
        System.out.println(logVersion());
        prevMetricsMap = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext)
            throws TaskExecutionException {
        if (taskArgs != null) {
            logger.info(logVersion());
            try {
                String configFilename = getConfigFilename(taskArgs.get("config-file"));
                Configuration config = YmlReader.readFromFile(configFilename, Configuration.class);

                Map<String, Object> statsMap = populateMetrics(config);
                printNestedMap(statsMap, config.getMetricPrefix());

                logger.info("Linux Monitoring Task completed successfully");
                return new TaskOutput("Linux Monitoring Task completed successfully");
            } catch (Exception e) {
                logger.error("Linux Metric Upload Failed ", e);
            }
        }
        throw new TaskExecutionException("Linux Monitor completed with failures");
    }

    private Map<String, Object> populateMetrics(Configuration config) {
        Stats stats = new Stats(logger);
        Map<String, Object> statsMap = new HashMap<String, Object>();
        Map<String, Object> map;

        if ((map = stats.getDiskStats()) != null) {
            statsMap.put("disk", map);
        }

        if ((map = stats.getCPUStats()) != null) {
            statsMap.put("CPU", map);
        }

        if ((map = stats.getDiskUsage()) != null) {
            statsMap.put("disk usage", map);
        }
        if ((map = stats.getFileStats()) != null) {
            statsMap.put("file", map);
        }
        if ((map = stats.getLoadStats()) != null) {
            statsMap.put("load average", map);
        }
        if ((map = stats.getMemStats()) != null) {
            statsMap.put("memory", map);
        }
        if ((map = stats.getNetStats()) != null) {
            statsMap.put("network", map);
        }
        if ((map = stats.getPageSwapStats()) != null) {
            statsMap.put("page", map);
        }
        if ((map = stats.getProcStats()) != null) {
            statsMap.put("process", map);
        }
        if ((map = stats.getSockStats()) != null) {
            statsMap.put("socket", map);
        }
        MountedNFS[] mountedNFS = config.getMountedNFS();
        if(mountedNFS != null) { //Null check to MountedNFS
            if ((map = stats.getMountStatus(mountedNFS)) != null) {
                statsMap.put("nfsMountStatus", map);
            }
        }
        return statsMap;
    }

    private void printNestedMap(Map<String, Object> map, String metricPath) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof Map) {
                printNestedMap((Map) val, metricPath + key + "|");
            } else {
                // compute Avg IO utilization using metric in diskstats
                if ("time spent doing I/Os (ms)".equals(key)) {
                    Long[] prevValues = processDelta(metricPath, val);
                    if (prevValues[0] != null) {
                        printMetric(metricPath + "Avg I/O Utilization %", getDeltaValue(val, prevValues));
                    }
                }
                printMetric(metricPath + key, val);
            }
        }
    }

    private Long[] processDelta(String metricName, Object timeSpentDoingIOInms) {
        String timeElapsed = "timeElapsed";
        Long prevMetricValue = prevMetricsMap.getIfPresent(metricName);
        Long prevTimeElapsed = prevMetricsMap.getIfPresent(metricName + timeElapsed);
        Long[] prevValues = {prevMetricValue, prevTimeElapsed};
        if (timeSpentDoingIOInms != null) {
            prevMetricsMap.put(metricName, Long.valueOf((String) timeSpentDoingIOInms));
            prevMetricsMap.put(metricName + timeElapsed, System.currentTimeMillis());
        }
        return prevValues;
    }

    private Double getDeltaValue(Object currentValue, Long[] prevValues) {
        if (currentValue == null) {
            return null;
        }
        long currentTime = System.currentTimeMillis();
        long prevTime = prevValues[1];
        Long deltaIOTimeSpent = Long.valueOf((String) currentValue) - prevValues[0];
        Long duration = currentTime - prevTime;

        Double avgIOUtilizationInPercent = (double) deltaIOTimeSpent / (double) duration * 100.0;
        return avgIOUtilizationInPercent;
    }

    private void printMetric(String metricName, Object metricValue) {
        if (metricValue != null) {
            MetricWriter metricWriter = getMetricWriter(metricName,
                    MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            try {
                String valueString = toWholeNumberString(metricValue);
                if (valueString != null) {
                    metricWriter.printMetric(valueString);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("metric = " + valueString);
                }
            } catch (Exception e) {
                logger.error("Error while reporting metric " + metricName + " " + metricValue, e);
            }
        }
    }

    private String toWholeNumberString(Object attribute) {
        if (attribute instanceof String) {
            String attrString = (String) attribute;
            Double d = Double.valueOf(attrString);
            return d > 0 && d < 1.0d ? "1" : String.valueOf(Math.round(d));
        } else if (attribute instanceof Long) {
            return String.valueOf(attribute);
        } else if (attribute instanceof Double) {
            Double f1 = (Double) attribute;
            return f1.doubleValue() > 0.0D && f1.doubleValue() < 1.0D ? "1" : String.valueOf(Math.round(f1.doubleValue()));
        } else if (attribute instanceof Float) {
            Float f = (Float) attribute;
            return f.floatValue() > 0.0F && f.floatValue() < 1.0F ? "1" : String.valueOf(Math.round(((Float) attribute).floatValue()));
        }
        return null;
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }

    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        return msg;
    }

    public static String getImplementationVersion() {
        return LinuxMonitor.class.getPackage().getImplementationTitle();
    }
}
