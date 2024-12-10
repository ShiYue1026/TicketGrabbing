package com.damai.toolkit;

import cn.hutool.core.date.SystemClock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class SnowflakeIdGenerator {

    private static final long BASIS_TIME = 1288834974657L;
    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    private final long sequenceBits = 12L;
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;

    private long lastTimestamp = -1L;

    private InetAddress inetAddress;

    public SnowflakeIdGenerator(WorkDataCenterId workDataCenterId) {
        if(Objects.nonNull(workDataCenterId.getDataCenterId())){
            this.workerId = workDataCenterId.getWorkId();
            this.datacenterId = workDataCenterId.getDataCenterId();
        }
        else{
            this.datacenterId = getDatacenterId(maxDatacenterId);
            this.workerId = getMaxWorkerId(datacenterId, maxWorkerId);
        }
    }

    private void initLog() {
        if (log.isDebugEnabled()) {
            log.debug("Initialization SnowflakeIdGenerator datacenterId:" + this.datacenterId + " workerId:" + this.workerId);
        }
    }

    protected long getMaxWorkerId(long datacenterId, long maxWorkerId){
        StringBuilder mpid = new StringBuilder();

        // 将数据中心 ID 添加到标识符中
        mpid.append(datacenterId);

        // 获取当前 JVM 进程的名称 (格式为: PID@主机名)
        String name = ManagementFactory.getRuntimeMXBean().getName();

        // 检查进程名是否为空 (非空时拼接进程 ID)
        if (StringUtils.isNotBlank(name)) {
            mpid.append(name.split("@")[0]);  // 提取进程 PID，拼接到 mpid 中
        }

        // 对拼接字符串进行哈希计算，并取模，生成 workerId
        return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }

    protected long getDatacenterId(long maxDatacenterId) {
        long id = 0L;
        try {
            if(null == this.inetAddress){
                this.inetAddress = InetAddress.getLocalHost();
            }
            NetworkInterface network = NetworkInterface.getByInetAddress(this.inetAddress);
            if (null == network) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    id = ((0x000000FF & (long) mac[mac.length - 2]) | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                    id = id % (maxDatacenterId + 1);
                }
            }
        } catch (Exception e){
            log.warn(" getDatacenterId: " + e.getMessage());
        }
        return id;
    }

    public long getBase() {
        int five = 5;
        long timestamp = timeGen();

        if(timestamp < lastTimestamp){
            long offset = lastTimestamp - timestamp;
            if(offset <= five){
                try{
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            else{
                throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
            }
        }

        if(lastTimestamp == timestamp) {  // 相同毫秒内，序列号自增
            sequence = (sequence + 1) & sequenceMask;
            if(sequence == 0){  // 同一毫秒的序列数已经达到最大
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        else{  // 不同毫秒内，序列号置为1 - 2随机数
            sequence = ThreadLocalRandom.current().nextLong(1, 3);
        }
        lastTimestamp = timestamp;

        return timestamp;
    }

    public synchronized long nextId() {
        long timestamp = getBase();

        return ((timestamp - BASIS_TIME) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    public synchronized long getOrderNumber(long userId,long tableCount) {  // 基因法生成订单id
        long timestamp = getBase();
        long sequenceShift = log2N(tableCount);
        return ((timestamp - BASIS_TIME) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | (sequence << sequenceShift)
                | (userId % tableCount);
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return SystemClock.now();
    }

    public long log2N(long count) {
        return (long)(Math.log(count)/ Math.log(2));
    }
}
