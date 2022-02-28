# AppDynamics Linux Monitoring Extension

This extension works only with the standalone machine agent. It has been tested against Linux 2.6.32 on Ubuntu; info to be updated as tests against other distributions and Linux versions are completed.

# Use Case

The Linux monitoring extension gathers metrics for a Linux machine and sends them to the AppDynamics Metric Browser.

## Pre-requisites
1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.

2. Download and install [Apache Maven](https://maven.apache.org/) which is configured with `Java 8` to build the extension artifact from source. You can check the java version used in maven using command `mvn -v` or `mvn --version`. If your maven is using some other java version then please download java 8 for your platform and set JAVA_HOME parameter before starting maven.

## Installation
1. Clone the "linux-monitoring-extension" repo using `git clone <repoUrl>` command.
2. Run 'mvn clean install' from "linux-monitoring-extension". This will produce a LinuxMonitor-VERSION.zip in the target directory. 
3. Unzip LinuxMonitor-VERSION.zip from `target` directory into the `<MACHINE_AGENT_HOME>/monitors/`
4. Configure the extension by referring to the below section.
5. Restart the Machine Agent. 
 
Please place the extension in the **"monitors"** directory of your **Machine Agent** installation directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.


## Configuration

Note : Please make sure not to use tab (\t) while editing yaml files. You can validate the yaml file using a [yaml validator](https://jsonformatter.org/yaml-validator)

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

|Metric Name                |Description|
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
Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension.

## Contributing

Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/linux-monitoring-extension/).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |2.1.5       |
|Product Tested On         |Ubuntu 16.04|
|Last Update               |08/01/2021  |
|Changes list              |[ChangeLog](https://github.com/Appdynamics/linux-monitoring-extension/blob/master/CHANGELOG.md)|

**Note**: While extensions are maintained and supported by customers under the open-source licensing model, they interact with agents and Controllers that are subject to [AppDynamics’ maintenance and support policy](https://docs.appdynamics.com/latest/en/product-and-release-announcements/maintenance-support-for-software-versions). Some extensions have been tested with AppDynamics 4.5.13+ artifacts, but you are strongly recommended against using versions that are no longer supported.
