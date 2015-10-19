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

import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by balakrishnav on 16/10/15.
 */
public class LinuxMonitorTest {

    @Test
    public void testLinuxMonitor() throws TaskExecutionException {
        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "src/test/resources/conf/config.yml");

        LinuxMonitor monitor = new LinuxMonitor();
        monitor.execute(taskArgs, null);
    }

    @Test
    public void testNFSMountStatusProcessor() {
        NFSMountStatusProcessor processor = new NFSMountStatusProcessor();
        Assert.assertEquals(processor.execute("/dev/sda1"), "1");

    }
}
