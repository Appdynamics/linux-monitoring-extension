/**
 * Copyright 2015 AppDynamics
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

import com.appdynamics.extensions.linux.config.MountedNFS;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by balakrishnav on 19/10/15.
 */
public class NFSMountStatusProcessor {
    private Logger logger = Logger.getLogger(NFSMountStatusProcessor.class);

    private static String[] NFS_IO_FILE_STATS = {"tps", "kB_read/s", "kB_wrtn/s", "kB_read", "kB_wrtn"};

    private String[] command = {"df | grep %s | wc -l"};

    private String nfsIOStatsCmd = "iostat -d %s";
    private static final String SPACE_REGEX = "[\t ]+";

    public String execute(String fileSystem) {

        Runtime rt = Runtime.getRuntime();
        Process p = null;
        BufferedReader input = null;
        String formattedCommand = "";
        try {
            formattedCommand = String.format(command[0], fileSystem);
            p = rt.exec(new String[]{"bash", "-c", formattedCommand});
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            if ((line = input.readLine()) != null) {

                logger.debug("NFS mount output for "+ fileSystem + " is: " + line);
                return line;
            }
        } catch (Exception e) {

            logger.info("NFS mount error: " + e);
        }
        return "";
    }

    public Map<String, Object> getNFSMetrics(final MountedNFS fileSystem){

            String formattedCommand = "";

            Map<String, Object> statsMap = new HashMap<String, Object>();
            try {
                formattedCommand = String.format(nfsIOStatsCmd, fileSystem.getFileSystem());

               List<String> processListOutput = CommandExecutor.execute(formattedCommand);

               for(String line: processListOutput){
                    if(line.contains(fileSystem.getFileSystem())) {
                        String[] stats = line.trim().split(SPACE_REGEX);
                        for (int i = 0; i < NFS_IO_FILE_STATS.length; i++) {
                            statsMap.put(NFS_IO_FILE_STATS[i], stats[i+1]);
                        }
                    }
                 }

            } catch (Exception e) {
                logger.error("Command ran '" + nfsIOStatsCmd + "' for nfsIOStats" + e);
            }

            return statsMap;
        }

}
