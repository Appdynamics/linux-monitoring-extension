package com.appdynamics.monitors.linux;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FileParser {
    private BufferedReader reader;
    private String description;
    private List<StatParser> parserList = new ArrayList<StatParser>();

    private Logger logger;

    public FileParser(BufferedReader reader, String description, Logger logger) {
        this.reader = reader;
        this.description = description;
        this.logger = logger;
    }

    public void addParser(StatParser parser){
        parserList.add(parser);
    }

    public Map<String, Object> getStats(){
        Map<String, Object> statsMap = null;
        if (reader == null) {
            logError();
        } else {
            statsMap = new HashMap<String, Object>();

            try {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        for (StatParser parser : parserList) {
                            if (parser.isMatchType(line)){
                                String[] stats = line.split(parser.regex);
                                Map<String, String> map = getStatMap(parser.keys, stats);
                                String name = map.remove(Stats.IDENTIFIER);
                                if (parser.isBase(stats)) {
                                    statsMap.putAll(map);   //put in base dir
                                } else if (name != null){
                                    statsMap.put(name,map); //put in subdir
                                }
                            }
                        }
                    }
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                logError();
            }
        }

        return statsMap;
    }

    private Map<String, String> getStatMap(String[] keys, String[] vals) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i=1; i<vals.length && i<keys.length; i++){
            map.put(keys[i],vals[i]);
        }
        return map;
    }

    public void logError(){
        logger.error("Failed to read " + description + " stats");
    }

    public abstract static class StatParser {
        private String[] keys;
        private String regex;

        public StatParser(String[] keys, String regex){
            this.keys = keys;
            this.regex = regex;
        }

        /**
         * Condition for parser to parse current line
         * @param line  a line from <code>reader</code>
         * @return true if parser should parse <code>line</code>, false otherwise
         */
        abstract boolean isMatchType(String line);

        /**
         * Whether current list of stats should be placed under base dir
         * @param stats array of stats from a line in <code>reader</code>
         * @return true if the stats should be placed under base dir, false if they're to be put in a sub dir
         */
        abstract boolean isBase(String[] stats);
    }
}
