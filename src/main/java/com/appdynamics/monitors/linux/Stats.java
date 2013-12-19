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

package com.appdynamics.monitors.linux;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class Stats {
    private Logger logger;

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

    private static final String DISK_USAGE_CMD = "df -kP 2>/dev/null";

    public static final String IDENTIFIER = "_ID_";

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
            {IDENTIFIER, "blocks (1024)", "used (KB)", "available (KB)", "capacity %"};
    private static String[] FILE_NR_STATS = {"fhalloc", "fhfree", "fhmax"};
    private static String[] INODE_NR_STATS = {"inalloc", "infree"};
    private static String[] DENTRIES_STATS = {"dentries", "unused", "agelimit", "wantpages"};
    private static String[] LOADAVG_STATS = {"load avg (1 min)", "load avg (10 min)", "load avg (15 min)"};
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



    public Stats(Logger logger) {
        this.logger = logger;
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

    public Map<String, Object> getCPUStats(){
        BufferedReader reader = getStream(STAT_PATH);
        FileParser parser = new FileParser(reader, "CPU", logger);
        FileParser.StatParser statParser = new FileParser.StatParser(CPU_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("cpu");
            }

            @Override
            boolean isBase(String[] stats) {
                return stats[0].equals("cpu");
            }
        };
        parser.addParser(statParser);

        return parser.getStats();
    }

    public Map<String, Object> getDiskStats(){
        BufferedReader reader = getStream(DISK_STAT_PATH);
        FileParser parser = new FileParser(reader, "disk", logger);
        FileParser.StatParser statParser = new FileParser.StatParser(DISK_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return true;
            }

            @Override
            boolean isBase(String[] stats) {
                return false;
            }
        };
        parser.addParser(statParser);

        return parser.getStats();
    }

    public Map<String, Object> getNetStats(){
        BufferedReader reader = getStream(NET_STAT_PATH);
        FileParser parser = new FileParser(reader, "net", logger);
        FileParser.StatParser statParser = new FileParser.StatParser(NET_STATS, SPACE_COLON_REGEX) {
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

        return parser.getStats();
    }

    public Map<String, Object> getDiskUsage(){
        BufferedReader reader;
        try {
            Process process = Runtime.getRuntime().exec(DISK_USAGE_CMD);
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        } catch (IOException e) {
            logger.error("Failed to run '"+DISK_USAGE_CMD+"' for disk usage");
            return null;
        } catch (InterruptedException e) {
            logger.error("Failed to run '"+DISK_USAGE_CMD+"' for disk usage");
            return null;
        }

        FileParser parser = new FileParser(reader, "disk usage", logger);
        FileParser.StatParser statParser = new FileParser.StatParser(DISK_USAGE_STATS, SPACE_REGEX) {
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

        return parser.getStats();
    }

    public Map<String, Object> getFileStats(){
        BufferedReader fhReader = getStream(FILE_NR_STAT_PATH);
        BufferedReader inodeReader = getStream(INODE_NR_STAT_PATH);
        BufferedReader dentriesReader = getStream(DENTRIES_STAT_PATH);

        Map<String, Object> statsMap = new HashMap<String, Object>();

        FileParser parser = new FileParser(fhReader, "file handler", logger);
        FileParser.StatParser statParser = new FileParser.StatParser(FILE_NR_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return true;
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(statParser);
        Map<String, Object> map = parser.getStats();
        if (map != null){
            statsMap.putAll(map);
        }

        parser = new FileParser(inodeReader, "inode", logger);
        statParser = new FileParser.StatParser(INODE_NR_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return true;
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(statParser);
        map = parser.getStats();
        if (map != null){
            statsMap.putAll(map);
        }

        parser = new FileParser(dentriesReader, "dcache", logger);
        statParser = new FileParser.StatParser(DENTRIES_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return true;
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(statParser);
        map = parser.getStats();
        if (map != null){
            statsMap.putAll(map);
        }

        return statsMap;
    }

    public Map<String, Object> getLoadStats(){
        BufferedReader reader = getStream(LOADAVG_STAT_PATH);
        FileParser parser = new FileParser(reader, "load average", logger);
        FileParser.StatParser statParser = new FileParser.StatParser(LOADAVG_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return true;
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(statParser);

        return parser.getStats();
    }

    public Map<String, Object> getMemStats(){
        BufferedReader reader = getStream(MEM_STAT_PATH);

        Map<String, Object> statsMap = getRowStats(reader, SPACE_COLON_REGEX,
                MEM_FILE_STATS, MEM_STATS, "memory", 0, 1);

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
            if (swapTotal != 0){
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

        return statsMap;

    }

    public Map<String, Object> getPageSwapStats(){
        BufferedReader reader = getStream(STAT_PATH);
        Map<String, Object> statsMap;

        FileParser parser = new FileParser(reader, "page", logger);
        FileParser.StatParser pageParser = new FileParser.StatParser(PAGE_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("page");
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(pageParser);
        FileParser.StatParser swapParser = new FileParser.StatParser(SWAP_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("swap");
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(swapParser);

        statsMap = parser.getStats();
        if (statsMap == null){  //page and swap are not in /proc/stat
            reader = getStream(VM_STAT_PATH);
            statsMap = getRowStats(reader, SPACE_REGEX, PAGE_SWAP_FILE_STATS, PAGE_SWAP_STATS, "page and swap", 0, 1);
        }
        return statsMap;
    }

    public Map<String, Object> getProcStats(){
        BufferedReader reader = getStream(STAT_PATH);
        Map<String, Object> statsMap = getRowStats(reader, SPACE_REGEX, PROC_FILE_STATS, PROC_STATS, "process", 0, 1);

        reader = getStream(LOADAVG_STAT_PATH);

        FileParser parser = new FileParser(reader, "process", logger);
        FileParser.StatParser statParser = new FileParser.StatParser(PROC_LOADAVG_STATS, SPACE_REGEX + "|/") {
            @Override
            boolean isMatchType(String line) {
                return true;
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(statParser);
        Map<String, Object> map = parser.getStats();
        if (map != null){
            statsMap.putAll(map);
        }

        return statsMap;
    }

    public Map<String, Object> getSockStats(){
        BufferedReader reader = getStream(SOCK_STAT_PATH);
        FileParser parser = new FileParser(reader, "socket", logger);
        FileParser.StatParser sockParser = new FileParser.StatParser(SOCK_USED_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("sockets");
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(sockParser);
        FileParser.StatParser tcpParser = new FileParser.StatParser(TCP_INUSE_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("TCP");
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(tcpParser);
        FileParser.StatParser udpParser = new FileParser.StatParser(UDP_INUSE_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("UDP:");
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(udpParser);
        FileParser.StatParser rawParser = new FileParser.StatParser(RAW_INUSE_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("RAW");
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(rawParser);
        FileParser.StatParser ipfragParser = new FileParser.StatParser(IPFRAG_STATS, SPACE_REGEX) {
            @Override
            boolean isMatchType(String line) {
                return line.startsWith("FRAG");
            }

            @Override
            boolean isBase(String[] stats) {
                return true;
            }
        };
        parser.addParser(ipfragParser);

        return parser.getStats();
    }


    private Map<String, Object> getRowStats(BufferedReader reader, String splitRegex,
                                           String[] fileKey, String[] statsKey,
                                           String description, int keyIndex, int valIndex){
        Map<String, Object> statsMap = null;
        if (reader == null) {
            logger.error("Failed to read " + description + " stats");
        } else {
            statsMap = new HashMap<String, Object>();

            Map<String, String> keyMap = new HashMap<String, String>();
            for (int i = 0; i<fileKey.length && i<statsKey.length; i++){
                keyMap.put(fileKey[i],statsKey[i]);
            }

            try {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        String[] stats = line.split(splitRegex);
                        String name = keyMap.get(stats[keyIndex]);
                        if (name != null){
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
}
