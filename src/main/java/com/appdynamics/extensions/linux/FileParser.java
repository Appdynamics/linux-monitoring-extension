/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.linux.input.MetricConfig;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Pattern;


public class FileParser {
    private BufferedReader reader;
    private String description;
    private List<StatParser> parserList = new ArrayList<StatParser>();

    private Logger logger  = Logger.getLogger(FileParser.class);

    public FileParser(BufferedReader reader, String description) {
        this.reader = reader;
        this.description = description;
    }

    public void addParser(StatParser parser) {
        parserList.add(parser);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> statsMap = null;
        if (reader == null) {
            logger.error("Failed to read " + description + " metricStats as reader is null");
        } else {
            statsMap = new HashMap<>();
            Pattern pattern = Pattern.compile("^([0-9])");
            try {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        for (StatParser parser : parserList) {
                            if (parser.isMatchType(line)) {
                                List<String> stats = new LinkedList<>(Arrays.asList(line.split(parser.regex)));
                                String name = stats.get(0);

                                ListIterator<String> iter = stats.listIterator();
                                while(iter.hasNext()){
                                    if(!(pattern.matcher(iter.next()).find())){
                                        iter.remove();
                                    }
                                }
                                Map<String, String> map = new HashMap<>();
                                for(MetricConfig metricConfig: parser.filteredMetrics){
                                    map.put(metricConfig.getAttr(), stats.get(Integer.valueOf(metricConfig.getIndex())));
                                }

                                if (parser.isBase(line.split(parser.regex))) {
                                    statsMap.putAll(map);   //put in base dir
                                } else if (name != null) {
                                    statsMap.put(name, map); //put in subdir
                                } else {    //no sub dir name from metricStats, fall back to description
                                    statsMap.put(description, map);
                                }
                            }
                        }
                    }
                }catch (Exception e) {
                    logger.error("Failed to read " + description + " metricStats", e);
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                logger.error("Failed to read " + description + " metricStats IOException", e);
            }
            if (statsMap.size() == 0) {
                statsMap = null;
            }
        }
        return statsMap;
    }

    public abstract static class StatParser {
        private MetricConfig[] filteredMetrics;
        private String regex;

        public StatParser(MetricConfig[] filteredMetrics, String regex) {
            this.filteredMetrics = filteredMetrics;
            this.regex = regex;
        }

        /**
         * Condition for parser to parse current line
         *
         * @param line a line from <code>reader</code>
         * @return true if parser should parse <code>line</code>, false otherwise
         */
         boolean isMatchType(String line){
             return true;
         }

        /**
         * Whether current list of metricStats should be placed under base dir
         *
         * @param stats array of metricStats from a line in <code>reader</code>
         * @return true if the metricStats should be placed under base dir, false if they're to be put in a sub dir
         */
         boolean isBase(String[] stats){
             return true;
         }

    }
}
