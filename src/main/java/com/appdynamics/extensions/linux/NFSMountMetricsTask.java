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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

/**
 * Created by akshay.srivastava on 9/12/18
 */
public class NFSMountMetricsTask implements Runnable {

    private Logger logger = Logger.getLogger(NFSMountMetricsTask.class);

    private static final String SPACE_REGEX = "[\t ]+";

    private MonitorContextConfiguration configuration;

    private Stats stats;

    private Phaser phaser;

    private MetricWriteHelper metricWriteHelper;

    private MetricStat[] metricStats;

    private String metricPrefix;

    private List<Metric> metrics = new ArrayList<>();

    public NFSMountMetricsTask(MetricStat[] metricStats, String metricPrefix, MonitorContextConfiguration configuration, MetricWriteHelper metricWriteHelper, Phaser phaser, List<Map<String, String>> metricReplacer) {
        this.configuration = configuration;
        this.metricWriteHelper = metricWriteHelper;
        this.metricStats = metricStats;
        this.phaser = phaser;
        this.metricPrefix = metricPrefix;
        this.phaser.register();

        stats = new Stats(metricPrefix, metricStats, metricReplacer);
    }

    public void run() {
        try {
            List<Map<String, String>> nfsMounts = (ArrayList<Map<String, String>>) configuration.getConfigYml().get("mountedNFS");

            for(Map<String, String> mountedNFS : nfsMounts) {
                metrics.addAll(getMountStatus(mountedNFS));
            }
            metrics.addAll(getMountIOStats(nfsMounts));

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

    public List<Metric> getMountStatus(Map<String, String> mountedNFS) {

        Runtime rt = Runtime.getRuntime();
        Process p = null;
        BufferedReader input = null;
        List<Metric> metricData = new ArrayList<>();
        int available = 0;
        try {
            MetricConfig[] statArray = null;
            String command = "";
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("mountedNFSStatus")) {
                    command = metricStats[i].getCommand();
                    statArray = metricStats[i].getMetricConfig();
                }
            }

            input = new BufferedReader( new StringReader(String.join("\n", CommandExecutor.execute(new String[]{"bash", "-c", command}))));

            logger.debug("Inside mountnfsStatus");
            FileParser parser = new FileParser(input, "mountedNFSStatus");


            FileParser.StatParser statParser = new FileParser.StatParser(statArray, SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return (!line.startsWith("Filesystem") && !line.startsWith("none"));
                }

                @Override
                boolean isBase(String[] stats) {
                    return false;
                }

                @Override
                int getNameIndex(){
                    return 5;
                }
            };
            parser.addParser(statParser);

            Map<String, Object> parserStats = parser.getStats();

            for(Map.Entry entry: parserStats.entrySet()){
                if(entry.getKey().equals(mountedNFS.get("fileSystem"))) {
                    metricData.addAll(stats.generateMetrics((Map<String, String>) entry.getValue(), "mountedNFSStatus", mountedNFS.get("displayName")));
                    available = 1;
                }
            }
            metricData.add(new Metric(mountedNFS.get("displayName") + "|Availability", String.valueOf(available), metricPrefix + "|mountedNFSStatus|" +mountedNFS.get("displayName") + "|Availability"));
        } catch (Exception e) {
            logger.error("Exception occurred collecting NFS mount metrics", e);
        }

        return metricData;
    }

    public List<Metric> getMountIOStats(List<Map<String, String>> mountedNFSMap) {

        BufferedReader reader;
        List<Metric> metricData = new ArrayList<>();
        String command = "";

        MetricConfig[] statArray = null;
        for (int i = 0; i < metricStats.length; i++) {
            if (metricStats[i].getName().equalsIgnoreCase("nfsIOStats")) {
                command = metricStats[i].getCommand();
                statArray = metricStats[i].getMetricConfig();
            }
        }
        for(Map mountedNFS : mountedNFSMap) {
            try {
                StringBuffer data = new StringBuffer();
                Iterator itr = CommandExecutor.execute(new String[]{"bash", "-c", command + " " + mountedNFS.get("fileSystem").toString() }).iterator();
                while (itr.hasNext()){
                    String line = (String)itr.next();
                    if(line.startsWith("read") || line.startsWith("write"))
                        data.append((String)itr.next()).append(" ");
                }

                reader = new BufferedReader(new StringReader(data.toString()));

                FileParser parser = new FileParser(reader, "nfsIOStats");

                FileParser.StatParser statParser = new FileParser.StatParser(statArray, SPACE_REGEX) {
                    @Override
                    boolean isBase(String[] stats) {
                        return false;
                    }
                };
                parser.addParser(statParser);

                for (Map.Entry entry : parser.getStats().entrySet()) {
                    metricData.addAll(stats.generateMetrics((Map<String, String>) entry.getValue(), "nfsIOStats", mountedNFS.get("displayName").toString()));
                }
            } catch (Exception e) {
                logger.error("Exception occurred collecting NFS I/O metrics", e);
            }
        }
        return metricData;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }
}
