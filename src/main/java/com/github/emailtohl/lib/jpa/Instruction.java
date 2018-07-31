package com.github.emailtohl.lib.jpa;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 标注在字段或JavaBean属性上，用于扩展性的条件说明，例如指明实体birthday属性大于1980-01-01小于1990-01-01。
 * @author HeLei
 */
@Target({FIELD, METHOD})
@Retention(RUNTIME)
@Documented
public @interface Instruction {
	/**
	 * 需要做比较的实体的属性名，默认是空字符串，表示与自身代表的实体属性做比较
	 * @return 需要比较的实体的属性名
	 */
	String propertyName() default "";
	/**
	 * @return 比较的方式
	 */
	Operator operator();
}
