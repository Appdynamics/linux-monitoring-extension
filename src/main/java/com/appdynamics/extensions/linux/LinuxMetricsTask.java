/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

/**
 * Created by akshay.srivastava on 9/12/18.
 */
public class LinuxMetricsTask implements Runnable {

    public static final Logger logger = Logger.getLogger(LinuxMetricsTask.class);

    private MonitorContextConfiguration configuration;

    private MetricWriteHelper metricWriteHelper;

    private String metricPrefix;

    private Phaser phaser;

    private List<Map<String, String>> metricReplacer;

    public LinuxMetricsTask(String metricPrefix, MonitorContextConfiguration configuration, MetricWriteHelper metricWriteHelper, Phaser phaser, List<Map<String, String>> metricReplacer) {
        this.configuration = configuration;
        this.metricPrefix = metricPrefix;
        this.metricWriteHelper = metricWriteHelper;
        this.phaser = phaser;
        this.metricReplacer = metricReplacer;
        this.phaser.register();
    }

    public void run() {
        try {

            Stats stats = new Stats((List<Map<String, List<Map<String, String>>>>) configuration.getConfigYml().get("metrics"), metricPrefix, metricWriteHelper, metricReplacer);

            logger.debug("Fetched stats from config");
            Map<String, Object> statsMap = new HashMap<String, Object>();
            List<MetricData> list;

            if ((list = stats.getDiskStats((List<String>)configuration.getConfigYml().get("diskIncludes"))) != null) {
                statsMap.put("disk", list);
            }

            if ((list = stats.getCPUStats()) != null) {
                statsMap.put("CPU", list);
            }

            if ((list = stats.getDiskUsage()) != null) {
                statsMap.put("disk usage", list);
            }
            if ((list = stats.getFileStats()) != null) {
                statsMap.put("file", list);
            }
            if ((list = stats.getLoadStats()) != null) {
                statsMap.put("load average", list);
            }
            if ((list = stats.getMemStats()) != null) {
                statsMap.put("memory", list);
            }
            if ((list = stats.getNetStats()) != null) {
                statsMap.put("network", list);
            }
            if ((list = stats.getPageSwapStats()) != null) {
                statsMap.put("page", list);
            }
            if ((list = stats.getProcStats()) != null) {
                statsMap.put("process", list);
            }
            if ((list = stats.getSockStats()) != null) {
                statsMap.put("socket", list);
            }

            logger.debug("StatsMap size: " + statsMap.size());

            stats.printNestedMap(statsMap, metricPrefix);

        }catch(Exception e){
            logger.error("LinuxMetrics Task error: " + e.getMessage());
            metricWriteHelper.printMetric(metricPrefix + "|HeartBeat", BigDecimal.ZERO, "AVG.AVG.IND");

        }finally {
            logger.debug("Linux Metrics Task Phaser arrived ");
            phaser.arriveAndDeregister();
        }
    }
}
