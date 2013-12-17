package com.appdynamics.monitors.linux;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;


public class LinuxMonitor extends AManagedMonitor {

    private static String metricPath = "Custom Metrics|Linux";
    private static Logger logger = Logger.getLogger(LinuxMonitor.class);

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
                printMetric(hierarchy + "|" + key, (String) val,
                        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
            } else if (val instanceof Map){
                printNestedMap((Map) val, hierarchy + "|" + key);
            }
        }
    }

    private void printMetric(String metricName, String metricValue, String aggregation, String timeRollup, String cluster)
    {
        MetricWriter metricWriter = getMetricWriter(metricName,
                aggregation,
                timeRollup,
                cluster
        );
        try {
            Long.parseLong(metricValue);
            metricWriter.printMetric(metricValue);
        } catch (NumberFormatException e) {
            Double val = Double.parseDouble(metricValue);
            metricWriter.printMetric(String.valueOf(val.longValue()));
        }
    }
}
