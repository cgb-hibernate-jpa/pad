package com.github.emailtohl.lib.jpa;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.persistence.AccessType;

import com.github.emailtohl.lib.exception.InnerDataStateException;

/**
 * 实体属性的结构
 * 
 * @author HeLei
 */
class EntityProperty {
	/**
	 * 属性的名字
	 */
	final String name;
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
	 * @param propertyDescriptor JavaBean的属性描述器
	 * @param field 类的属性字段
	 * @throws IllegalArgumentException 如果没有getter方法，则抛出异常
	 */
	EntityProperty(PropertyDescriptor propertyDescriptor, Field field) {
		if (propertyDescriptor != null) {
			this.getter = propertyDescriptor.getReadMethod();
			if (this.getter == null) {
				throw new IllegalArgumentException("There's no getter method in " + propertyDescriptor.getName());
			}
			this.propertyDescriptor = propertyDescriptor;
			this.name = propertyDescriptor.getName();
			this.field = null;
			this.getter.setAccessible(true);
			this.accessType = AccessType.PROPERTY;
		} else if (field != null) {
			field.setAccessible(true);
			this.field = field;
			this.name = field.getName();
			this.propertyDescriptor = null;
			this.getter = null;
			this.accessType = AccessType.FIELD;
		} else {
			throw new IllegalArgumentException("PropertyDescriptor and field cannot all be null");
		}
	}

	/**
	 * 获取该属性的值
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
	 * 获取该属性的泛型类
	 * @return 泛型类的实例
	 */
	Class<?>[] getGenericClass() {
		if (AccessType.PROPERTY == accessType) {
			return EntityInspector.getGenericClass(propertyDescriptor);
		} else {
			assert AccessType.FIELD == accessType;
			return EntityInspector.getGenericClass(field);
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		EntityProperty other = (EntityProperty) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}
}
