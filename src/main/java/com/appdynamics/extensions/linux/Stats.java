/*
 * Copyright 2018. AppDynamics LLC and its `filiates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.linux.input.MetricConfig;
import com.appdynamics.extensions.linux.input.MetricStat;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.base.Joiner;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Stats {

    private static Logger logger  = Logger.getLogger(Stats.class);

    private List<Map<String, String>> metricReplacer;

    protected MetricStat[] metricStats;

    private String metricPrefix;

    private static ObjectMapper objectMapper = new ObjectMapper();

    public Stats(String metricPrefix, MetricStat[] metricStats, List<Map<String, String>> metricReplacer) {
        this.metricPrefix = metricPrefix;
        this.metricReplacer = metricReplacer;
        this.metricStats = metricStats;
    }

    protected BufferedReader getStream(String filePath) {
        File file = new File(filePath);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            logger.error("File '" + filePath + "' not found");
        }
        return reader;
    }

    public List<Metric> getCPUStats(final List<String> cpuIncludes) {

        logger.debug("Fetching CPU metricStats");
        BufferedReader reader = getStream(Constants.STAT_PATH);
        FileParser parser = new FileParser(reader, "CPU");
        List<Metric> metricData = new ArrayList<>();

        try{
            MetricConfig[] statArray = null;

            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("cpuStats")) {
                    statArray = metricStats[i].getMetricConfig();
                }
            }
            FileParser.StatParser statParser = new FileParser.StatParser(statArray, Constants.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return  isFiltered(line, cpuIncludes) && line.startsWith("cpu");
                }

                @Override
                boolean isBase(String[] stats) {
                    return false;
                }
            };
            parser.addParser(statParser);

            Map<String, Object> parserStats = parser.getStats();

            for(Map.Entry entry: parserStats.entrySet()){
                metricData.addAll(generateMetrics((Map<String, String>)entry.getValue(), "cpuStats", String.valueOf(entry.getKey())));
            }
            Metric countMetric = new Metric("CPU (Cores) Logical", String.valueOf(parserStats.entrySet().size()), metricPrefix + "|cpu|CPU (Cores) Logical" );

            metricData.add(countMetric);
        }catch(Exception e){
            logger.error("Exception fetching CPU metricStats", e);
        }
        return metricData;
    }

    public List<Metric> getDiskStats(final List<String> diskIncludes) {
        BufferedReader reader = getStream(Constants.DISK_STAT_PATH);
        logger.debug("Fetching disk metricStats for " + diskIncludes);
        FileParser parser = new FileParser(reader, "diskStats");
        List<Metric> metricData = new ArrayList<>();

        try {
            MetricConfig[] statArray = null;
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("diskStats")) {
                    statArray = metricStats[i].getMetricConfig();
                }
            }

            FileParser.StatParser statParser = new FileParser.StatParser(statArray, Constants.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return isFiltered(line, diskIncludes);
                }

                @Override
                boolean isBase(String[] stats) {
                    return false;
                }

                @Override
                int getNameIndex(){
                    return 2;
                }
            };
            parser.addParser(statParser);
            Map<String, Object> parserStats = parser.getStats();

            for(Map.Entry entry: parserStats.entrySet()){
                metricData.addAll(generateMetrics((Map<String, String>)entry.getValue(), "diskStats", String.valueOf(entry.getKey())));
            }
        }catch(Exception e){
            logger.error("Exception fetching disk metricStats", e);
        }
        return metricData;
    }

    public List<Metric> getNetStats() {
        logger.debug("Fetching net metricStats");
        BufferedReader reader = getStream(Constants.NET_STAT_PATH);
        FileParser parser = new FileParser(reader, "net");
        List<Metric> metricData = new ArrayList<>();

        try {
            MetricConfig[] statArray = null;
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("netStats")) {
                    statArray = metricStats[i].getMetricConfig();
                }
            }

            FileParser.StatParser statParser = new FileParser.StatParser(statArray, Constants.SPACE_COLON_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return !line.contains("|");
                }

                @Override
                boolean isBase(String[] stats) {
                    return false;
                }
            };
            parser.addParser(statParser);

            Map<String, Object> parserStats = parser.getStats();

            for(Map.Entry entry: parserStats.entrySet()){
                metricData.addAll(generateMetrics((Map<String, String>)entry.getValue(), "netStats", String.valueOf(entry.getKey())));
            }
        }catch(Exception e){
            logger.error("Exception fetching net metricStats", e);
        }

        return metricData;
    }

    public List<Metric> getDiskUsage() {
        logger.debug("Fetching diskusage metricStats");
        BufferedReader reader;
        Map<String, Object> stats = new HashMap<>();
        try {
            Process process = Runtime.getRuntime().exec(Constants.DISK_USAGE_CMD);
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            FileParser parser = new FileParser(reader, "disk usage");

            MetricConfig[] statArray = null;
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("diskUsageStats")) {
                    statArray = metricStats[i].getMetricConfig();
                }
            }

            FileParser.StatParser statParser = new FileParser.StatParser(statArray, Constants.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return !line.startsWith("Filesystem") && !line.startsWith("none");
                }
                @Override
                boolean isBase(String[] stats) {
                    return false;
                }
            };
            parser.addParser(statParser);
            stats = parser.getStats();

        } catch (IOException ex) {
            logger.error("Failed to run '" + Constants.DISK_USAGE_CMD + "' for disk usage", ex);
        } catch (Exception e) {
            logger.error("Exception occurred collecting disk usage metrics", e);
        }

        return generateMetrics(stats, "diskUsageStats");
    }

    public List<Metric> getFileStats() {
        logger.debug("Fetching file metricStats");
        BufferedReader fhReader = getStream(Constants.FILE_NR_STAT_PATH);
        BufferedReader inodeReader = getStream(Constants.INODE_NR_STAT_PATH);
        BufferedReader dentriesReader = getStream(Constants.DENTRIES_STAT_PATH);

        List<Metric> statsMetrics = new ArrayList<Metric>();

        try {
            logger.debug("Fetching File NR metricStats");
            MetricConfig[] statArray = null;
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("fileNRStats")) {
                    statArray = metricStats[i].getMetricConfig();
                }
            }
            FileParser parser = new FileParser(fhReader, "file handler");
            FileParser.StatParser statParser = new FileParser.StatParser(statArray, Constants.SPACE_REGEX){};
            parser.addParser(statParser);
            List<Metric> metrics = generateMetrics(parser.getStats(), "fileNRStats");
            if (metrics != null) {
                statsMetrics.addAll(metrics);
            }

            logger.debug("Fetching iNode NR Stats");

            MetricConfig[] inodeStatArray = null;
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("inodeNRStats")) {
                    inodeStatArray = metricStats[i].getMetricConfig();
                }
            }

            parser = new FileParser(inodeReader, "inode");
            statParser = new FileParser.StatParser(inodeStatArray, Constants.SPACE_REGEX) {
            };
            parser.addParser(statParser);

            metrics = generateMetrics(parser.getStats(), "iNodeNRStats");
            if (metrics != null) {
                statsMetrics.addAll(metrics);
            }

            logger.debug("Fetching dentries Stats");

            MetricConfig[] dentriesStats = null;
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("dentriesStats")) {
                    dentriesStats = metricStats[i].getMetricConfig();
                }
            }

            parser = new FileParser(dentriesReader, "dcache");
            statParser = new FileParser.StatParser(dentriesStats, Constants.SPACE_REGEX) {};
            parser.addParser(statParser);
            metrics = generateMetrics(parser.getStats(), "dentriesStats");
            if (metrics != null) {
                statsMetrics.addAll(metrics);
            }
        }catch(Exception e) {
            logger.error("Exception collection metrics for file: ", e);
        }
        return statsMetrics;
    }

    public List<Metric> getLoadStats() {
        logger.debug("Fetching load Stats");
        BufferedReader reader = getStream(Constants.LOADAVG_STAT_PATH);
        List<Metric> metricData = new ArrayList<>();
        try {
            MetricConfig[] statArray = null;
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("loadAvgStats")) {
                    statArray = metricStats[i].getMetricConfig();
                }
            }

            FileParser parser = new FileParser(reader, "load average");
            FileParser.StatParser statParser = new FileParser.StatParser(statArray, Constants.SPACE_REGEX) {
            };
            parser.addParser(statParser);

            metricData.addAll(generateMetrics(parser.getStats(), "loadAvgStats"));
        }catch(Exception e){
        logger.error("Exception fetching load metricStats", e);
        }
        return metricData;
    }

    public List<Metric> getMemStats() {
        logger.debug("Fetching Memory Stats");
        BufferedReader reader = getStream(Constants.MEM_STAT_PATH);
        List<Metric> metricData = new ArrayList<>();
        try {
            Map<String, Object> statsMap = getRowStats(reader, Constants.SPACE_COLON_REGEX,
                    Constants.MEM_FILE_STATS, 0, 1);

            metricData.addAll(generateMetrics(statsMap,"memStats"));
        }catch(Exception e){
            logger.error("Exception fetching load metricStats", e);
        }
        return metricData;

    }

    public List< Metric> getPageSwapStats() {
        BufferedReader reader = getStream(Constants.STAT_PATH);
        Map<String, Object> statsMap;
        List<Metric> pageMetrics = new ArrayList<>();

        try{
            logger.debug("Fetching page metricStats");
            MetricConfig[] pageStats = null;
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("pageStats")) {
                    pageStats = metricStats[i].getMetricConfig();
                }
            }

            FileParser parser = new FileParser(reader, "page");
            FileParser.StatParser pageParser = new FileParser.StatParser(pageStats, Constants.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return line.startsWith("page");
                }
            };
            parser.addParser(pageParser);

            logger.debug("Fetching swap metricStats");
            MetricConfig[] swapStats = null;
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("swapStats")) {
                    swapStats = metricStats[i].getMetricConfig();
                }
            }

            FileParser.StatParser swapParser = new FileParser.StatParser(swapStats, Constants.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return line.startsWith("swap");
                }
            };
            parser.addParser(swapParser);

            logger.debug("Fetching pageSwap metricStats");
            statsMap = parser.getStats();
            if (statsMap == null) {  //page and swap are not in /proc/stat
                reader = getStream(Constants.VM_STAT_PATH);
                statsMap = getRowStats(reader, Constants.SPACE_REGEX, Constants.PAGE_SWAP_FILE_STATS, 0, 1);
            }
            pageMetrics.addAll(generateMetrics(statsMap,"pageStats"));
            pageMetrics.addAll(generateMetrics(statsMap,"swapStats"));
            pageMetrics.addAll(generateMetrics(statsMap,"pageSwapStats"));
        }catch(Exception e){
            logger.error("Exception fetching swap metricStats", e);
        }
        return pageMetrics;
    }

    public List<Metric> getProcStats() {
        BufferedReader reader = getStream(Constants.STAT_PATH);

        List<Metric> procMetrics = new ArrayList<>();

        try {
            Map<String, Object> statsMap = getRowStats(reader, Constants.SPACE_REGEX, Constants.PROC_FILE_STATS,0, 1);
            logger.debug("Fetching process metricStats");
            procMetrics.addAll(generateMetrics(statsMap, "procStats"));

            reader = getStream(Constants.LOADAVG_STAT_PATH);
            FileParser parser = new FileParser(reader, "process");

            MetricConfig[] statsArray = null;
            for(int i=0; i<metricStats.length; i++){
                if(metricStats[i].getName().equalsIgnoreCase("procStats")) {
                    statsArray = metricStats[i].getMetricConfig();
                }
            }
            FileParser.StatParser statParser = new FileParser.StatParser(statsArray, Constants.SPACE_REGEX + "|/") {};
            parser.addParser(statParser);
            Map<String, Object> map = parser.getStats();
            if (map != null) {
                procMetrics.addAll(generateMetrics(map, "procLoadAvgStats"));
            }
        }catch(Exception e){
            logger.error("Exception fetching process metricStats", e);
        }

        return procMetrics;
    }

    public List<Metric> getSockStats() {
        BufferedReader reader = getStream(Constants.SOCK_STAT_PATH);
        logger.debug("Fetching socket metricStats");
        List<Metric> sockStats = new ArrayList<>();
        try {
            Map<String, Object> statsMap = getRowStats(reader, Constants.SPACE_REGEX, Constants.SOCK_STATS,0, 2);
            sockStats.addAll(generateMetrics(statsMap, "sockUsedStats"));
        }catch(Exception e){
            logger.error("Exception fetching socket metricStats", e);
        }
        return sockStats;
    }


    protected Map<String, Object> getRowStats(BufferedReader reader, String splitRegex,
                                            String[] fileKey,
                                            int keyIndex, int valIndex) {
        Map<String, Object> statsMap = null;
        if (reader == null) {
            logger.error("Failed to read metricStats as reader is null");
        } else {
            statsMap = new HashMap<>();

            try {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        String[] stats = line.split(splitRegex);
                        String name = null;
                        for(int j=0; j<fileKey.length; j++){
                           if(fileKey[j].equalsIgnoreCase(stats[keyIndex]))
                              name = fileKey[j];
                        }
                        if (name != null)
                            statsMap.put(name, stats[valIndex]);
                    }
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                logger.error("Failed to read metricStats: ", e);
            }
        }
        return statsMap;
    }

    protected List<Metric> generateMetrics(Map<String, Object> statsMap, String metricName){

        List<Metric> metricDataList = new ArrayList<>();
        MetricConfig[] metricConfig = null;
        if(statsMap!=null) {
            for (Map.Entry<String, Object> statsEntry : statsMap.entrySet()) {

                for(int i=0; i<metricStats.length; i++){
                    if(metricStats[i].getName().equalsIgnoreCase(metricName)) {
                        metricConfig = metricStats[i].getMetricConfig();
                    }
                }
                for (MetricConfig metrics : metricConfig) {
                    if (metrics != null && metrics.getAttr().equalsIgnoreCase(statsEntry.getKey())) {
                        Map<String, String> propertiesMap = objectMapper.convertValue(metrics, Map.class);
                        Metric metric = new Metric(metrics.getAttr(), replaceCharacter(String.valueOf(statsEntry.getValue())), metricPrefix + "|" + metricName + "|" + metrics.getAlias(), propertiesMap);
                        metricDataList.add(metric);
                    }
                }
            }
        }else{
            logger.debug("No metricStats found for: " + metricName);
        }
        logger.debug("Returning " + metricDataList.size() + " metrics for " + metricName);

        return metricDataList;
    }

    protected List<Metric> generateMetrics(Map<String, String> statsMap, String metricName, String systemName){

        List<Metric> metricDataList = new ArrayList<>();
        MetricConfig[] metricConfig = null;
        if(statsMap!=null) {
            for (Map.Entry<String, String> statsEntry : statsMap.entrySet()) {

                for(int i=0; i<metricStats.length; i++){
                    if(metricStats[i].getName().equalsIgnoreCase(metricName)) {
                        metricConfig = metricStats[i].getMetricConfig();
                    }
                }
                for (MetricConfig metrics : metricConfig) {
                    if (metrics != null && metrics.getAttr().equalsIgnoreCase(statsEntry.getKey())) {
                        Map<String, String> propertiesMap = objectMapper.convertValue(metrics, Map.class);
                        Metric metric = new Metric(systemName + "|" + metrics.getAttr(), replaceCharacter(String.valueOf(statsEntry.getValue())), metricPrefix + "|" + metricName + "|" +systemName + "|" + metrics.getAlias(), propertiesMap);

                        metricDataList.add(metric);
                    }
                }
            }
        }else{
            logger.debug("No metricStats found for: " + metricName);
        }
        logger.debug("Returning " + metricDataList.size() + " metrics for " + metricName);
        return metricDataList;
    }



    public boolean isFiltered(String line, List<String> filterIncludes) {
        if (filterIncludes == null || filterIncludes.isEmpty()) {
            return false;
        }
        if (filterIncludes.contains("*")) {
            return true;
        }
        Joiner joiner = Joiner.on("|");
        String includePattern = joiner.join(filterIncludes);
        includePattern = "\\b(" + includePattern + ")\\b";
        Pattern pattern = Pattern.compile(includePattern);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return true;
        }
        return false;
    }

    private String replaceCharacter(String metric) {

        for (Map chars : metricReplacer) {
            String replace = (String) chars.get("replace");
            String replaceWith = (String) chars.get("replaceWith");

            if (metric.contains(replace)) {
                metric = metric.replaceAll(replace, replaceWith);
            }
        }
        return metric;
    }
}
