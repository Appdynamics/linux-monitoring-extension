package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.linux.input.MetricStat;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

/**
 * Created by akshay.srivastava on 9/12/18.
 */
public class LinuxMonitorTask implements AMonitorTaskRunnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(LinuxMonitorTask.class);

    private MonitorContextConfiguration configuration;

    private MetricWriteHelper metricWriter;

    private String metricPrefix;

    private List<Map<String, String>> metricReplacer;

    public LinuxMonitorTask(TasksExecutionServiceProvider serviceProvider, MonitorContextConfiguration configuration) {
        this.configuration = configuration;
        this.metricPrefix = configuration.getMetricPrefix();
        this.metricWriter = serviceProvider.getMetricWriteHelper();
        this.metricReplacer = (List<Map<String, String>>) configuration.getConfigYml().get("metricCharacterReplacer");
    }

    public void run() {
        try {
            Phaser phaser = new Phaser();
            phaser.register();
            MetricStat.MetricStats metricConfig = (MetricStat.MetricStats) configuration.getMetricsXml();

            LinuxMetricsTask linuxMetricsTask = new LinuxMetricsTask(metricConfig.getMetricStats(), metricPrefix, configuration, metricWriter, phaser, metricReplacer);
            configuration.getContext().getExecutorService().execute("LinuxMetricsTask", linuxMetricsTask);
            logger.debug("Registering phaser for LinuxMetricsTask" );

            NFSMountMetricsTask nfsMountMetricsTask = new NFSMountMetricsTask(metricConfig.getMetricStats(), metricPrefix, configuration, metricWriter, phaser, metricReplacer);
            configuration.getContext().getExecutorService().execute("NFSMountMetricCollectorTask", nfsMountMetricsTask);
            logger.debug("Registering phaser for NFSMountMetricCollectorTask" );

            //Wait for all tasks to finish
            phaser.arriveAndAwaitAdvance();
            logger.info("Completed the Linux Monitoring task");

        }catch(Exception e) {
            logger.error("Unexpected error while running the Linux Monitor", e);
        }
    }


    public void onTaskComplete() {
        logger.info("All tasks for Linux Monitor finished");
    }
}
