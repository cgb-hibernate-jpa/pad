package com.github.emailtohl.pad.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 超时
 * @author HeLei
 *
 */
@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class TimeOutException extends RestException{
	private static final long serialVersionUID = -6335237943473851328L;

	public TimeOutException() {}

	public TimeOutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {}

	public TimeOutException(String message, Throwable cause) {}

	public TimeOutException(String message) {}

	public TimeOutException(Throwable cause) {}

}
