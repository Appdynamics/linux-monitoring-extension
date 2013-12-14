package com.appdynamics.monitors.linux;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.util.Map;


public class LinuxMonitor extends AManagedMonitor {

    private static Logger logger = Logger.getLogger(LinuxMonitor.class);

    public TaskOutput execute(Map<String, String> stringStringMap, TaskExecutionContext taskExecutionContext)
            throws TaskExecutionException {
        try {
            //TODO call/do main logic
            return new TaskOutput("Linux Metric Upload Complete");
        } catch (Exception e) {
            //TODO logger error
            return new TaskOutput("Linux Metric Upload Failed");
        }
    }

    private void printMetric(String metricName, Object metricValue, String aggregation, String timeRollup, String cluster)
    {
        MetricWriter metricWriter = getMetricWriter(metricName,
                aggregation,
                timeRollup,
                cluster
        );
        if (metricValue instanceof Double){
            metricWriter.printMetric(String.valueOf(Math.round((Double)metricValue)));
        } else if (metricValue instanceof Float) {
            metricWriter.printMetric(String.valueOf(Math.round((Float)metricValue)));
        } else {
            metricWriter.printMetric(String.valueOf(metricValue));
        }
    }
}
