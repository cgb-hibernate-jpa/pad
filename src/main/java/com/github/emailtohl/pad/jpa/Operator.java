package com.github.emailtohl.pad.jpa;

/**
 * 操作类型，以下操作符在被注解的属性的值不为null时生效EQ, NEQ, LT, LTE, GT, GTE, LIKE, NOT_LIKE, IN, EMPTY, NOT_EMPTY
 * 此外EMPTY, NOT_EMPTY, NULL, NOT_NULL虽然不需要使用属性的值，但是只有存储任意值后才生效
 * 注意：EMPTY, NOT_EMPTY只能应用于集合属性
 * @author HeLei
 */
public enum Operator {
	EQ, NEQ, LT, LTE, GT, GTE, LIKE, NOT_LIKE, IN, EMPTY, NOT_EMPTY, NULL, NOT_NULL
}
