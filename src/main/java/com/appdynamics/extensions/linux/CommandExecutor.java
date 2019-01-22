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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class CommandExecutor {
    private static Logger logger = Logger.getLogger(CommandExecutor.class);

    public static List<String> execute(String[] command) {
        return init(command);
    }

    public static List<String> init(String[] command) {
        Process process;
        try {
            logger.debug("Executing the command " + command);
            process = Runtime.getRuntime().exec(command);

            new ErrorReader(process.getErrorStream()).start();
            ResponseParser responseParser = new ResponseParser(process);
            responseParser.start();
            process.waitFor();
            responseParser.join();
            List<String> commandOutput = responseParser.getData();
            logger.trace("Command Output: " + commandOutput);
            return commandOutput;
        } catch (Exception e) {
            logger.error("Error while executing the process " + command, e);
            return null;
        }
    }

    public static class ResponseParser extends Thread {

        private Process process;
        private List<String> data = new ArrayList<String>();

        public ResponseParser(Process process) {
            this.process = process;
        }

        public void run() {
            InputStream inputStream = process.getInputStream();
            BufferedReader input = null;
            try {
                input = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = input.readLine()) != null) {
                    data.add(line);
                }
            } catch (Exception e) {
                logger.error("Error while reading the input stream from the command ", e);
            } finally {
                try {
                    input.close();
                } catch (IOException e) {
                }
                if (process != null) {
                    logger.trace("Destroying the process ");
                    process.destroy();
                }
            }
        }

        public List<String> getData() {
            return data;
        }

    }


    public static class ErrorReader extends Thread {
        public static final Logger logger = Logger.getLogger(ErrorReader.class);


        private final InputStream in;

        public ErrorReader(InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String temp;
            try {
                while ((temp = reader.readLine()) != null) {
                    logger.error("Process Error - " + temp);
                }
            } catch (IOException e) {
                logger.error("Error while reading the contents of the the error stream", e);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            logger.trace("Closing the Error Reader " + Thread.currentThread().getName());
        }
    }
}