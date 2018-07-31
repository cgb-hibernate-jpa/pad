package com.github.emailtohl.lib.jpa;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 对于基本类型来说，添加该注解后，不论是否初始值，均作为查询条件
 * @author HeLei
 */
@Target({FIELD, METHOD})
@Retention(RUNTIME)
@Documented
public @interface InitialValueAsCondition {

}
