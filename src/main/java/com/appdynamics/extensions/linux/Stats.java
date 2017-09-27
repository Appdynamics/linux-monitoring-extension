/**
 * Copyright 2013 AppDynamics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.linux.config.Configuration;
import com.appdynamics.extensions.linux.config.MountedNFS;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import javafx.beans.property.MapProperty;
import org.apache.log4j.Logger;
import sun.security.krb5.Config;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Stats {
    public static final String IDENTIFIER = "_ID_";
    private static final String STAT_PATH = "/proc/stat";
    private static final String NET_STAT_PATH = "/proc/net/dev";
    private static final String DISK_STAT_PATH = "/proc/diskstats";
    private static final String MEM_STAT_PATH = "/proc/meminfo";
    private static final String FILE_NR_STAT_PATH = "/proc/sys/fs/file-nr";
    private static final String INODE_NR_STAT_PATH = "/proc/sys/fs/inode-nr";
    private static final String DENTRIES_STAT_PATH = "/proc/sys/fs/dentry-state";
    private static final String LOADAVG_STAT_PATH = "/proc/loadavg";
    private static final String VM_STAT_PATH = "/proc/vmstat";
    private static final String SOCK_STAT_PATH = "/proc/net/sockstat";

    private static final String[] DISK_USAGE_CMD = {"bash", "-c", "exec df -mP 2>/dev/null"};
    private static final String SPACE_REGEX = "[\t ]+";
    private static final String SPACE_COLON_REGEX = "[\t :]+";

    private static String[] CPU_STATS =
            {IDENTIFIER, "user", "nice", "system", "idle", "iowait", "irq", "softirq", "steal", "guest", "guest_nice"};
    private static String[] PAGE_STATS = {"page", "page in", "page out"};
    private static String[] SWAP_STATS = {"swap", "swap page in", "swap page out"};
    private static String[] NET_STATS =
            {IDENTIFIER, "receive bytes", "receive packets", "receive errs", "receive drop", "receive fifo",
                    "receive frame", "receive compressed", "receive multicast", "transmit bytes", "transmit packets",
                    "transmit errs", "transmit drop", "transmit fifo", "transmit colls", "transmit carrier",
                    "transmit compressed"};
    private static String[] DISK_STATS =
            {"major", "minor", IDENTIFIER, "reads completed successfully", "reads merged", "sectors read",
                    "time spent reading (ms)", "writes completed", "writes merged", "sectors written",
                    "time spent writing (ms)", "I/Os currently in progress", "time spent doing I/Os (ms)",
                    "weighted time spent doing I/Os (ms)"};
    private static String[] DISK_USAGE_STATS =
            {IDENTIFIER, "size (MB)", "used (MB)", "available (MB)", "use %"};
    private static String[] FILE_NR_STATS = {"fhalloc", "fhfree", "fhmax"};
    private static String[] INODE_NR_STATS = {"inalloc", "infree"};
    private static String[] DENTRIES_STATS = {"dentries", "unused", "agelimit", "wantpages"};
    private static String[] LOADAVG_STATS = {"load avg (1 min)", "load avg (5 min)", "load avg (15 min)"};
    private static String[] MEM_FILE_STATS =
            {"MemTotal", "MemFree", "Buffers", "Cached", "SwapCached", "Active", "Inactive", "SwapTotal", "SwapFree",
                    "Dirty", "Writeback", "Mapped", "Slab", "CommitLimit", "Committed_AS"};
    private static String[] MEM_STATS = {"total", "free", "buffers", "cached", "swap cached", "active", "inactive",
            "swap total", "swap free", "dirty", "writeback", "mapped", "slab", "commit limit", "committed_as"};
    private static String[] PAGE_SWAP_FILE_STATS = {"pgpgin", "pgpgout", "pswpin", "pswpout", "pgfault", "pgmajfault"};
    private static String[] PAGE_SWAP_STATS = {"page in", "page out", "swap page in", "swap page out", "page fault",
            "page major fault"};
    private static String[] PROC_FILE_STATS = {"processes", "procs_running", "procs_blocked"};
    private static String[] PROC_STATS = {"processes", "running", "blocked"};
    private static String[] PROC_LOADAVG_STATS = {IDENTIFIER, IDENTIFIER, IDENTIFIER, "runqueue", "count"};
    private static String[] SOCK_USED_STATS = {IDENTIFIER, IDENTIFIER, "used"};
    private static String[] TCP_INUSE_STATS = {IDENTIFIER, IDENTIFIER, "tcp"};
    private static String[] UDP_INUSE_STATS = {IDENTIFIER, IDENTIFIER, "udp"};
    private static String[] RAW_INUSE_STATS = {IDENTIFIER, IDENTIFIER, "raw"};
    private static String[] IPFRAG_STATS = {IDENTIFIER, IDENTIFIER, "ipfrag"};
    private static Logger logger;
    protected static Map<String, List<Map<String, String>>> allMetricsFromConfig = new HashMap<String, List<Map<String, String>>>();


    public Stats(Logger logger, List<Map<String, List<Map<String, String>>>> metrics) {
        this.logger = logger;
        populateMetricsMap(metrics);
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

    public List<MetricData> getCPUStats() {
        BufferedReader reader = getStream(STAT_PATH);
        FileParser parser = new FileParser(reader, "CPU", logger, "CPU cores (logical)");

        String[] CPUStats = generateStatsArray("cpuStats");

        FileParser.StatParser statParser = new FileParser.StatParser(CPUStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("cpu");
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

    public List<MetricData> getDiskStats() {
        BufferedReader reader = getStream(DISK_STAT_PATH);
        FileParser parser = new FileParser(reader, "disk", logger, null);

        String[] diskStats = generateStatsArray("diskStats");

        FileParser.StatParser statParser = new FileParser.StatParser(diskStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.contains("sd") | line.contains("hd");
            }

            @Override
            boolean isBase(String[] stats) {
                return false;
            }

            @Override
            boolean isCountRequired() {
                return false;
            }
        };
        parser.addParser(statParser);

        return generateStatsMap(parser.getStats(), "diskStats");
    }

    public List<MetricData> getNetStats() {
        BufferedReader reader = getStream(NET_STAT_PATH);
        FileParser parser = new FileParser(reader, "net", logger, null);

        String[] netStats = generateStatsArray("netStats");

        FileParser.StatParser statParser = new FileParser.StatParser(netStats, SPACE_COLON_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return !line.contains("|");
            }

            @Override
            boolean isBase(String[] stats) {
                return false;
            }

            @Override
            boolean isCountRequired() {
                return false;
            }
        };
        parser.addParser(statParser);


        return generateStatsMap(parser.getStats(), "netStats");
    }

    public List<MetricData> getDiskUsage() {
        BufferedReader reader;
        try {
            Process process = Runtime.getRuntime().exec(DISK_USAGE_CMD);
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        } catch (IOException e) {
            logger.error("Failed to run '" + DISK_USAGE_CMD + "' for disk usage");
            return null;
        } catch (InterruptedException e) {
            logger.error("Failed to run '" + DISK_USAGE_CMD + "' for disk usage");
            return null;
        }

        FileParser parser = new FileParser(reader, "disk usage", logger, null);

        String[] diskUsageStats = generateStatsArray("diskUsageStats");

        FileParser.StatParser statParser = new FileParser.StatParser(diskUsageStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return !line.startsWith("Filesystem") && !line.startsWith("none");
            }

            @Override
            boolean isBase(String[] stats) {
                return false;
            }

            @Override
            boolean isCountRequired() {
                return false;
            }
        };
        parser.addParser(statParser);

        // Check, if use % metrics is already being created, then remove this code piece
        Map<String, Object> stats = parser.getStats();
        for (Map.Entry<String, Object> diskStats : stats.entrySet()) {
            Map<String, Object> diskStat = (Map) diskStats.getValue();
            String capacityValue = (String) diskStat.get("use %");
            diskStat.put("use %", capacityValue.replace("%", "").trim());
        }

        return generateStatsMap(parser.getStats(), "diskUsageStats");
    }

    public List<MetricData> getFileStats() {
        BufferedReader fhReader = getStream(FILE_NR_STAT_PATH);
        BufferedReader inodeReader = getStream(INODE_NR_STAT_PATH);
        BufferedReader dentriesReader = getStream(DENTRIES_STAT_PATH);

        List<MetricData> statsMetrics = new ArrayList<MetricData>();

        String[] fileNRStats = generateStatsArray("fileNRStats");
        FileParser parser = new FileParser(fhReader, "file handler", logger, null);
        FileParser.StatParser statParser = new FileParser.StatParser(fileNRStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return true;
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
        List< MetricData> metrics = generateStatsMap(parser.getStats(),"fileNRStats");
        if (metrics != null) {
            statsMetrics.addAll(metrics);
        }

        String[] inodeNRStats = generateStatsArray("inodeNRStats");

        parser = new FileParser(inodeReader, "inode", logger, null);
        statParser = new FileParser.StatParser(inodeNRStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return true;
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
/*
        metrics = generateStatsMap(parser.getStats(),"iNodeNRStats");
        if (metrics != null) {
            statsMap.addAll(metrics);
        }
*/

        String[] dentriesStats = generateStatsArray("dentriesStats");

        parser = new FileParser(dentriesReader, "dcache", logger, null);
        statParser = new FileParser.StatParser(dentriesStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return true;
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
        metrics = generateStatsMap(parser.getStats(),"dentriesStats");
        if (metrics != null) {
            statsMetrics.addAll(metrics);
        }

        return statsMetrics;
    }

    public List<MetricData> getLoadStats() {
        BufferedReader reader = getStream(LOADAVG_STAT_PATH);

        String[] loadAvgStats = generateStatsArray("loadAvgStats");

        FileParser parser = new FileParser(reader, "load average", logger, null);
        FileParser.StatParser statParser = new FileParser.StatParser(loadAvgStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return true;
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

        return generateStatsMap(parser.getStats(), "loadAvgStats");
    }

    public List<MetricData> getMemStats() {
        BufferedReader reader = getStream(MEM_STAT_PATH);

        String[] memStats = generateStatsArray("memStats");

        Map<String, Object> statsMap = getRowStats(reader, SPACE_COLON_REGEX,
                MEM_FILE_STATS, memStats, "memory", 0, 1);

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
        BufferedReader reader = getStream(STAT_PATH);
        Map<String, Object> statsMap;

        String[] pageStats = generateStatsArray("pageStats");
        String[] swapStats = generateStatsArray("swapStats");
        String[] pageSwapStats = generateStatsArray("pageSwapStats");


        FileParser parser = new FileParser(reader, "page", logger, null);
        FileParser.StatParser pageParser = new FileParser.StatParser(pageStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("page");
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
        parser.addParser(pageParser);
        FileParser.StatParser swapParser = new FileParser.StatParser(swapStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("swap");
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
        parser.addParser(swapParser);

        statsMap = parser.getStats();
        if (statsMap == null) {  //page and swap are not in /proc/stat
            reader = getStream(VM_STAT_PATH);
            statsMap = getRowStats(reader, SPACE_REGEX, PAGE_SWAP_FILE_STATS, pageSwapStats, "page and swap", 0, 1);
        }

        List<MetricData> metricStats = new ArrayList<MetricData>();

        metricStats.addAll(generateStatsMap(statsMap,"pageStats"));
        metricStats.addAll(generateStatsMap(statsMap,"swapStats"));
        metricStats.addAll(generateStatsMap(statsMap,"pageSwapFileStats"));
        metricStats.addAll(generateStatsMap(statsMap,"pageSwapStats"));

        return metricStats;
    }

    public List<MetricData> getProcStats() {
        BufferedReader reader = getStream(STAT_PATH);

        List<MetricData> metricStats = new ArrayList<MetricData>();

       String[] procStats = generateStatsArray("procStats");
        String[] procLoadAvgStats = generateStatsArray("procLoadAvgStats");

        Map<String, Object> statsMap = getRowStats(reader, SPACE_REGEX, PROC_FILE_STATS, procStats, "process", 0, 1);

        metricStats.addAll(generateStatsMap(statsMap,"procStats"));

        reader = getStream(LOADAVG_STAT_PATH);

        FileParser parser = new FileParser(reader, "process", logger, null);
        FileParser.StatParser statParser = new FileParser.StatParser(procLoadAvgStats, SPACE_REGEX + "|/") {
            @Override
            boolean isMatchType(String line) {
                return true;
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
        Map<String, Object> map = parser.getStats();
        if (map != null) {
            metricStats.addAll(generateStatsMap(map,"procLoadAvgStats"));
        }

        return metricStats;
    }

    public List<MetricData> getSockStats() {
        BufferedReader reader = getStream(SOCK_STAT_PATH);

        List<MetricData> metricStats = new ArrayList<MetricData>();

        String[] sockUsedStats = generateStatsArray("sockUsedStats");
        String[] tcpInuseStats = generateStatsArray("tcpInuseStats");
        String[] udpInuseStats = generateStatsArray("udpInuseStats");
        String[] rawInuseStats = generateStatsArray("rawInuseStats");
        String[] ipfragStats = generateStatsArray("ipfragStats");
        FileParser parser = new FileParser(reader, "socket", logger, null);
        FileParser.StatParser sockParser = new FileParser.StatParser(sockUsedStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("sockets");
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
        parser.addParser(sockParser);
        metricStats.addAll(generateStatsMap(parser.getStats(),"sockUsedStats"));

        FileParser.StatParser tcpParser = new FileParser.StatParser(tcpInuseStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("TCP");
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
        parser.addParser(tcpParser);
        metricStats.addAll(generateStatsMap(parser.getStats(),"tcpInuseStats"));

        FileParser.StatParser udpParser = new FileParser.StatParser(udpInuseStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("UDP:");
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
        parser.addParser(udpParser);
        metricStats.addAll(generateStatsMap(parser.getStats(),"udpInuseStats"));

        FileParser.StatParser rawParser = new FileParser.StatParser(rawInuseStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("RAW");
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
        parser.addParser(rawParser);
        metricStats.addAll(generateStatsMap(parser.getStats(),"rawInuseStats"));

        FileParser.StatParser ipfragParser = new FileParser.StatParser(ipfragStats, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("FRAG");
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
        parser.addParser(ipfragParser);
        metricStats.addAll(generateStatsMap(parser.getStats(),"ipfragStats"));

        return metricStats;
    }


    protected static Map<String, Object> getRowStats(BufferedReader reader, String splitRegex,
                                            String[] fileKey, String[] statsKey,
                                            String description, int keyIndex, int valIndex) {
        Map<String, Object> statsMap = null;
        if (reader == null) {
            logger.error("Failed to read " + description + " stats");
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
                logger.error("Failed to read " + description + " stats");
            }
        }

        return statsMap;
    }

    public List<MetricData> getMountStatus(MountedNFS[] mountedNFS) {

        Map<String, Object> mountStatsMap = Maps.newHashMap();
        NFSMountStatusProcessor statusProcessor = new NFSMountStatusProcessor();
        for (MountedNFS fileSystem : mountedNFS) {
            String status = statusProcessor.execute(fileSystem.getFileSystem());

            if (!Strings.isNullOrEmpty(status)) {
                if (!Strings.isNullOrEmpty(fileSystem.getDisplayName())) {
                    mountStatsMap.put(fileSystem.getDisplayName(), status);
                } else {
                    mountStatsMap.put(fileSystem.getFileSystem(), status);
                }
            }
        }
        return generateStatsMap(mountStatsMap,"mountedNFSStatus");

    }

    public List<MetricData> getMountIOStats(MountedNFS[] mountedNFS) {

        List<MetricData> ioStats = new ArrayList<MetricData>();

        NFSMountStatusProcessor statusProcessor = new NFSMountStatusProcessor();
        for (MountedNFS fileSystem : mountedNFS) {
            Map<String,Object> statsMap = statusProcessor.getNFSMetrics(fileSystem);

            List<MetricData> metricStats = new ArrayList<MetricData>();

            for(Map.Entry statsEntry : statsMap.entrySet()) {

                for (Map<String, String> metrics : Stats.allMetricsFromConfig.get("nfsIOStats")) {

                    System.out.println("Metric name: " + metrics.get("name") + "StatsEntry key: " + statsEntry.getKey() );
                    if (metrics.get("name").equalsIgnoreCase(String.valueOf(statsEntry.getKey()))) {

                        MetricData metricData = new MetricData();
                        metricData.setStats(statsEntry.getValue());
                        metricData.setName(fileSystem.getDisplayName() + "|" + metrics.get("name"));
                        metricData.setCollectDelta(Boolean.valueOf(metrics.get("collectDelta")));
                        metricData.setMetricType(metrics.get("metricType"));

                        metricStats.add(metricData);
                    }
                }
            }

        }
        return ioStats;

    }

    private void populateMetricsMap(List<Map<String, List<Map<String, String>>>> metrics){
        for(Map<String, List<Map<String, String>>> metricsConfigEntry: metrics){
            allMetricsFromConfig.putAll(metricsConfigEntry);
        }
    }

    protected static String[] generateStatsArray(String metricName){

        String[] stats = new String[allMetricsFromConfig.get(metricName).size()+1];
        int index = 0;
        for(Map<String,String> metricsEntry: allMetricsFromConfig.get(metricName)){
            stats[index++] = metricsEntry.get("name");
        }
        return stats;
    }

    protected static List<MetricData> generateStatsMap(Map<String, Object> statsMap, String metricName){

        List<MetricData> metricStats = new ArrayList<MetricData>();
        if(statsMap!=null) {

            for (Map.Entry<String, Object> statsEntry : statsMap.entrySet()) {
                for (Map<String, String> metrics : allMetricsFromConfig.get(metricName)) {

                  if (metrics.get("name").equalsIgnoreCase(statsEntry.getKey()) ) {

                        MetricData metricData = new MetricData();
                        metricData.setStats(statsEntry.getValue());
                        metricData.setName(metrics.get("name"));
                        metricData.setCollectDelta(Boolean.valueOf(metrics.get("collectDelta")));
                        metricData.setMetricType(metrics.get("metricType"));

                        metricStats.add(metricData);
                    }

                  if(metricName.equalsIgnoreCase("mountedNFSStatus") ){

                        MetricData metricData = new MetricData();
                        metricData.setStats(statsEntry.getValue());
                        metricData.setName(statsEntry.getKey() + "|" + metrics.get("name"));
                        metricData.setCollectDelta(Boolean.valueOf(metrics.get("collectDelta")));
                        metricData.setMetricType(metrics.get("metricType"));

                        metricStats.add(metricData);
                  }
                }

            }
        }else{
            logger.error("No stats found for: " + metricName);
        }
        return metricStats;
    }
}
