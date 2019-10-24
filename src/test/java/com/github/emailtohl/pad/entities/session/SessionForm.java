package com.github.emailtohl.pad.entities.session;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.github.emailtohl.pad.entities.session.SpringSession;
import com.github.emailtohl.pad.jpa.Instruction;
import com.github.emailtohl.pad.jpa.Operator;
import com.github.emailtohl.pad.util.LocalDateUtil;

public class SessionForm extends SpringSession {
	private static final long getTime(LocalDate localDate) {
		return LocalDateUtil.toDate(localDate).getTime();
	}
	
	@Instruction(propertyName = "sessionId", operator = Operator.NEQ)
	public String notIsSessionId = "-1234567890";
	
	@Instruction(propertyName = "principalName", operator = Operator.NOT_LIKE)
	public String notlike = "bar";

	@Instruction(propertyName = "creationTime", operator = Operator.GT)
	public long history = getTime(LocalDate.now().minusDays(1));
	
	@Instruction(propertyName = "creationTime", operator = Operator.GTE)
	public long History = getTime(LocalDate.now().minusDays(1));
	
	private long future = getTime(LocalDate.now().plusDays(1));
	
	public long Future = getTime(LocalDate.now().plusDays(1));
	
	@Instruction(propertyName = "creationTime", operator = Operator.LT)
	public long getFuture() {
		return future;
	}
	public void setFuture(long future) {
		this.future = future;
	}

	@Instruction(propertyName = "principalName", operator = Operator.IN)
	public String[] names = {"foo", "bar", "baz", "qux"};

	@Instruction(propertyName = "principalName", operator = Operator.IN)
	public Set<String> Names = new HashSet<>(Arrays.asList("foo", "bar", "baz", "qux"));
	
}
