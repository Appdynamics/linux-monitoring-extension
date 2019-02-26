package com.appdynamics.extensions.linux;

/**
 * Created by akshay.srivastava on 9/12/18.
 */
public class Constants {

    public static final String STAT_PATH = "/proc/stat";
    public static final String NET_STAT_PATH = "/proc/net/dev";
    public static final String DISK_STAT_PATH = "/proc/diskstats";
    public static final String MEM_STAT_PATH = "/proc/meminfo";
    public static final String FILE_NR_STAT_PATH = "/proc/sys/fs/file-nr";
    public static final String INODE_NR_STAT_PATH = "/proc/sys/fs/inode-nr";
    public static final String DENTRIES_STAT_PATH = "/proc/sys/fs/dentry-state";
    public static final String LOADAVG_STAT_PATH = "/proc/loadavg";
    public static final String VM_STAT_PATH = "/proc/vmstat";
    public static final String SOCK_STAT_PATH = "/proc/net/sockstat";

    public static final String[] DISK_USAGE_CMD = {"bash", "-c", "exec df -mP 2>/dev/null"};
    public static final String SPACE_REGEX = "[\t ]+";
    public static final String SPACE_COLON_REGEX = "[\t :]+";

    public static String[] MEM_FILE_STATS =
            {"MemTotal", "MemFree", "Buffers", "Cached", "SwapCached", "Active", "Inactive", "SwapTotal", "SwapFree",
                    "Dirty", "Writeback", "Mapped", "Slab", "CommitLimit", "Committed_AS"};
    public static String[] PAGE_SWAP_FILE_STATS = {"pgpgin", "pgpgout", "pswpin", "pswpout", "pgfault", "pgmajfault"};
    public static String[] PROC_FILE_STATS = {"processes", "procs_running", "procs_blocked"};
    public static String[] SOCK_STATS = {"sockets:", "tcp:","udp:","raw:","frag:"};

}
