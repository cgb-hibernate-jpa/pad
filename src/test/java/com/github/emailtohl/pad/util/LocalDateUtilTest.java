package com.github.emailtohl.pad.util;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.Test;

import com.github.emailtohl.pad.util.LocalDateUtil;

public class LocalDateUtilTest {

	@Test
	public void testDateLocalDate() {
		String format = "yyyy-MM-dd";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date today = new Date();
		String s = sdf.format(today);
		LocalDate d = LocalDateUtil.toLocalDate(today);
		assertEquals(s, d.format(DateTimeFormatter.ofPattern(format)));
		System.out.println(LocalDateUtil.toDate(d));
	}

	@Test
	public void testLocalDateTime() {
		String format = "HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date today = new Date();
		String s = sdf.format(today);
		LocalDateTime dt = LocalDateUtil.toLocalDateTime(today);
		assertEquals(s, dt.format(DateTimeFormatter.ofPattern(format)));
		System.out.println(LocalDateUtil.toDate(dt));
	}

}
