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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;


public class LinuxMonitor extends AManagedMonitor {

    private static String metricPath = "Custom Metrics|Linux";
    private static Logger logger = Logger.getLogger("com.singularity.extensions.LinuxMonitor");
    
    public LinuxMonitor() {
    	String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
	}

    public TaskOutput execute(Map<String, String> args, TaskExecutionContext taskExecutionContext)
            throws TaskExecutionException {
        try {
            if (!args.get("metric-path").equals("")){
                metricPath = args.get("metric-path");
            }

            Stats stats = new Stats(logger);
            Map<String, Object> statsMap = new HashMap<String, Object>();
            Map<String, Object> map;

            if ((map = stats.getCPUStats()) != null){
                statsMap.put("CPU",map);
            }
            if ((map = stats.getDiskStats()) != null){
                statsMap.put("disk",map);
            }
            if ((map = stats.getDiskUsage()) != null){
                statsMap.put("disk usage",map);
            }
            if ((map = stats.getFileStats()) != null){
                statsMap.put("file",map);
            }
            if ((map = stats.getLoadStats()) != null){
                statsMap.put("load average",map);
            }
            if ((map = stats.getMemStats()) != null){
                statsMap.put("memory",map);
            }
            if ((map = stats.getNetStats()) != null){
                statsMap.put("network",map);
            }
            if ((map = stats.getPageSwapStats()) != null){
                statsMap.put("page",map);
            }
            if ((map = stats.getProcStats()) != null){
                statsMap.put("process",map);
            }
            if ((map = stats.getSockStats()) != null){
                statsMap.put("socket",map);
            }

            printNestedMap(statsMap, metricPath);

            return new TaskOutput("Linux Metric Upload Complete");
        } catch (Exception e) {
            logger.error(e);
            return new TaskOutput("Linux Metric Upload Failed");
        }
    }

    private void printNestedMap(Map<String, Object> map, String hierarchy){
        for (Map.Entry<String, Object> entry : map.entrySet()){
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof String) {
                printMetric(hierarchy + "|" + key, val,
                        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
            } else if (val instanceof Map){
                printNestedMap((Map) val, hierarchy + "|" + key);
            }
        }
    }

    private void printMetric(String metricName, Object metricValue, String aggregation, String timeRollup, String cluster)
    {
        MetricWriter metricWriter = getMetricWriter(metricName,
                aggregation,
                timeRollup,
                cluster
        );
        if(metricValue != null) {
        	String valueString = toWholeNumberString(metricValue);
        	metricWriter.printMetric(valueString);
        	if (logger.isDebugEnabled()) {
                logger.debug("metric = " + metricValue);
            }
        }
    }
    
    private String toWholeNumberString(Object attribute) {
        if(attribute instanceof Double){
            Double d = (Double) attribute;
            if(d > 0 && d < 1.0d){
                return "1";
            }
            return String.valueOf(Math.round(d));
        }
        else if(attribute instanceof Float){
            Float f = (Float) attribute;
            if(f > 0 && f < 1.0f){
                return "1";
            }
            return String.valueOf(Math.round((Float) attribute));
        }
        return attribute.toString();
    }
    
    public static String getImplementationVersion() {
        return LinuxMonitor.class.getPackage().getImplementationTitle();
    }
}
