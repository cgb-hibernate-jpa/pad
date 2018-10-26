package com.github.emailtohl.lib.jpa;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 排除的属性，不作为查询项
 * @author HeLei
 */
@Target({FIELD, METHOD})
@Retention(RUNTIME)
@Documented
public @interface ExcludeCondition {

}
