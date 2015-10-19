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

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by balakrishnav on 19/10/15.
 */
public class NFSMountStatusProcessor {
    private Logger logger = Logger.getLogger(NFSMountStatusProcessor.class);
    private String command = "df | grep %s | wc -l";

    public String execute(String fileSystem) {
        Runtime rt = Runtime.getRuntime();
        Process p = null;
        BufferedReader input = null;
        String formattedCommand = "";
        try {
            formattedCommand = String.format(command, fileSystem);
            p = rt.exec(new String[]{"bash", "-c", formattedCommand});
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            if ((line = input.readLine()) != null) {
                return line;
            }
        } catch (Exception e) {

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
}
