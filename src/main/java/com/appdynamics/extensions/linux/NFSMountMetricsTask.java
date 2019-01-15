/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.linux.input.MetricConfig;
import com.appdynamics.extensions.linux.input.MetricStat;
import com.appdynamics.extensions.metrics.Metric;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

/**
 * Created by akshay.srivastava on 9/12/18
 */
public class NFSMountMetricsTask implements Runnable {

    private Logger logger = Logger.getLogger(NFSMountMetricsTask.class);

    private String[] command = {"df"};

    private String nfsIOStatsCmd = "nfsiostat";

    private static final String SPACE_REGEX = "[\t ]+";

    private MonitorContextConfiguration configuration;

    private Stats stats;

    private Phaser phaser;

    private MetricWriteHelper metricWriteHelper;

    private MetricStat[] metricStats;

    public NFSMountMetricsTask(MetricStat[] metricStats, String metricPrefix, MonitorContextConfiguration configuration, MetricWriteHelper metricWriteHelper, Phaser phaser, List<Map<String, String>> metricReplacer) {
        this.configuration = configuration;
        this.metricWriteHelper = metricWriteHelper;
        this.metricStats = metricStats;
        this.phaser = phaser;
        this.phaser.register();

        stats = new Stats(metricPrefix, metricStats, metricReplacer);
    }

    public void run() {
        try {
            List<Metric> metrics = new ArrayList<>();

            logger.debug("Fetched metricStats from config");

            List<String> mountFilters = ((Map<String, List<String>>) configuration.getConfigYml().get("filters")).get("mountedNFS");

            metrics.addAll(getMountStatus(mountFilters));
            metrics.addAll(getMountIOStats(mountFilters));

            if (metrics != null && metrics.size() > 0) {
                metricWriteHelper.transformAndPrintMetrics(metrics);
            }

        }catch(Exception e){
            logger.debug("Error fetching NFS metrics: ", e);
        }finally {
            logger.debug("NFS Metrics Task Phaser arrived ");
            phaser.arriveAndDeregister();
        }

    }

    public List<Metric> getMountStatus(List<String> mountedNFSFilter) {

        Runtime rt = Runtime.getRuntime();
        Process p = null;
        BufferedReader input = null;
        List<Metric> metricData = new ArrayList<>();

        try {
            p = rt.exec(new String[]{"bash", "-c", command[0]});
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            logger.debug("Inside mountnfsStatus");
            FileParser parser = new FileParser(input, "mountedNFSStatus");

            MetricConfig[] statArray = null;

            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("mountedNFSStatus")) {
                    statArray = metricStats[i].getMetricConfig();
                }
            }
            FileParser.StatParser statParser = new FileParser.StatParser(statArray, SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return stats.isFiltered(line, mountedNFSFilter) && (!line.startsWith("Filesystem") && !line.startsWith("none"));
                }

                @Override
                boolean isBase(String[] stats) {
                    return false;
                }
            };
            parser.addParser(statParser);

            Map<String, Object> parserStats = parser.getStats();

            for(Map.Entry entry: parserStats.entrySet()){
                metricData.addAll(stats.generateMetrics((Map<String, String>)entry.getValue(), "mountedNFSStatus", String.valueOf(entry.getKey())));
            }
        } catch (IOException e) {
            logger.error("Failed to run for NFS mount ", e);
        } catch (Exception e) {
            logger.error("Exception occurred collecting NFS mount metrics", e);
        }

        return metricData;
    }



    public List<Metric> getMountIOStats(List<String> mountedNFSFilter) {

        BufferedReader reader;
        List<Metric> metricData = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(nfsIOStatsCmd);
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            FileParser parser = new FileParser(reader, "nfsIOStats");

            MetricConfig[] statArray = null;
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("nfsIOStats")) {
                    statArray = metricStats[i].getMetricConfig();
                }
            }
            FileParser.StatParser statParser = new FileParser.StatParser(statArray, SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) { return stats.isFiltered(line, mountedNFSFilter) && (!line.startsWith("Filesystem") && !line.startsWith("none")); }

                @Override
                boolean isBase(String[] stats) {
                    return false;
                }
            };
            parser.addParser(statParser);

            Map<String, Object> parserStats = parser.getStats();

            for(Map.Entry entry: parserStats.entrySet()){
                metricData.addAll(stats.generateMetrics((Map<String, String>)entry.getValue(), "nfsIOStats", String.valueOf(entry.getKey())));
            }
        }catch (IOException ex) {
            logger.error("Failed to run '" + nfsIOStatsCmd + "' for NFS I/O", ex);
        } catch (Exception e) {
            logger.error("Exception occurred collecting NFS I/O metrics", e);
        }
        return metricData;
    }
}
