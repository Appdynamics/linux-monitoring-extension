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

import com.google.common.collect.Lists;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
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

    @Test
    public void testIsDiskIncluded() {
        Stats stats = new Stats(null);

        String line = " 253     201 dm-201 7759 0 1480475 25933 13112075 0 104896600 92663065 0 4608730 92797611";
        List<String> diskIncludes = Lists.newArrayList();
        diskIncludes.add("*");
        Assert.assertEquals(stats.isDiskIncluded(line, diskIncludes), true);

        diskIncludes.clear();
        diskIncludes.add("sda1");
        diskIncludes.add("as.*");
        diskIncludes.add("*");
        Assert.assertEquals(stats.isDiskIncluded(line, diskIncludes), true);

        diskIncludes.clear();
        diskIncludes.add("sda1");
        diskIncludes.add("as.*");
        Assert.assertEquals(stats.isDiskIncluded(line, diskIncludes), false);

        diskIncludes.clear();
        diskIncludes.add("dm.*");
        Assert.assertEquals(stats.isDiskIncluded(line, diskIncludes), true);

        String line1 = "252       0 asm/.asm_ctl_spec 0 0 0 0 0 0 0 0 0 0 0";
        diskIncludes.clear();
        diskIncludes.add("as.*");
        /*diskIncludes.add("sda1");
        diskIncludes.add("dm.*");*/
        Assert.assertEquals(stats.isDiskIncluded(line1, diskIncludes), true);

    }
}
