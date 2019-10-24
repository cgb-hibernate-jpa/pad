package com.github.emailtohl.pad.util;

import com.github.emailtohl.pad.exception.InnerDataStateException;
import com.github.emailtohl.pad.exception.NotAcceptableException;

/**
 * <h1>id生成工具——SnowFlake雪花算法 Twitter_Snowflake</h1>
 * <p>
 * 其核心思想是： 使用41bit作为毫秒数，10bit作为机器的ID（5个bit是数据中心，5个bit的机器ID），
 * 12bit作为毫秒内的流水号（意味着每个节点在每毫秒可以产生 4096 个 ID），最后还有一个符号位，永远是0。
 * </p>
 * 优点在于：
 * <ol>
 * <li>简单高效，生成速度快</li>
 * <li>时间戳在高位，自增序列在低位，整个ID是趋势递增的，按照时间有序递增</li>
 * <li>灵活度高，可以根据业务需求，调整bit位的划分，满足不同的需求</li>
 * </ol>
 * 缺点在于：
 * <ol>
 * <li>依赖机器的时钟，如果服务器时钟回拨，会导致重复ID生成</li>
 * <li>在分布式环境上，每个服务器的时钟不可能完全同步，有时会出现不是全局递增的情况</li>
 * </ol>
 * SnowFlake的结构如下(每部分用-分开):
 * <p>0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000</p>
 * <p>1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0</p>
 * <p>
 * 41位时间戳(毫秒级)，<b>注意，41位时间戳不是存储当前时间的时间戳，而是存储时间戳的差值（当前时间戳 - 开始时间戳)
 * 得到的值），</b>这里的的开始时间戳，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序START_STMP属性）。
 * 41位的时间戳，可以使用69年，年T = (1L &lt;&lt; 41) / (1000L * 60 * 60 * 24 * 365) = 69
 * </p>
 * <p>10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位machineId</p>
 * <p>12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间戳)产生4096个ID序号 加起来刚好64位，为一个Long型</p>
 * <p>
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，
 * 经测试，SnowFlake每秒能够产生11万ID左右
 * </p>
 */
public class SnowFlake {
	/**
	 * 起始的时间戳(2019-03-26)
	 */
	private final static long START_STMP = 1553529600000L;

	/**
	 * 每一部分占用的位数
	 */
	private final static long SEQUENCE_BIT = 12L; // 序列号占用的位数
	private final static long MACHINE_BIT = 5L; // 机器标识占用的位数
	private final static long DATACENTER_BIT = 5L;// 数据中心占用的位数

	/**
	 * 每一部分的最大值, 移位算法可以很快的计算出几位二进制数所能表示的最大十进制数
	 */
	public final static long MAX_DATACENTER_NUM = -1L ^ (-1L << DATACENTER_BIT);
	public final static long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
	private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);

	/**
	 * 每一部分向左的位移
	 */
	private final static long MACHINE_LEFT = SEQUENCE_BIT;
	private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
	private final static long TIMESTMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

	private long datacenterId; // 数据中心标识
	private long machineId; // 机器标识
	private long sequence = 0L; // 序列号
	private long lastStmp = -1L; // 上一次时间戳

	/**
	 * 构造器,需指定数据中心id和机器id,以保证整个分布式系统内不会产生ID碰撞
	 * 
	 * @param datacenterId 数据中心ID (0~MAX_DATACENTER_NUM)
	 * @param machineId 机器ID (0~MAX_MACHINE_NUM)
	 */
	public SnowFlake(long datacenterId, long machineId) {
		if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
			throw new NotAcceptableException(
					String.format("datacenterId can't be greater than %d or less than 0", MAX_DATACENTER_NUM));
		}
		if (machineId > MAX_MACHINE_NUM || machineId < 0) {
			throw new NotAcceptableException(
					String.format("machineId can't be greater than %d or less than 0", MAX_MACHINE_NUM));
		}
		this.datacenterId = datacenterId;
		this.machineId = machineId;
	}

	/**
	 * 产生下一个id
	 *
	 * @return 下一个id
	 */
	public synchronized long nextId() {
		long currStmp = getNewstmp();
		// 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
		if (currStmp < lastStmp) {
			throw new InnerDataStateException("Clock moved backwards.  Refusing to generate id");
		}
		// 如果是同一时间生成的，则进行毫秒内序列
		if (currStmp == lastStmp) {
			// 相同毫秒内，序列号自增
			sequence = (sequence + 1) & MAX_SEQUENCE;
			// 同一毫秒的序列数已经达到最大
			if (sequence == 0L) {
				currStmp = getNextMill();
			}
		} else {
			// 不同毫秒内，序列号置为0
			sequence = 0L;
		}
		lastStmp = currStmp;

		return (currStmp - START_STMP) << TIMESTMP_LEFT // 时间戳部分
				| datacenterId << DATACENTER_LEFT // 数据中心部分
				| machineId << MACHINE_LEFT // 机器标识部分
				| sequence; // 序列号部分
	}

	private long getNextMill() {
		long mill = getNewstmp();
		while (mill <= lastStmp) {
			mill = getNewstmp();
		}
		return mill;
	}

	private long getNewstmp() {
		return System.currentTimeMillis();
	}

}
