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
import com.google.common.base.Strings;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by balakrishnav on 19/10/15.
 */
public class NFSMountStatusProcessor {
    private Logger logger = Logger.getLogger(NFSMountStatusProcessor.class);

    private static String[] NFS_IO_FILE_STATS = {"tps", "kB_read/s", "kB_wrtn/s", "kB_read", "kB_wrtn"};

    private String command = "df | grep %s | wc -l";

    private String nfsIOStatsCmd = "iostat -d %s";
    private static final String SPACE_REGEX = "[\t ]+";

    public String execute(String fileSystem) {
        Runtime rt = Runtime.getRuntime();
        Process p = null;
        BufferedReader input = null;
        String formattedCommand = "";
        try {
            formattedCommand = String.format(command, fileSystem);
            System.out.println("formatted command: " + formattedCommand);
            p = rt.exec(new String[]{"bash", "-c", formattedCommand});
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            if ((line = input.readLine()) != null) {

                logger.debug("NFS mount output for "+ fileSystem + " is: " + line);
                return line;
            }
        } catch (Exception e) {

            logger.info("NFS mount error: " + e);
        } finally {
            closeBufferedReader(input);
            cleanUpProcess(p, formattedCommand);
        }
        return "";
    }

    protected void cleanUpProcess(Process p, String cmd) {
        try {
            if (p != null) {
                int exitValue = p.waitFor();
                if (exitValue != 0) {
                    logger.warn("Unable to terminate the command " + cmd + " normally. ExitValue = " + exitValue);
                }
                p.destroy();
            }
        } catch (InterruptedException e) {
            logger.error("Execution of command " + cmd + " got interrupted ", e);
        }
    }

    protected void closeBufferedReader(BufferedReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                logger.error("Exception while closing the reader: ", e);
            }
        }
    }

    public Map<String, Object> getNFSMetrics(final MountedNFS fileSystem){

            BufferedReader reader = null;
            Process process = null;
            String formattedCommand = "";

            Map<String, Object> statsMap = new HashMap<String, Object>();
            try {
                formattedCommand = String.format(nfsIOStatsCmd, fileSystem.getFileSystem());

                process = Runtime.getRuntime().exec(formattedCommand);
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null ) {
                    if(line.contains(fileSystem.getFileSystem())) {
                        String[] stats = line.trim().split(SPACE_REGEX);
                        for (int i = 0; i < NFS_IO_FILE_STATS.length; i++) {
                            statsMap.put(NFS_IO_FILE_STATS[i], stats[i]);
                        }
                    }
                 }

            } catch (InterruptedIOException e) {
                logger.error("Command failed to run '" + nfsIOStatsCmd + "' for nfsIOStats" + e);
            }
            catch (Exception e) {
                logger.error("Command ran '" + nfsIOStatsCmd + "' for nfsIOStats" + e);
            }

            return statsMap;
        }

}
