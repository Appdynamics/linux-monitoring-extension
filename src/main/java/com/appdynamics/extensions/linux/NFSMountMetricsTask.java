/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.linux.config.MountedNFS;
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
 * Created by balakrishnav on 19/10/15.
 */
public class NFSMountMetricsTask implements Runnable {

    private Logger logger = Logger.getLogger(NFSMountMetricsTask.class);

    private static String[] NFS_IO_FILE_STATS = {"rkB_nor/s", "WkB_nor/s", "rkB_dir/s", "WkB_dir/s", "rkB_svr/s", "WkB_svr/s", "ops/s", "rops/s", "wops/s"};

    private String[] command = {"df | grep %s "};

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
            List<Map> nfsConfig = (List<Map>) configuration.getConfigYml().get("mountedNFS");
            if (nfsConfig != null) { //Null check to MountedNFS

                MountedNFS[] mountedNFS = new MountedNFS[nfsConfig.size()];
                int i = 0;
                for (Map configEntry : nfsConfig) {
                    MountedNFS nfs = new MountedNFS();
                    nfs.setDisplayName((String) configEntry.get("displayName"));
                    nfs.setFileSystem((String) configEntry.get("fileSystem"));
                    mountedNFS[i++] = nfs;
                }

                List<MetricData> nfsStats = new ArrayList<>();

                if ((list = getMountStatus(mountedNFS)) != null) {
                    statsMap.put("mountedNFSStats",list);
                }

                if ((list = getMountIOStats(mountedNFS)) != null) {
                    statsMap.put("nfsIOStat",list);
                }
                //statsMap.put("mountedNFSStats", nfsStats);
            } else {
                logger.info("NFS mount is null");
            }
            logger.debug("StatsMap size: " + statsMap.size());

            stats.printNestedMap(statsMap, metricPrefix);
        }catch(Exception e){
            logger.debug("Error fetching NFS metrics: ", e);
        }finally {
            logger.debug("Linux Metrics Task Phaser arrived ");
            phaser.arriveAndDeregister();
        }

    }

    public List<MetricData> execute(String fileSystem) {

        Runtime rt = Runtime.getRuntime();
        Process p = null;
        BufferedReader input = null;
        String formattedCommand = "";
        try {
            formattedCommand = String.format(command[0], fileSystem);
            p = rt.exec(new String[]{"bash", "-c", formattedCommand});
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            /*String line;
            if ((line = input.readLine()) != null) {

                logger.debug("NFS mount output for "+ fileSystem + " is: " + line);
                return line;
            }*/

            FileParser parser = new FileParser(input, "mountedNFSStatus", null);

            String[] diskUsageStats = stats.generateStatsArray("mountedNFSStatus");

            logger.debug("In nfsmountedstats");
            for(int i=0; i<diskUsageStats.length;i++){
                logger.debug(diskUsageStats[i]);
            }

            FileParser.StatParser statParser = new FileParser.StatParser(diskUsageStats, SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return !line.startsWith("Filesystem") && !line.startsWith("none");
                }

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
            // Check, if use % metrics is already being created, then remove this code piece
            /*
            for (Map.Entry<String, Object> diskStats : stats.entrySet()) {
                Map<String, Object> diskStat = (Map) diskStats.getValue();
                String capacityValue = (String) diskStat.get("use %");
                diskStat.put("use %", capacityValue.replace("%", "").trim());
            }*/
            Map<String, Object> parserStats = parser.getStats();
            for(Map.Entry entry:  parserStats.entrySet()){
                logger.debug("Key: " + entry.getKey() + " Value: " + entry.getValue());
            }

            return stats.generateStatsMap(parserStats, "mountedNFSStatus");
        } catch (IOException e) {
            logger.error("Failed to run " + " disk usage for NFS mount: " + fileSystem);
        } catch (Exception e) {
            logger.error("Exception occurred collecting disk usage metrics", e);
        }

        return new ArrayList<>();
    }

    public Map<String, Object> getNFSMetrics(final MountedNFS fileSystem){

            String formattedCommand = "";

            Map<String, Object> statsMap = new HashMap<String, Object>();
            try {
                formattedCommand = String.format(nfsIOStatsCmd, fileSystem.getFileSystem());

               List<String> processListOutput = CommandExecutor.execute(formattedCommand);

               logger.debug("IO stat command output: " + processListOutput.size());
               for(String line: processListOutput){
                   logger.debug("NFS output: " + line);
                    if(line.contains(fileSystem.getFileSystem())) {
                        String[] stats = line.trim().split(SPACE_REGEX);
                        for (int i = 0; i < NFS_IO_FILE_STATS.length; i++) {
                            statsMap.put(NFS_IO_FILE_STATS[i], stats[i+1]);
                        }
                    }
                 }

            } catch (Exception e) {
                logger.error("Command ran '" + nfsIOStatsCmd + "' for nfsIOStats, exception occurred:" + e);
            }
            logger.debug("Size of NFS iostat statsMap is: " + statsMap.size());
            return statsMap;
        }

    public List<MetricData> getMountStatus(MountedNFS[] mountedNFS) {

        logger.debug("Fetching mount stats");
        List<MetricData> metrics = new ArrayList<>();
        for (MountedNFS fileSystem : mountedNFS) {
             metrics =  execute(fileSystem.getFileSystem());
        }
        return metrics;

    }

    public List<MetricData> getMountIOStats(MountedNFS[] mountedNFS) {
        logger.debug("Fetching mountIO stats");
        List<MetricData> ioStats = new ArrayList<MetricData>();

        for (MountedNFS fileSystem : mountedNFS) {
            Map<String,Object> statsMap = getNFSMetrics(fileSystem);

            List<MetricData> metricStats = new ArrayList<MetricData>();

            for(Map.Entry statsEntry : statsMap.entrySet()) {

                logger.debug("NFS metric: " + String.valueOf(statsEntry.getKey()));
                for (Map<String, String> metrics : Stats.allMetricsFromConfig.get("nfsIOStats")) {

                    if (metrics.get("name").equalsIgnoreCase(String.valueOf(statsEntry.getKey()))) {

                        MetricData metricData = new MetricData();
                        metricData.setStats(statsEntry.getValue());
                        metricData.setName(fileSystem.getDisplayName() + "|" + metrics.get("name"));
                        metricData.setCollectDelta(Boolean.valueOf(metrics.get("collectDelta")));
                        metricData.setMultiplier(metrics.get("multiplier"));
                        metricData.setMetricType(metrics.get("metricType"));
                        metricData.constructProperties();

                        metricStats.add(metricData);
                        logger.debug("NFS Metric Data: " + metricData.getName() + ": " + metricData.getStats().toString());
                    }
                }
            }
            ioStats.addAll(metricStats);
        }
        return ioStats;

    }
}
