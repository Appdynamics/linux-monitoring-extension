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
                                } else {    //no sub dir name from stats, fall back to description
                                    statsMap.put(description,map);
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
            if (statsMap.size() == 0){
                statsMap = null;
            }
        }

        return statsMap;
    }

    private Map<String, String> getStatMap(String[] keys, String[] vals) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i=0; i<vals.length && i<keys.length; i++){
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
