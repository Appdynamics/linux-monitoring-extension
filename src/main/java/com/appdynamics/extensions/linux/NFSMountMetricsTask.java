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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

/**
 * Created by akshays
 */
public class NFSMountMetricsTask implements Runnable {

    private Logger logger = Logger.getLogger(NFSMountMetricsTask.class);

    private static String[] NFS_IO_FILE_STATS = {"rkB_nor/s", "WkB_nor/s", "rkB_dir/s", "WkB_dir/s", "rkB_svr/s", "WkB_svr/s", "ops/s", "rops/s", "wops/s"};

    private String[] command = {"df"};

    private String nfsIOStatsCmd = "nfsiostat";

    private static final String SPACE_REGEX = "[\t ]+";

    private MonitorContextConfiguration configuration;

    private String metricPrefix;

    private Stats stats;

    private Phaser phaser;

    public NFSMountMetricsTask(String metricPrefix, MonitorContextConfiguration configuration, MetricWriteHelper metricWriteHelper, Phaser phaser, List<Map<String, String>> metricReplacer) {
        this.configuration = configuration;
        this.metricPrefix = metricPrefix;
        this.phaser = phaser;
        this.phaser.register();

        stats = new Stats((List<Map<String, List<Map<String, String>>>>) configuration.getConfigYml().get("metrics"), metricPrefix, metricWriteHelper, metricReplacer);
    }

    public void run() {
        try {
            List<MetricData> list;

            logger.debug("Fetched stats from config");
            Map<String, Object> statsMap = new HashMap<String, Object>();

            List<String> mountFilters = ((Map<String, List<String>>) configuration.getConfigYml().get("filters")).get("mountedNFS");

            if ((list = getMountStatus(mountFilters)) != null) {
                statsMap.put("mountedNFSStats",list);
            }

            if ((list = getMountIOStats(mountFilters)) != null) {
                statsMap.put("nfsIOStat",list);
            }

            logger.debug("StatsMap size: " + statsMap.size());

            stats.printNestedMap(statsMap, metricPrefix);
        }catch(Exception e){
            logger.debug("Error fetching NFS metrics: ", e);
        }finally {
            logger.debug("NFS Metrics Task Phaser arrived ");
            phaser.arriveAndDeregister();
        }

    }

    public List<MetricData> getMountStatus(List<String> mountedNFSFilter) {

        Runtime rt = Runtime.getRuntime();
        Process p = null;
        BufferedReader input = null;
        try {
            p = rt.exec(new String[]{"bash", "-c", command[0]});
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            FileParser parser = new FileParser(input, "mountedNFSStatus", null);

            String[] mountedNFSStats = stats.generateStatsArray("mountedNFSStatus");


            FileParser.StatParser statParser = new FileParser.StatParser(mountedNFSStats, SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) { return stats.isFiltered(line, mountedNFSFilter) || (!line.startsWith("Filesystem") && !line.startsWith("none")); }

                @Override
                boolean isBase(String[] stats) {
                    return true;
                }

                @Override
                boolean isCountRequired() {
                    return false;
                }
            };
            parser.addParser(statParser);

            Map<String, Object> parserStats = parser.getStats();

            return stats.generateStatsMap(parserStats, "mountedNFSStatus");
        } catch (IOException e) {
            logger.error("Failed to run for NFS mount ", e);
        } catch (Exception e) {
            logger.error("Exception occurred collecting NFS metrics", e);
        }

        return new ArrayList<>();
    }



    public List<MetricData> getMountIOStats(List<String> mountedNFSFilter) {

        BufferedReader reader;
        try {
            Process process = Runtime.getRuntime().exec(nfsIOStatsCmd);
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            FileParser parser = new FileParser(reader, "nfsIOStats", null);

            String[] mountedNFSStats = stats.generateStatsArray("nfsIOStats");

            FileParser.StatParser statParser = new FileParser.StatParser(mountedNFSStats, SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) { return stats.isFiltered(line, mountedNFSFilter) || (!line.startsWith("Filesystem") && !line.startsWith("none")); }

                @Override
                boolean isBase(String[] stats) {
                    return true;
                }

                @Override
                boolean isCountRequired() {
                    return false;
                }
            };
            parser.addParser(statParser);

            Map<String, Object> parserStats = parser.getStats();

            return stats.generateStatsMap(parserStats, "nfsIOStats");
        }catch (IOException e) {
            logger.error("Failed to run '" + nfsIOStatsCmd + "' for disk usage");
        } catch (Exception e) {
            logger.error("Exception occurred collecting disk usage metrics", e);
        }
        return new ArrayList<>();
    }

}
