/*
 * Copyright 2018. AppDynamics LLC and its `filiates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.MetricWriteHelper;
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
import java.util.Arrays;
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


    private BufferedReader getStream(String filePath) {
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
        BufferedReader reader = getStream(Commands.STAT_PATH);
        FileParser parser = new FileParser(reader, "CPU");
        List<Metric> metricData = new ArrayList<>();

        try{
            String[] CPUStats = generateStatsArray("cpuStats");

            FileParser.StatParser statParser = new FileParser.StatParser(CPUStats, Commands.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return  isFiltered(line, cpuIncludes);
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
            Metric countMetric = new Metric("CPU (Cores) Logical", String.valueOf(parserStats.entrySet().size()), metricPrefix + "cpu|");

            metricData.add(countMetric);
        }catch(Exception e){
            logger.debug("Exception fetching CPU metricStats", e);
        }
        return metricData;
    }

    public List<Metric> getDiskStats(final List<String> diskIncludes) {
        BufferedReader reader = getStream(Commands.DISK_STAT_PATH);
        logger.debug("Fetching disk metricStats for " + diskIncludes);
        FileParser parser = new FileParser(reader, "disk");
        List<Metric> metricData = new ArrayList<>();

        try {
            String[] diskStats = generateStatsArray("diskStats");

            FileParser.StatParser statParser = new FileParser.StatParser(diskStats, Commands.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return isFiltered(line, diskIncludes);
                }
            };
            parser.addParser(statParser);
            metricData.addAll(generateMetrics(parser.getStats(), "diskStats"));
        }catch(Exception e){
            logger.debug("Exception fetching disk metricStats", e);
        }
        return metricData;
    }

    public List<Metric> getNetStats() {
        logger.debug("Fetching net metricStats");
        BufferedReader reader = getStream(Commands.NET_STAT_PATH);
        FileParser parser = new FileParser(reader, "net");
        List<Metric> metricData = new ArrayList<>();

        try {
            String[] netStats = generateStatsArray("netStats");

            FileParser.StatParser statParser = new FileParser.StatParser(netStats, Commands.SPACE_COLON_REGEX) {
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
            logger.debug("Exception fetching net metricStats", e);
        }

        return metricData;
    }

    public List<Metric> getDiskUsage() {
        logger.debug("Fetching diskusage metricStats");
        BufferedReader reader;
        Map<String, Object> stats = new HashMap<String, Object>();
        try {
            Process process = Runtime.getRuntime().exec(Commands.DISK_USAGE_CMD);
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));


            FileParser parser = new FileParser(reader, "disk usage");

            String[] diskUsageStats = generateStatsArray("diskUsageStats");

            FileParser.StatParser statParser = new FileParser.StatParser(diskUsageStats, Commands.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return !line.startsWith("Filesystem") && !line.startsWith("none");
                }
            };
            parser.addParser(statParser);
            stats = parser.getStats();

        } catch (IOException e) {
            logger.error("Failed to run '" + Commands.DISK_USAGE_CMD + "' for disk usage");
        } catch (Exception e) {
            logger.error("Exception occurred collecting disk usage metrics", e);
        }

        return generateMetrics(stats, "diskUsageStats");
    }

    public List<Metric> getFileStats() {
        logger.debug("Fetching file metricStats");
        BufferedReader fhReader = getStream(Commands.FILE_NR_STAT_PATH);
        BufferedReader inodeReader = getStream(Commands.INODE_NR_STAT_PATH);
        BufferedReader dentriesReader = getStream(Commands.DENTRIES_STAT_PATH);

        List<Metric> statsMetrics = new ArrayList<Metric>();

        try {
            logger.debug("Fetching File NR metricStats");
            String[] fileNRStats = generateStatsArray("fileNRStats");
            FileParser parser = new FileParser(fhReader, "file handler");
            FileParser.StatParser statParser = new FileParser.StatParser(fileNRStats, Commands.SPACE_REGEX){};
            parser.addParser(statParser);
            List<Metric> metrics = generateMetrics(parser.getStats(), "fileNRStats");
            if (metrics != null) {
                statsMetrics.addAll(metrics);
            }

            logger.debug("Fetching iNode NR Stats");
            String[] inodeNRStats = generateStatsArray("inodeNRStats");

            parser = new FileParser(inodeReader, "inode");
            statParser = new FileParser.StatParser(inodeNRStats, Commands.SPACE_REGEX) {
            };
            parser.addParser(statParser);

            metrics = generateMetrics(parser.getStats(), "iNodeNRStats");
            if (metrics != null) {
                statsMetrics.addAll(metrics);
            }

            logger.debug("Fetching dentries Stats");
            String[] dentriesStats = generateStatsArray("dentriesStats");

            parser = new FileParser(dentriesReader, "dcache");
            statParser = new FileParser.StatParser(dentriesStats, Commands.SPACE_REGEX) {};
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
        BufferedReader reader = getStream(Commands.LOADAVG_STAT_PATH);
        List<Metric> metricData = new ArrayList<>();
        try {
            String[] loadAvgStats = generateStatsArray("loadAvgStats");

            FileParser parser = new FileParser(reader, "load average");
            FileParser.StatParser statParser = new FileParser.StatParser(loadAvgStats, Commands.SPACE_REGEX) {
            };
            parser.addParser(statParser);

            metricData.addAll(generateMetrics(parser.getStats(), "diskStats"));
        }catch(Exception e){
        logger.debug("Exception fetching load metricStats", e);
        }
        return metricData;
    }

    public List<Metric> getMemStats() {
        logger.debug("Fetching Memory Stats");
        BufferedReader reader = getStream(Commands.MEM_STAT_PATH);

        String[] memStats = generateStatsArray("memStats");

        Map<String, Object> statsMap = getRowStats(reader, Commands.SPACE_COLON_REGEX,
                Commands.MEM_FILE_STATS, memStats, "memory", 0, 1);

        try {
            Long total = Long.parseLong((String) statsMap.get("total"));
            Long free = Long.parseLong((String) statsMap.get("free"));
            Long used = total - free;
            Long usedP = 100 * used / total;
            Long swapTotal = Long.parseLong((String) statsMap.get("swap total"));
            Long swapUsed = swapTotal - Long.parseLong((String) statsMap.get("swap free"));
            Long realFree = free + Long.parseLong((String) statsMap.get("buffers"))
                    + Long.parseLong((String) statsMap.get("cached"));
            Long realFreeP = 100 * realFree / total;
            Long swapUsedP = 0L;
            if (swapTotal != 0) {
                swapUsedP = 100 * swapUsed / swapTotal;
            }

            statsMap.put("used", used.toString());
            statsMap.put("used %", usedP.toString());
            statsMap.put("swap used", swapUsed.toString());
            statsMap.put("swap used %", swapUsedP.toString());
            statsMap.put("real free", realFree.toString());
            statsMap.put("real free %", realFreeP.toString());
        } catch (NumberFormatException e) {
            logger.error("Failed to read some memory metricStats");
        } catch (ArithmeticException e) {
            logger.error("Error calculating additional memory metricStats");
        }
        return generateMetrics(statsMap,"memStats");

    }

    public List< Metric> getPageSwapStats() {
        BufferedReader reader = getStream(Commands.STAT_PATH);
        Map<String, Object> statsMap;
        List<Metric> metricStats = new ArrayList<>();

        try{
            String[] pageStats = generateStatsArray("pageStats");
            String[] swapStats = generateStatsArray("swapStats");
            String[] pageSwapStats = generateStatsArray("pageSwapStats");

            logger.debug("Fetching page metricStats");
            FileParser parser = new FileParser(reader, "page");
            FileParser.StatParser pageParser = new FileParser.StatParser(pageStats, Commands.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return line.startsWith("page");
                }
            };
            logger.debug("Fetching swap metricStats");
            parser.addParser(pageParser);
            FileParser.StatParser swapParser = new FileParser.StatParser(swapStats, Commands.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return line.startsWith("swap");
                }
            };
            parser.addParser(swapParser);

            statsMap = parser.getStats();
            if (statsMap == null) {  //page and swap are not in /proc/stat
                reader = getStream(Commands.VM_STAT_PATH);
                statsMap = getRowStats(reader, Commands.SPACE_REGEX, Commands.PAGE_SWAP_FILE_STATS, pageSwapStats, "page and swap", 0, 1);
            }
            metricStats.addAll(generateMetrics(statsMap,"pageStats"));
            metricStats.addAll(generateMetrics(statsMap,"swapStats"));
            metricStats.addAll(generateMetrics(statsMap,"pageSwapStats"));
        }catch(Exception e){
            logger.debug("Exception fetching swap metricStats", e);
        }
        return metricStats;
    }

    public List<Metric> getProcStats() {
        BufferedReader reader = getStream(Commands.STAT_PATH);

        List<Metric> metricStats = new ArrayList<>();

        try {
            String[] procStats = generateStatsArray("procStats");
            String[] procLoadAvgStats = generateStatsArray("procLoadAvgStats");

            Map<String, Object> statsMap = getRowStats(reader, Commands.SPACE_REGEX, Commands.PROC_FILE_STATS, procStats, "process", 0, 1);
            logger.debug("Fetching process metricStats");
            metricStats.addAll(generateMetrics(statsMap, "procStats"));

            reader = getStream(Commands.LOADAVG_STAT_PATH);

            FileParser parser = new FileParser(reader, "process");
            FileParser.StatParser statParser = new FileParser.StatParser(procLoadAvgStats, Commands.SPACE_REGEX + "|/") {
            };
            parser.addParser(statParser);
            Map<String, Object> map = parser.getStats();
            if (map != null) {
                metricStats.addAll(generateMetrics(map, "procLoadAvgStats"));
            }
        }catch(Exception e){
            logger.debug("Exception fetching process metricStats", e);
        }

        return metricStats;
    }

    public List<Metric> getSockStats() {
        BufferedReader reader = getStream(Commands.SOCK_STAT_PATH);
        logger.debug("Fetching socket metricStats");
        List<Metric> metricStats = new ArrayList<>();

        try {
            String[] sockUsedStats = generateStatsArray("sockUsedStats");
            String[] tcpInuseStats = generateStatsArray("tcpInuseStats");
            String[] udpInuseStats = generateStatsArray("udpInuseStats");
            String[] rawInuseStats = generateStatsArray("rawInuseStats");
            String[] ipfragStats = generateStatsArray("ipfragStats");

            FileParser parser = new FileParser(reader, "socket");
            FileParser.StatParser sockParser = new FileParser.StatParser(sockUsedStats, Commands.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return line.startsWith("sockets");
                }
            };
            parser.addParser(sockParser);
            metricStats.addAll(generateMetrics(parser.getStats(), "sockUsedStats"));

            reader = getStream(Commands.SOCK_STAT_PATH);
            parser = new FileParser(reader, "socket");
            FileParser.StatParser tcpParser = new FileParser.StatParser(tcpInuseStats, Commands.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return line.startsWith("TCP");
                }
            };
            parser.addParser(tcpParser);
            metricStats.addAll(generateMetrics(parser.getStats(), "tcpInuseStats"));

            reader = getStream(Commands.SOCK_STAT_PATH);
            parser = new FileParser(reader, "socket");
            FileParser.StatParser udpParser = new FileParser.StatParser(udpInuseStats, Commands.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return line.startsWith("UDP:");
                }
            };
            parser.addParser(udpParser);
            metricStats.addAll(generateMetrics(parser.getStats(), "udpInuseStats"));

            reader = getStream(Commands.SOCK_STAT_PATH);
            parser = new FileParser(reader, "socket");
            FileParser.StatParser rawParser = new FileParser.StatParser(rawInuseStats, Commands.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return line.startsWith("RAW");
                }
            };
            parser.addParser(rawParser);
            metricStats.addAll(generateMetrics(parser.getStats(), "rawInuseStats"));

            reader = getStream(Commands.SOCK_STAT_PATH);
            parser = new FileParser(reader, "socket");
            FileParser.StatParser ipfragParser = new FileParser.StatParser(ipfragStats, Commands.SPACE_REGEX) {
                @Override
                boolean isMatchType(String line) {
                    return line.startsWith("FRAG");
                }
            };
            parser.addParser(ipfragParser);
            metricStats.addAll(generateMetrics(parser.getStats(), "ipfragStats"));
        }catch(Exception e){
            logger.debug("Exception fetching socket metricStats", e);
        }
        return metricStats;
    }


    protected Map<String, Object> getRowStats(BufferedReader reader, String splitRegex,
                                            String[] fileKey, String[] statsKey,
                                            String description, int keyIndex, int valIndex) {
        Map<String, Object> statsMap = null;
        if (reader == null) {
            logger.error("Failed to read " + description + " metricStats as reader is null");
        } else {
            statsMap = new HashMap<String, Object>();

            Map<String, String> keyMap = new HashMap<String, String>();
            for (int i = 0; i < fileKey.length && i < statsKey.length; i++) {
                keyMap.put(fileKey[i], statsKey[i]);
            }
            try {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        String[] stats = line.split(splitRegex);
                        String name = keyMap.get(stats[keyIndex]);
                        if (name != null) {
                            statsMap.put(name, stats[valIndex]);
                        }
                    }
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                logger.error("Failed to read " + description + " metricStats: " + e.getStackTrace());
            }
        }
        logger.debug("Fetched rowstats for :" + description);
        return statsMap;
    }

    protected String[] generateStatsArray(String metricName){

        logger.debug("Generating Stats Array for metric: " + metricName);
        int arrayLength = 0;
        MetricConfig[] statArray = null;
        for(int i=0; i<metricStats.length; i++){
            if(metricStats[i].getName().equalsIgnoreCase(metricName)) {
                arrayLength = metricStats[i].getMetricConfig().length + 1;
                statArray = metricStats[i].getMetricConfig();
            }
        }
        String[] stats = new String[arrayLength];
        int index = 0;
        for(MetricConfig metricsEntry: statArray){
            stats[index++] = metricsEntry.getAttr();
        }
        return Arrays.stream(stats)
                .filter(s -> (s != null && s.length() > 0))
                .toArray(String[]::new);
    }

    protected List<Metric> generateMetrics(Map<String, Object> statsMap, String metricName){

        List<Metric> metricDataList = new ArrayList<Metric>();
        MetricConfig[] metricConfig = null;
        if(statsMap!=null) {
            for (Map.Entry<String, Object> statsEntry : statsMap.entrySet()) {

                logger.debug("Generating metrics for " + metricName);
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
            logger.error("No metricStats found for: " + metricName);
        }

        logger.debug("Returning " + metricDataList.size() + " metrics for " + metricName);

        return metricDataList;
    }

    protected List<Metric> generateMetrics(Map<String, String> statsMap, String metricName, String systemName){

        List<Metric> metricDataList = new ArrayList<Metric>();
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
            logger.error("No metricStats found for: " + metricName);
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
