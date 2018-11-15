package com.github.emailtohl.lib.event;

import org.springframework.context.ApplicationEvent;
/**
 * 基础的应用事件
 * @author HeLei
 */
public class EventBase extends ApplicationEvent {
	private static final long serialVersionUID = 3638756292152803235L;
	/**
	 * @param source 事件发生源
	 */
	public EventBase(Object source) {
		super(source);
	}

	public EventBase() {
		this("event base");
	}
}
