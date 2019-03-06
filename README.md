# AppDynamics Linux Monitoring Extension

This extension works only with the standalone machine agent. It has been tested against Linux 2.6.32 on Ubuntu; info to be updated as tests against other distributions and Linux versions are completed.

# Use Case

The Linux monitoring extension gathers metrics for a Linux machine and sends them to the AppDynamics Metric Browser.

## Pre-requisites
Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.

## Installation

1. To build from source, clone this repository and run 'mvn clean install'. This will produce a LinuxMonitor-VERSION.zip in the target directory. Alternatively, download the latest release archive from [Github](https://github.com/Appdynamics/linux-monitoring-extension/releases)
2. Unzip LinuxMonitor.zip and copy the 'LinuxMonitor' directory to `<MACHINE_AGENT_HOME>/monitors/`
3. Configure the extension by referring to the below section.
4. Restart the Machine Agent. 
 
In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | Linux (or the custom path you specified).


## Configuration

Note : Please make sure not to use tab (\t) while editing yaml files. You can validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the Linux Extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/LinuxMonitor/`.
2. The metricPrefix of the extension has to be configured as specified [here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695#Configuring%20an%20Extension). Please make sure that the right metricPrefix is chosen based on your machine agent deployment, otherwise this could lead to metrics not being visible in the controller.
3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/LinuxMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/LinuxMonitor/config.yml" />
          ....
     </task-arguments>
    ```
4. filters: Filters for disk, CPU and mountedNFS can be configured in config.yml to fetch data only for particular entities. Be default, all entities are set to report.
5. All metrics to be reported are configured in metrics.xml. Users can remove entries from metrics.xml to stop the metric from reporting.


## Metrics
### Metric Category: CPU

|Metric Name            	|Description|
|------------------------------	|------------|
|nice                   | niced processes executing in user mode
|iowait					| waiting for I/O to complete
|system					| processes executing in kernel mode
|idle                   | twiddling thumbs
|irq                    | servicing interrupts
|softirq                | servicing softirqs
|CPU cores (logical)    | Number of CPU cores

### Metric Category: disk

|Metric Name            	|Description|
|------------------------------	|------------|
|I/Os currently in progress		|Current I/O operations	
|reads completed successfully 	|Number of successful reads
|reads merged					|Reads merged
|sectord read 					|Sectors read
|sectors written 				|Sectors written 
|time spent doing I/Os(ms) 		|time in ms spent doing I/Os 
|time spent reading(ms) 		|time in ms spent reading 
|time spent writing(ms)			|time in ms spent writing 		 
|writes completed 				|Number of writes completed 
|writes merged 					|Number of writes merged
|Avg I/O Utilization %          |Percentage of CPU time during which I/O requests were issued to the device

### Metric Category: disk usage

|Metric Name            	|Description|
|------------------------------	|------------|
|size (MB)	                    |	
|used (MB) 	                    |
|available (MB)					|
|use %       					|

### Metric Category: file

|Metric Name            	|Description|
|------------------------------	|------------|
|agelimit						|Age limit
|fhalloc						|fhalloc
|fhfree 						|fhfree
|unused							|unused

### Metric Category: load average

|Metric Name            	|Description|
|------------------------------	|------------|
|load avg (1 min) 				|Load average 1 Minute
|load avg (5 min) 				|Load average 5 Minute
|load avg (15 min) 				|Load average 15 Minute

### Metric Category: memory

|Metric Name            	|Description|
|------------------------------	|------------|
|free                           |Free Memory
|buffers 						|Buffers 
|cached 						|Cached
|swapCached                     |Swap Cached
|commit limit 					|Commit limit 
|dirty 							|Dirty
|active 						|Active
|inactive 						|Inactive
|swapTotal                      |Swap Total
|swapFree                       |Swap Free
|writeback                      |WriteBack
|mapped 						|Mapped
|slab 						    |Memory Slab
|commited_as					|Committed As

### Metric Category: network

|Metric Name            	|Description|
|------------------------------	|------------|
|receive bytes 					|Receive bytes
|receive compressed 			|Receive compressed
|receive drop 					|Receive drop
|receive errs 					|Receive errors
|receive packets 				|Receive packets
|transmit bytes 				|Transmit bytes
|transmit compressed 			|Transmit compresses
|transmit drop 					|Transmit drop
|transmit errs 					|Transmit errors
|transmit packets 				|Transmit packets

### Metric Category: page

|Metric Name            	|Description|
|------------------------------	|------------|
|page fault 					|Page fault
|page in 						|Page in
|page out 						|Page out
|swap page in					|Swap page in
|swap page out					|Swap page out

### Metric Category: process

|Metric Name            	|Description|
|------------------------------	|------------|
|blocked 						|Blocked processes
|count 							|Total number of processes
|processes 						|Nnumber of processes
|running 						|Running processes
|runqueue 						|Processes in run queue

### Metric Category: socket

|Metric Name            	|Description|
|------------------------------	|------------|
|raw 							|Raw sockets
|tcp 							|TCP sockets
|udp 							|UDP sockets
|used 							|Used sockets

### Metric Category: mountedNFSStatus
In addition to the above metrics for configured mounts, an availability metrics for any external network file system (NFS) mount is reported as well.
|------------------------------	|------------|
|1K-blocks 							|Number of 1-K blocks
|used (MB) 							|Used space
|available (MB) 					|Available space
|used %							    |Percentage of space used


### Metric Category: nfsIOStats
The storage metrics for any external network file system (NFS) mounts is reported by executing the command `iostat -d <fileSystem>`. Following metrics are reported:

|Metric Name            	|Description|
|------------------------------	|------------|
|op/s   						|This is the number of operations per second.
|kB/s                           |This is the number of kB written/read per second.
|kB/op                          |This is the number of kB written/read per each operation.
|avg RTT (ms)                   |This is the duration from the time that client’s kernel sends the RPC request until the time it receives the reply.
|avg exe (ms)                   |This is the duration from the time that NFS client does the RPC request to its kernel until the RPC request is completed, this includes the RTT time above.


## Credentials Encryption
Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

## Troubleshooting
Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension. If these don't solve your issue, please follow the last step on the [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) to contact the support team.

## Support Tickets
If after going through the [Troubleshooting Document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) you have not been able to get your extension working, please file a ticket and add the following information.

Please provide the following in order for us to assist you better.

    1. Stop the running machine agent.
    2. Delete all existing logs under <MachineAgent>/logs.
    3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug.
        <logger name="com.singularity">
        <logger name="com.appdynamics">
    4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
    5. Attach the zipped <MachineAgent>/conf/* directory here.
    6. Attach the zipped <MachineAgent>/monitors/ExtensionFolderYouAreHavingIssuesWith directory here.

For any support related questions, you can also contact help@appdynamics.com.


## Contributing

Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/apache-monitoring-extension/).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |2.1.1       |
|Controller Compatibility  |4.5.0       |
|Product Tested On         |Ubuntu 16.04|
|Last Update               |03/06/2019  |
