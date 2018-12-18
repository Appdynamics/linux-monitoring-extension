/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.linux;

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
    private String countMetric;
    private List<StatParser> parserList = new ArrayList<StatParser>();

    private Logger logger  = Logger.getLogger(FileParser.class);

    public FileParser(BufferedReader reader, String description, String countMetric) {
        this.reader = reader;
        this.description = description;
        this.countMetric = countMetric;
    }

    public void addParser(StatParser parser) {
        parserList.add(parser);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> statsMap = null;
        int count = 0;
        if (reader == null) {
            logger.error("Failed to read " + description + " stats as reader is null");
        } else {
            statsMap = new HashMap<String, Object>();
            Pattern pattern = Pattern.compile("^([0-9])");
            try {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        for (StatParser parser : parserList) {
                            if (parser.isMatchType(line)) {
                                List<String> stats = new LinkedList<>(Arrays.asList(line.split(parser.regex)));

                                ListIterator<String> iter = stats.listIterator();
                                while(iter.hasNext()){
                                    if(!(pattern.matcher(iter.next()).find())){
                                        iter.remove();
                                    }
                                }
                                Map<String, String> map = getStatMap(parser.keys, stats);

                                String name = map.remove(Commands.IDENTIFIER);
                                if (parser.isBase(line.split(parser.regex))) {
                                    statsMap.putAll(map);   //put in base dir
                                } else if (name != null) {
                                    if(parser.isCountRequired()) {
                                        count++;
                                    }
                                    statsMap.put(name, map); //put in subdir
                                } else {    //no sub dir name from stats, fall back to description
                                    statsMap.put(description, map);
                                }
                            }
                        }
                    }
                    if(countMetric != null) {
                        statsMap.put(countMetric, Long.valueOf(count));
                    }
                }catch (Exception e) {
                    logger.error("Failed to read " + description + " stats", e);
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                logger.error("Failed to read " + description + " stats IOException", e);
            }
            if (statsMap.size() == 0) {
                statsMap = null;
            }
        }
        return statsMap;
    }

    private Map<String, String> getStatMap(String[] keys, List<String> vals) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < vals.size() && i < keys.length; i++) {
            map.put(keys[i], vals.get(i));
        }
        return map;
    }

    public abstract static class StatParser {
        private String[] keys;
        private String regex;

        public StatParser(String[] keys, String regex) {
            this.keys = keys;
            this.regex = regex;
        }

        /**
         * Condition for parser to parse current line
         *
         * @param line a line from <code>reader</code>
         * @return true if parser should parse <code>line</code>, false otherwise
         */
        abstract boolean isMatchType(String line);

        /**
         * Whether current list of stats should be placed under base dir
         *
         * @param stats array of stats from a line in <code>reader</code>
         * @return true if the stats should be placed under base dir, false if they're to be put in a sub dir
         */
        abstract boolean isBase(String[] stats);

        /**
         * Condition to check if any of the metric in reader needs an increment to have a count metric
         * @return
         */
        abstract boolean isCountRequired();

    }
}
