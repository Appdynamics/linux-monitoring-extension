/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.linux.input.MetricStat;
import com.appdynamics.extensions.metrics.Metric;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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

    private Stats stats;

    private List<Metric> metrics = new ArrayList<>();

    public LinuxMetricsTask(MetricStat[] metricStats, String metricPrefix, MonitorContextConfiguration configuration, MetricWriteHelper metricWriteHelper, Phaser phaser, List<Map<String, String>> metricReplacer) {
        this.configuration = configuration;
        this.metricPrefix = metricPrefix;
        this.metricWriteHelper = metricWriteHelper;
        this.phaser = phaser;
        this.phaser.register();

        stats = new Stats(metricPrefix, metricStats, metricReplacer);
    }

    public void run() {
        try {
            logger.debug("Fetched metricStats from config");
            Map<String, List<String>> filtersMap = (Map<String, List<String>>) configuration.getConfigYml().get("filters");

            metrics.addAll(stats.getDiskStats(filtersMap.get("diskIncludes")));
            metrics.addAll(stats.getCPUStats(filtersMap.get("cpuIncludes")));
            metrics.addAll(stats.getDiskUsage());
            metrics.addAll(stats.getFileStats());
            metrics.addAll(stats.getLoadStats());
            metrics.addAll(stats.getMemStats());
            metrics.addAll(stats.getNetStats());
            metrics.addAll(stats.getPageSwapStats());
            metrics.addAll(stats.getProcStats());
            metrics.addAll(stats.getSockStats());

            metrics.add(new Metric("HeartBeat", String.valueOf(BigInteger.ONE), metricPrefix + "|HeartBeat", "AVG", "AVG", "IND"));

            if (metrics != null && metrics.size() > 0) {
                metricWriteHelper.transformAndPrintMetrics(metrics);
            }

        } catch (Exception e) {
            logger.error("LinuxMetrics Task error: " , e);
            metricWriteHelper.printMetric(metricPrefix + "|HeartBeat", BigDecimal.ZERO, "AVG.AVG.IND");

        } finally {
            logger.debug("Linux Metrics Task Phaser arrived ");
            phaser.arriveAndDeregister();
        }
    }

    public List<Metric> getMetrics() {
        return metrics;
    }
}
