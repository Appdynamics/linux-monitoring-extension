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

/*
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
                    configuration.getExecutorService().execute(new LinuxMetricsTask(configuration, metricPrefix,configFileName,prevMetricsMap, deltaCalculator));
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

*/

    public static void main(String[] args) throws TaskExecutionException {

      /*  ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        LinuxMonitor monitor = new LinuxMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "/Users/akshay.srivastava/AppDynamics/extensions/linux-monitoring-extension/src/main/resources/conf/config.yml");*/


        String line ="TCP: inuse 8 orphan 0 tw 0 alloc 15 mem 0";
        String[] stats = line.split("[\t ]+");
        //for(int i=0; i<=1; i++) {
            while(!StringUtils.isNumeric(stats[0])) {
                stats = ArrayUtils.removeElement(stats, stats[0]);
            }
       // }
        for(int i=0;i<stats.length;i++){
            System.out.println(stats[i]);
        }

    }

}
