package com.github.emailtohl.lib.jpa;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.persistence.AccessType;

import com.github.emailtohl.lib.exception.InnerDataStateException;

/**
 * 从@Instructions注解中提取的数据
 * 
 * @author HeLei
 */
class Condition {
	/**
	 * 实体的属性名，用于条件比较中
	 */
	final String propertyName;
	/**
	 * 自身的属性名，若@Instruction的propertyName为空字符，则两者表示相同的类属性
	 * 主要用于equals && hashcode唯一性确认中
	 */
	final String selfName;
	/**
	 * 比较符
	 */
	final Operator operator;
	/**
	 * 属性描述器
	 */
	private final PropertyDescriptor propertyDescriptor;
	/**
	 * 属性描述器中的Getter方法
	 */
	private final Method getter;
	/**
	 * 字段
	 */
	private final Field field;
	/**
	 * 访问类型
	 */
	private final AccessType accessType;
	/**
	 * read方法没有参数
	 */
	private final Object[] args = new Object[] {};

	/**
	 * 构造器，getter与field参数不能全为null，在调用getValue方法时以getter优先
	 * 
	 * @param propertyDescriptor JavaBean的属性描述器
	 * @param field 类的属性字段
	 * @throws IllegalArgumentException 如果没有getter方法或没有标注@instruction，则抛出异常
	 */
	Condition(PropertyDescriptor propertyDescriptor, Field field) {
		if (propertyDescriptor != null) {
			this.getter = propertyDescriptor.getReadMethod();
			if (this.getter == null) {
				throw new IllegalArgumentException("There's no getter method in " + propertyDescriptor.getName());
			}
			Instruction anno = this.getter.getAnnotation(Instruction.class);
			if (anno == null) {
				throw new IllegalArgumentException(
						"The @instruction is not marked on the getter method of the " + propertyDescriptor.getName());
			}
			this.selfName = propertyDescriptor.getName();
			this.propertyName = anno.propertyName().isEmpty() ? this.selfName : anno.propertyName();
			this.operator = anno.operator();
			this.propertyDescriptor = propertyDescriptor;
			this.field = null;
			this.getter.setAccessible(true);
			this.accessType = AccessType.PROPERTY;
		} else if (field != null) {
			field.setAccessible(true);
			Instruction anno = field.getAnnotation(Instruction.class);
			if (anno == null) {
				throw new IllegalArgumentException(
						"The @instruction is not marked on the field of the " + field.getName());
			}
			this.selfName = field.getName();
			this.propertyName = anno.propertyName().isEmpty() ? this.selfName : anno.propertyName();
			this.operator = anno.operator();
			this.field = field;
			this.propertyDescriptor = null;
			this.getter = null;
			this.accessType = AccessType.FIELD;
		} else {
			throw new IllegalArgumentException("PropertyDescriptor and field cannot all be null");
		}
	}

	/**
	 * 获取该属性的值
	 * 
	 * @param entity 实体对象
	 * @return entity在this.name属性上的值
	 */
	Object getValue(Object entity) {
		try {
			if (AccessType.PROPERTY == accessType) {
				return getter.invoke(entity, args);
			} else {
				assert AccessType.FIELD == accessType;
				return field.get(entity);
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new InnerDataStateException(e);
		}
	}

	/**
	 * @return 返回属性的类型
	 */
	Class<?> getType() {
		if (AccessType.PROPERTY == accessType) {
			return propertyDescriptor.getPropertyType();
		} else {
			assert AccessType.FIELD == accessType;
			return field.getType();
		}
	}
	
	/**
	 * 获取该属性上所有的注解
	 * @return 如果不存在该注解，则返回null
	 */
	<A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		if (AccessType.PROPERTY == accessType) {
			return getter.getAnnotation(annotationClass);
		} else {
			assert AccessType.FIELD == accessType;
			return field.getAnnotation(annotationClass);
		}
	}
	
	/**
	 * 类上的属性名（可能不是实体的属性名）
	 * @return 属性名
	 */
	String fromName() {
		if (AccessType.PROPERTY == accessType) {
			return propertyDescriptor.getName();
		} else {
			assert AccessType.FIELD == accessType;
			return field.getName();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((selfName == null) ? 0 : selfName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Condition other = (Condition) obj;
		if (selfName == null) {
			if (other.selfName != null)
				return false;
		} else if (!selfName.equals(other.selfName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Condition [propertyName=" + propertyName + ", selfName=" + selfName + ", operator=" + operator + "]";
	}
}
