package com.appdynamics.monitors.linux;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class Parser {
    private Logger logger;

    private static final String STAT_PATH = "/proc/stat";
    private static final String NET_STAT_PATH = "/proc/net/dev";
    private static final String DISK_STAT_PATH = "/proc/diskstats";
    private static final String PARTITION_STAT_PATH = "/proc/partitions";
    private static final String MEM_STAT_PATH = "/proc/meminfo";


    private static final String SPACE_REGEX = "[\t ]+";
    private static final String COLON_REGEX = ":+";

    private static String[] CPU_STATS =
            {"cpu", "user", "nice", "system", "idle", "iowait", "irq", "softirq", "steal", "guest", "guest_nice"};
    private static String[] PAGE_STATS = {"page", "in", "out"};
    private static String[] SWAP_STATS = {"swap", "in", "out"};
    private static String[] INTERRUPT_STATS = {"intr", "total"};
    private static String[] NET_STATS =
            {"", "receive bytes", "receive packets", "receive errs", "receive drop", "receive fifo", "receive frame",
                    "receive compressed", "receive multicast", "transmit bytes", "transmit packets", "transmit errs",
                    "transmit drop", "transmit fifo", "transmit colls", "transmit carrier", "transmit compressed"};


    public Parser(Logger logger) {
        this.logger = logger;
    }


    public BufferedReader getStream(String filePath) {
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
        if (reader == null) {
            logger.error("Failed to read CPU stats");
            return null;
        }
        Map<String, Object> statsMap = new HashMap<String, Object>();

        try {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith(CPU_STATS[0])) {
                        String[] stats = line.split(SPACE_REGEX);
                        Map<String, String> map = getStatMap(CPU_STATS, stats);
                        if (stats[0].equals(CPU_STATS[0])) {
                            statsMap.putAll(map);
                        } else {
                            statsMap.put(stats[0],map);
                        }
                    } else {
                        break;  //assume cpu stats are at the beginning of the file and no breaks in between
                    }
//                    else if (line.startsWith(PAGE_STATS[0])) {
//                        Map<String, String> map = getStatMap(PAGE_STATS, line.split(SPACE_REGEX));
//                        statsMap.put(PAGE_STATS[0],map);
//                    } else if (line.startsWith(SWAP_STATS[0])) {
//                        Map<String, String> map = getStatMap(SWAP_STATS, line.split(SPACE_REGEX));
//                        statsMap.put(SWAP_STATS[0],map);
//                    } else if (line.startsWith(INTERRUPT_STATS[0])) {
//                        Map<String, String> map = getStatMap(INTERRUPT_STATS, line.split(SPACE_REGEX));
//                        statsMap.put(INTERRUPT_STATS[0],map);
//                    }
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            logger.error("Failed to read CPU stats");
        }

        return statsMap;
    }

    public Map<String, Object> getNetStats(){
        BufferedReader reader = getStream(NET_STAT_PATH);
        if (reader == null) {
            logger.error("Failed to read net stats");
            return null;
        }
        Map<String, Object> statsMap = new HashMap<String, Object>();

        try {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.contains("|")) {  //ignore name rows
                        String[] stats = line.split(SPACE_REGEX + "|" + COLON_REGEX);
                        Map<String, String> map = getStatMap(NET_STATS, stats);
                        if (stats[0].equals(NET_STATS[0])) {
                            statsMap.putAll(map);
                        } else {
                            statsMap.put(stats[0],map);
                        }
                    } else {
                        break;  //assume cpu stats are at the beginning of the file and no breaks in between
                    }
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            logger.error("Failed to read CPU stats");
        }
    }

    public Map<String, Object> getDiskStats(){
        BufferedReader reader = getStream(DISK_STAT_PATH);
        if (reader == null) {
            logger.error("Failed to read disk stats");
            return null;
        }
        Map<String, Object> statsMap = new HashMap<String, Object>();
    }

    private Map<String, String> getStatMap(String[] keys, String[] vals) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i=1; i<vals.length; i++){
            map.put(keys[i],vals[i]);
        }
        return map;
    }
}
