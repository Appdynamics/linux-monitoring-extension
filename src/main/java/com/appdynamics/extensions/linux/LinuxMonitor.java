/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.linux.input.MetricStat;
import com.appdynamics.extensions.util.AssertUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class LinuxMonitor extends ABaseMonitor {


    private static final String METRIC_PREFIX = "Custom Metrics|Linux Monitor";

    @Override
    protected String getDefaultMetricPrefix() {
        return METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return "Linux Monitor";
    }


    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {

        LinuxMonitorTask task = new LinuxMonitorTask(serviceProvider, this.getContextConfiguration());
        serviceProvider.submit("LinuxMonitor", task);
    }


    protected List<Map<String, ?>> getServers() {
        return new ArrayList<Map<String, ?>>();
    }


    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        this.getContextConfiguration().setMetricXml(args.get("metric-file"), MetricStat.MetricStats.class);

    }
}
