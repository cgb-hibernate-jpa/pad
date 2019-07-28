package com.github.emailtohl.lib.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Java8 本地时间的一些常用功能
 * 
 * @author HeLei
 */
public final class LocalDateUtil {
	
	private LocalDateUtil() {}

	/**
	 * @param localDate java.time.LocalDate
	 * @return Date java.util.Date
	 */
	public static Date toDate(LocalDate localDate) {
		ZoneId zoneId = ZoneId.systemDefault();
		ZonedDateTime zonedDateTime = localDate.atStartOfDay(zoneId);
		return Date.from(zonedDateTime.toInstant());
	}

	/**
	 * @param date java.util.Date
	 * @return LocalDate java.time.LocalDate
	 */
	public static LocalDate toLocalDate(Date date) {
		// java.sql.Date的toInstant方法不能使用
		Date _date;
		if (date instanceof java.sql.Date) {
			_date = new Date(date.getTime());
		} else {
			_date = date;
		}
		Instant instant = _date.toInstant();
		ZoneId zoneId = ZoneId.systemDefault();
		// atZone()方法返回在指定时区从此Instant生成的ZonedDateTime。
		return instant.atZone(zoneId).toLocalDate();
	}

	/**
	 * @param date java.util.Date
	 * @return java.time.LocalDateTime
	 */
	public static LocalDateTime toLocalDateTime(Date date) {
		// java.sql.Date的toInstant方法不能使用
		Date _date;
		if (date instanceof java.sql.Date) {
			_date = new Date(date.getTime());
		} else {
			_date = date;
		}
		Instant instant = _date.toInstant();
		ZoneId zone = ZoneId.systemDefault();
		return LocalDateTime.ofInstant(instant, zone);
	}

	/**
	 * @param localDateTime java.time.LocalDateTime
	 * @return java.util.Date
	 */
	public static Date toDate(LocalDateTime localDateTime) {
		ZoneId zone = ZoneId.systemDefault();
		Instant instant = localDateTime.atZone(zone).toInstant();
		return Date.from(instant);
	}
}
