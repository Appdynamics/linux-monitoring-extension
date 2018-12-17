/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;


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


    @Override
    protected int getTaskCount() {
        // Always run on only 1 machine.
        return 1;
    }

}
