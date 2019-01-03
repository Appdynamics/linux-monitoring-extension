/*
 * Copyright 2018. AppDynamics LLC and its `filiates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.base.Joiner;
import org.apache.log4j.Logger;

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

    protected static Map<String, List<Map<String, String>>> allMetricsFromConfig = new HashMap<String, List<Map<String, String>>>();

    private static Logger logger  = Logger.getLogger(Stats.class);

    private MetricWriteHelper metricWriteHelper;

    private String metricPrefix;

    private List<Map<String, List<Map<String, String>>>> configMetrics;

    private List<Metric> metrics = new ArrayList<Metric>();

    private List<Map<String, String>> metricReplacer;

    public Stats(List<Map<String, List<Map<String, String>>>> configMetrics, String metricPrefix, MetricWriteHelper metricWriteHelper, List<Map<String, String>> metricReplacer) {
        this.configMetrics = configMetrics;
        this.metricPrefix = metricPrefix;
        this.metricWriteHelper = metricWriteHelper;
        this.metricReplacer = metricReplacer;
        populateMetricsMap(configMetrics);
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

    public List<MetricData> getCPUStats(final List<String> cpuIncludes) {

        logger.debug("Fetching CPU stats");
        BufferedReader reader = getStream(Commands.STAT_PATH);
        FileParser parser = new FileParser(reader, "CPU", "CPU cores (logical)");

        String[] CPUStats = generateStatsArray("cpuStats");

        FileParser.StatParser statParser = new FileParser.StatParser(CPUStats, Commands.SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return  isFiltered(line, cpuIncludes);
            }

            @Override
            boolean isBase(String[] stats) {
                return stats[0].equals("cpu");
            }

            @Override
            boolean isCountRequired() {
                return true;
            }
        };
        parser.addParser(statParser);

        return generateStatsMap(parser.getStats(), "cpuStats");
    }

    public List<MetricData> getDiskStats(final List<String> diskIncludes) {
        BufferedReader reader = getStream(Commands.DISK_STAT_PATH);
        logger.debug("Fetching disk stats for " + diskIncludes);
        FileParser parser = new FileParser(reader, "disk", null);

        String[] diskStats = generateStatsArray("diskStats");

        FileParser.StatParser statParser = new FileParser.StatParser(diskStats, Commands.SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return isFiltered(line, diskIncludes);
            }
        };
        parser.addParser(statParser);

        return generateStatsMap(parser.getStats(), "diskStats");
    }

    public List<MetricData> getNetStats() {
        logger.debug("Fetching net stats");
        BufferedReader reader = getStream(Commands.NET_STAT_PATH);
        FileParser parser = new FileParser(reader, "net", null);

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


        return generateStatsMap(parser.getStats(), "netStats");
    }

    public List<MetricData> getDiskUsage() {
        logger.debug("Fetching diskusage stats");
        BufferedReader reader;
        Map<String, Object> stats = new HashMap<String, Object>();
        try {
            Process process = Runtime.getRuntime().exec(Commands.DISK_USAGE_CMD);
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));


            FileParser parser = new FileParser(reader, "disk usage", null);

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

        return generateStatsMap(stats, "diskUsageStats");
    }

    public List<MetricData> getFileStats() {
        logger.debug("Fetching file stats");
        BufferedReader fhReader = getStream(Commands.FILE_NR_STAT_PATH);
        BufferedReader inodeReader = getStream(Commands.INODE_NR_STAT_PATH);
        BufferedReader dentriesReader = getStream(Commands.DENTRIES_STAT_PATH);

        List<MetricData> statsMetrics = new ArrayList<MetricData>();

        try {
            logger.debug("Fetching File NR stats");
            String[] fileNRStats = generateStatsArray("fileNRStats");
            FileParser parser = new FileParser(fhReader, "file handler", null);
            FileParser.StatParser statParser = new FileParser.StatParser(fileNRStats, Commands.SPACE_REGEX){};
            parser.addParser(statParser);
            List<MetricData> metrics = generateStatsMap(parser.getStats(), "fileNRStats");
            if (metrics != null) {
                statsMetrics.addAll(metrics);
            }

            logger.debug("Fetching iNode NR Stats");
            String[] inodeNRStats = generateStatsArray("inodeNRStats");

            parser = new FileParser(inodeReader, "inode", null);
            statParser = new FileParser.StatParser(inodeNRStats, Commands.SPACE_REGEX) {
            };
            parser.addParser(statParser);

            metrics = generateStatsMap(parser.getStats(), "iNodeNRStats");
            if (metrics != null) {
                statsMetrics.addAll(metrics);
            }

            logger.debug("Fetching dentries Stats");
            String[] dentriesStats = generateStatsArray("dentriesStats");

            parser = new FileParser(dentriesReader, "dcache", null);
            statParser = new FileParser.StatParser(dentriesStats, Commands.SPACE_REGEX) {};
            parser.addParser(statParser);
            metrics = generateStatsMap(parser.getStats(), "dentriesStats");
            if (metrics != null) {
                statsMetrics.addAll(metrics);
            }
        }catch(Exception e) {
            logger.error("Exception collection metrics for file: ", e);
        }
        return statsMetrics;
    }

    public List<MetricData> getLoadStats() {
        logger.debug("Fetching load Stats");
        BufferedReader reader = getStream(Commands.LOADAVG_STAT_PATH);

        String[] loadAvgStats = generateStatsArray("loadAvgStats");

        FileParser parser = new FileParser(reader, "load average", null);
        FileParser.StatParser statParser = new FileParser.StatParser(loadAvgStats, Commands.SPACE_REGEX) {};
        parser.addParser(statParser);

        return generateStatsMap(parser.getStats(), "loadAvgStats");
    }

    public List<MetricData> getMemStats() {
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
            logger.error("Failed to read some memory stats");
        } catch (ArithmeticException e) {
            logger.error("Error calculating additional memory stats");
        }

        List<MetricData> metricStats = new ArrayList<MetricData>();

        metricStats.addAll(generateStatsMap(statsMap,"memStats"));

        return metricStats;

    }

    public List< MetricData> getPageSwapStats() {
        BufferedReader reader = getStream(Commands.STAT_PATH);
        Map<String, Object> statsMap;

        String[] pageStats = generateStatsArray("pageStats");
        String[] swapStats = generateStatsArray("swapStats");
        String[] pageSwapStats = generateStatsArray("pageSwapStats");

        logger.debug("Fetching page stats");
        FileParser parser = new FileParser(reader, "page", null);
        FileParser.StatParser pageParser = new FileParser.StatParser(pageStats, Commands.SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("page");
            }
        };
        logger.debug("Fetching swap stats");
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

        List<MetricData> metricStats = new ArrayList<MetricData>();

        metricStats.addAll(generateStatsMap(statsMap,"pageStats"));
        metricStats.addAll(generateStatsMap(statsMap,"swapStats"));
        metricStats.addAll(generateStatsMap(statsMap,"pageSwapFileStats"));
        metricStats.addAll(generateStatsMap(statsMap,"pageSwapStats"));

        return metricStats;
    }

    public List<MetricData> getProcStats() {
        BufferedReader reader = getStream(Commands.STAT_PATH);

        List<MetricData> metricStats = new ArrayList<MetricData>();

        String[] procStats = generateStatsArray("procStats");
        String[] procLoadAvgStats = generateStatsArray("procLoadAvgStats");

        Map<String, Object> statsMap = getRowStats(reader, Commands.SPACE_REGEX, Commands.PROC_FILE_STATS, procStats, "process", 0, 1);
        logger.debug("Fetching process stats");
        metricStats.addAll(generateStatsMap(statsMap,"procStats"));

        reader = getStream(Commands.LOADAVG_STAT_PATH);

        FileParser parser = new FileParser(reader, "process", null);
        FileParser.StatParser statParser = new FileParser.StatParser(procLoadAvgStats, Commands.SPACE_REGEX + "|/") {};
        parser.addParser(statParser);
        Map<String, Object> map = parser.getStats();
        if (map != null) {
            metricStats.addAll(generateStatsMap(map,"procLoadAvgStats"));
        }

        return metricStats;
    }

    public List<MetricData> getSockStats() {
        BufferedReader reader = getStream(Commands.SOCK_STAT_PATH);
        logger.debug("Fetching socket stats");
        List<MetricData> metricStats = new ArrayList<MetricData>();

        String[] sockUsedStats = generateStatsArray("sockUsedStats");
        String[] tcpInuseStats = generateStatsArray("tcpInuseStats");
        String[] udpInuseStats = generateStatsArray("udpInuseStats");
        String[] rawInuseStats = generateStatsArray("rawInuseStats");
        String[] ipfragStats = generateStatsArray("ipfragStats");

        FileParser parser = new FileParser(reader, "socket", null);
        FileParser.StatParser sockParser = new FileParser.StatParser(sockUsedStats, Commands.SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("sockets");
            }
        };
        parser.addParser(sockParser);
        metricStats.addAll(generateStatsMap(parser.getStats(),"sockUsedStats"));

        reader = getStream(Commands.SOCK_STAT_PATH);
        parser = new FileParser(reader, "socket", null);
        FileParser.StatParser tcpParser = new FileParser.StatParser(tcpInuseStats, Commands.SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("TCP");
            }
        };
        parser.addParser(tcpParser);
        metricStats.addAll(generateStatsMap(parser.getStats(),"tcpInuseStats"));

        reader = getStream(Commands.SOCK_STAT_PATH);
        parser = new FileParser(reader, "socket", null);
        FileParser.StatParser udpParser = new FileParser.StatParser(udpInuseStats, Commands.SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("UDP:");
            }
        };
        parser.addParser(udpParser);
        metricStats.addAll(generateStatsMap(parser.getStats(),"udpInuseStats"));

        reader = getStream(Commands.SOCK_STAT_PATH);
        parser = new FileParser(reader, "socket", null);
        FileParser.StatParser rawParser = new FileParser.StatParser(rawInuseStats, Commands.SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("RAW");
            }
        };
        parser.addParser(rawParser);
        metricStats.addAll(generateStatsMap(parser.getStats(),"rawInuseStats"));

        reader = getStream(Commands.SOCK_STAT_PATH);
        parser = new FileParser(reader, "socket", null);
        FileParser.StatParser ipfragParser = new FileParser.StatParser(ipfragStats, Commands.SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("FRAG");
            }
        };
        parser.addParser(ipfragParser);
        metricStats.addAll(generateStatsMap(parser.getStats(),"ipfragStats"));

        return metricStats;
    }


    protected static Map<String, Object> getRowStats(BufferedReader reader, String splitRegex,
                                            String[] fileKey, String[] statsKey,
                                            String description, int keyIndex, int valIndex) {
        Map<String, Object> statsMap = null;
        if (reader == null) {
            logger.error("Failed to read " + description + " stats as reader is null");
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
                logger.error("Failed to read " + description + " stats: " + e.getStackTrace());
            }
        }
        logger.debug("Fetched rowstats for :" + description);
        return statsMap;
    }

    private void populateMetricsMap(List<Map<String, List<Map<String, String>>>> metrics){
        for(Map<String, List<Map<String, String>>> metricsConfigEntry: metrics){
            allMetricsFromConfig.putAll(metricsConfigEntry);
        }
    }

    protected static String[] generateStatsArray(String metricName){

        logger.debug("Generating Stats Array for metric: " + metricName);

        String[] stats = new String[allMetricsFromConfig.get(metricName).size()+1];
        int index = 0;
        for(Map<String,String> metricsEntry: allMetricsFromConfig.get(metricName)){
            stats[index++] = metricsEntry.get("name");
        }
        return Arrays.stream(stats)
                .filter(s -> (s != null && s.length() > 0))
                .toArray(String[]::new);
    }

    protected static List<MetricData> generateStatsMap(Map<String, Object> statsMap, String metricName){

        List<MetricData> metricStats = new ArrayList<MetricData>();
        if(statsMap!=null) {
            for (Map.Entry<String, Object> statsEntry : statsMap.entrySet()) {
                List<Map<String, String>> metricConfig = allMetricsFromConfig.get(metricName);
                if(metricConfig!=null) {

                    for (Map<String, String> metrics : metricConfig) {

                         if (metrics != null && metrics.get("name").equalsIgnoreCase(statsEntry.getKey())) {
                            MetricData metricData = new MetricData();
                            metricData.setStats(statsEntry.getValue());
                            metricData.setName(metrics.get("name"));
                            metricData.setCollectDelta(Boolean.valueOf(metrics.get("collectDelta")));
                            metricData.setMultiplier(metrics.get("multiplier"));
                            metricData.setMetricType(metrics.get("metricType"));
                            metricData.constructProperties();

                            metricStats.add(metricData);
                        }
                    }
                }
            }
        }else{
            logger.error("No stats found for: " + metricName);
        }

        logger.debug("Returning " + metricStats.size() + " metrics for " + metricName);
        return metricStats;
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

    public void printNestedMap(Map<String, Object> map, String metricPath) {

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();

            List<MetricData> val = (ArrayList)entry.getValue();
            if(val != null) {
                for (MetricData metricData : val) {
                    try {
                        if ("time spent doing I/Os (ms)".equals(key)) {
                            metrics.add(new Metric(metricData.getName(), replaceCharacter(metricData.getStats().toString()), metricPath + "|" + "Avg I/O Utilization %", metricData.getPropertiesMap()));
                        } else {
                            metrics.add(new Metric(metricData.getName(), replaceCharacter(metricData.getStats().toString()), metricPath + "|" + key + "|" + metricData.getName(), metricData.getPropertiesMap()));
                        }
                    } catch (Exception e) {
                        logger.error("Exception printing metric: " + metricPath + key + "|" + metricData.getName() + " with value: " + metricData.getStats().toString(), e);
                    }
                }
            }
        }
        logger.debug("Number of metrics reporting: " + metrics.size());
        if (metrics != null && metrics.size() > 0) {
            metricWriteHelper.transformAndPrintMetrics(metrics);
        }
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
