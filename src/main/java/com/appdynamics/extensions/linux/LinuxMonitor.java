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

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.DeltaMetricsCalculator;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class LinuxMonitor extends AManagedMonitor {

    private static Logger logger = Logger.getLogger(LinuxMonitor.class);

    public static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|LinuxMonitor|";

    private String metricPrefix = DEFAULT_METRIC_PREFIX;

    private MonitorConfiguration configuration;
    private String configFileName;

    private Cache<String, Long> prevMetricsMap;

    private boolean initialized;

    private final DeltaMetricsCalculator deltaCalculator = new DeltaMetricsCalculator(10);


    public LinuxMonitor() {
        logger.info("Using Monitor Version [" + getImplementationVersion() + "]");
        prevMetricsMap = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
    }

    private void configure(Map<String, String> argsMap) {
        logger.info("Initializing the Linux Configuration");
        MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
        MonitorConfiguration conf = new MonitorConfiguration(metricPrefix, new TaskRunnable(), metricWriteHelper);
        configFileName = argsMap.get("config-file");
        if(Strings.isNullOrEmpty(configFileName)){
            configFileName = "monitors/LinuxMonitor/config.yml";
        }
        conf.setConfigYml(configFileName);
        conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE,
                MonitorConfiguration.ConfItem.METRIC_PREFIX, MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
        this.configuration = conf;
        String prefix = (String)this.configuration.getConfigYml().get("metricPrefix");
        if(!Strings.isNullOrEmpty(prefix)){
            metricPrefix = prefix;
        }
        initialized = true;
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            Map<String, ?> config = configuration.getConfigYml();
            if(config!=null){
                    configuration.getExecutorService().execute(new LinuxMonitoringTask(configuration, metricPrefix,configFileName,prevMetricsMap, deltaCalculator));
            }
            else{
                logger.error("Configuration not found");
            }
        }
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext)
            throws TaskExecutionException {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        try{
            if (taskArgs != null) {
                if (!initialized) {
                    configure(taskArgs);
                }
                logger.info("Starting the Linux Monitoring task");
                if (logger.isDebugEnabled()) {
                    logger.debug("The arguments after appending the default values are " + taskArgs);
                }
                configuration.executeTask();
                return new TaskOutput("Linux Monitor Metric Upload Complete");
            }
        }catch(Exception e) {
                logger.error("Failed to execute the Linux monitoring task", e);
        }
        throw new TaskExecutionException(logVersion() + "Linux monitoring task completed with failures.");
    }

    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        return msg;
    }

    public static String getImplementationVersion() {
        return LinuxMonitor.class.getPackage().getImplementationTitle();
    }


    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        LinuxMonitor monitor = new LinuxMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "/Users/akshay.srivastava/AppDynamics/extensions/linux-monitoring-extension/src/main/resources/conf/config.yml");

    }

}
