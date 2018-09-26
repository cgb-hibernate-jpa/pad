package com.github.emailtohl.lib.jpa;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.emailtohl.lib.exception.InnerDataStateException;

/**
 * 实体分析工具
 * 
 * @author HeLei
 */
class EntityInspector {
	private static final Logger LOG = LogManager.getLogger();
	private static final Pattern GENERIC_PATTERN = Pattern.compile("<(.+)>");

	/**
	 * 判断该类是否实体
	 * 
	 * @return 判断的结果
	 */
	static boolean isEntity(Class<?> clazz) {
		Class<?> clz = clazz;
		while (clz != Object.class) {
			Embeddable embeddableAnno = clz.getAnnotation(Embeddable.class);
			Entity entityAnno = clz.getAnnotation(Entity.class);
			if (embeddableAnno != null || entityAnno != null) {
				return true;
			}
			clz = clz.getSuperclass();
		}
		return false;
	}

	/**
	 * 从JavaBean属性描述器中获取注解
	 * 
	 * @param descriptor JavaBean属性描述器
	 * @param annotationClass 注解的class
	 * @return
	 */
	static <A extends Annotation> A getAnnotation(PropertyDescriptor descriptor, Class<A> annotationClass) {
		Method read = descriptor.getReadMethod(), write = descriptor.getWriteMethod();
		LOG.debug("read: {} write: {}", read, write);
		A a = null;
		if (read != null) {
			a = read.getAnnotation(annotationClass);
		}
		if (a == null && write != null) {
			a = write.getAnnotation(annotationClass);
		}
		return a;
	}

	/**
	 * 对注解了@Entity的类进行分析，确定其访问的规则，默认返回PROPERTY，这不仅是JPA默认规则，而且与BaseEntity规则一致
	 * 
	 * @param entityClass 注解了@Entity的类
	 * @return 访问规则：FIELD还是PROPERTY
	 */
	static AccessType getAccessType(Class<?> entityClass) {
		Access accessAnno = entityClass.getAnnotation(Access.class);
		if (accessAnno != null) {
			return accessAnno.value();
		}
		LOG.debug("No @Access annotation was found and the AccessType was judged by @Id or @Embeddedid");
		// 查看@Id标准在访问器方法上还是字段上
		try {
			for (PropertyDescriptor pd : Introspector.getBeanInfo(entityClass, entityClass.getSuperclass())
					.getPropertyDescriptors()) {
				if (getAnnotation(pd, Id.class) != null || getAnnotation(pd, EmbeddedId.class) != null) {
					return AccessType.PROPERTY;
				}
			}
		} catch (IntrospectionException e) {
			LOG.catching(e);
			throw new InnerDataStateException(e);
		}
		LOG.debug("No @Id or @Embeddedid was found on the JavaBean property");
		Class<?> clz = entityClass;
		while (clz != Object.class) {
			Field[] fields = clz.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				Field field = fields[i];
				if (field.getAnnotation(Id.class) != null || field.getAnnotation(EmbeddedId.class) != null) {
					return AccessType.FIELD;
				}
			}
			clz = clz.getSuperclass();
		}
		LOG.debug("No @Id or @Embeddedid was found on the field property, returns the default AccessType.PROPERTY");
		return AccessType.PROPERTY;
	}

	/**
	 * 获取特殊属性的比较方式，以JavaBean属性优先
	 * @param clazz 被标注了@Instruction，需要分析的类
	 * @return 特殊属性的比较方式，key实体的属性名，value是操作符
	 */
	static Set<Condition> getConditions(Class<?> clazz) {
		Set<Condition> conditions = new HashSet<Condition>();
		Class<?> clz = clazz;
		while (clz != Object.class) {
			Field[] fields = clz.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				Field field = fields[i];
				Instruction anno = field.getAnnotation(Instruction.class);
				if (anno != null) {
					Condition condition = new Condition(null, field);
					// 导出类的属性要覆盖基类属性
					if (!conditions.contains(condition)) {
						conditions.add(condition);
					} else {
						LOG.debug(condition.selfName + " is exist ignore this");
					}
				}
			}
			clz = clz.getSuperclass();
		}
		try {
			for (PropertyDescriptor pd : Introspector.getBeanInfo(clazz, Object.class).getPropertyDescriptors()) {
				Instruction anno = getAnnotation(pd, Instruction.class);
				if (anno != null) {
					Condition condition = new Condition(pd, null);
					if (LOG.isDebugEnabled()) {
						if (conditions.contains(condition)) {
							LOG.debug(condition.selfName + " is exist but cover this");
						}
					}
					// PropertyDescriptor的属性优先于字段
					conditions.add(condition);
				}
			}
		} catch (IntrospectionException e) {
			LOG.catching(e);
			throw new InnerDataStateException(e);
		}
		return conditions;
	}

	/**
	 * 查找实体的边界范围。因为继承关系，有两种情况需要考虑： 1. 一个未标注@Entity的类，它继承于标注了@Entity的类。 2.
	 * 标注了@Entity的类，但它的父类存在既不含@Entity也不含@MappedSuperclass的类
	 * 所以对于那些不能映射到数据库表字段的属性，均需要排除。
	 * 
	 * @param clazz 被标注了Entity、Embeddable的实体类
	 * @return 一个entityClass，stopClass 2个元素的数组，在继承上为上闭下开，在baseclass上停止分析
	 */
	static Class<?>[] findEntityBound(Class<?> clazz) {
		Class<?> clz = clazz, entityClass = null, stopClass = null;
		ArrayList<Class<?>> ls = new ArrayList<Class<?>>();
		while (clz != Object.class) {
			ls.add(clz);
			clz = clz.getSuperclass();
		}
		// 寻找上界
		Embeddable embeddableAnno = null;
		Entity entityAnno = null;
		// 指定为ArrayList，对数组操作
		for (int i = 0; i < ls.size(); i++) {
			clz = ls.get(i);
			embeddableAnno = clz.getAnnotation(Embeddable.class);
			entityAnno = clz.getAnnotation(Entity.class);
			if (embeddableAnno != null || entityAnno != null) {
				entityClass = clz;
				break;
			}
		}
		// 寻找下届
		MappedSuperclass mappedSuperclassAnno = null;
		for (int i = ls.size() - 1; i >= 0; i--) {
			clz = ls.get(i);
			mappedSuperclassAnno = clz.getAnnotation(MappedSuperclass.class);
			entityAnno = clz.getAnnotation(Entity.class);
			embeddableAnno = clz.getAnnotation(Embeddable.class);
			if (mappedSuperclassAnno != null || entityAnno != null || embeddableAnno != null) {
				stopClass = clz.getSuperclass();
				break;
			}
		}
		if (entityClass == null || stopClass == null) {
			throw new IllegalArgumentException(clazz.getName() + " is not entity class");
		}
		LOG.debug("entityClass is {} and stopClass is {}", entityClass.getSimpleName(), stopClass.getSimpleName());
		return new Class<?>[] { entityClass, stopClass };
	}

	/**
	 * 通过分析字段获取属性
	 * 
	 * @param clazz 被标注了Entity、Embeddable的实体类
	 * @return 属性集合
	 */
	static <T> Set<EntityProperty> getEntityPropertyByField(Class<T> clazz) {
		Class<?>[] bound = findEntityBound(clazz);
		Class<?> entityClass = bound[0], stopClass = bound[1];
		Set<EntityProperty> properties = new HashSet<EntityProperty>();
		while (!entityClass.equals(stopClass)) {
			Field[] fields = entityClass.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				Field field = fields[i];
				int modifiers = field.getModifiers();
				// isStrict 内部类连接外围类的引用
				if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || Modifier.isAbstract(modifiers)
						|| Modifier.isNative(modifiers) || Modifier.isStrict(modifiers)
						|| field.getAnnotation(Transient.class) != null) {
					continue;
				}
				EntityProperty property = new EntityProperty(null, field);
				// 扩展类的属性将覆盖基类的属性
				if (properties.contains(property)) {
					LOG.debug("{} is exist ignore this", property.name);
					continue;
				}
				properties.add(property);
			}
			entityClass = entityClass.getSuperclass();
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("{} has the following properties {}", clazz.getSimpleName(), properties);
		}
		return properties;
	}

	/**
	 * 根据JPA定义，实体类的属性包括非private字段以及JavaBean定义的属性
	 * 
	 * @param clazz 被标注了Entity、Embeddable的实体类
	 * @return 属性集合
	 */
	static <T> Set<EntityProperty> getEntityPropertyByJpaDefinition(Class<T> clazz) {
		Class<?>[] bound = findEntityBound(clazz);
		Class<?> entityClass = bound[0], stopClass = bound[1];
		Set<EntityProperty> properties = new HashSet<EntityProperty>();
		// 先查找公开属性
		Field[] entityFields = entityClass.getFields();
		Set<Field> notEntityFields = new HashSet<Field>(Arrays.asList(stopClass.getFields()));
		for (Field entityField : entityFields) {
			if (notEntityFields.contains(entityField)) {
				continue;
			}
			int modifiers = entityField.getModifiers();
			// isStrict 内部类连接外围类的引用
			if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || Modifier.isStrict(modifiers)
					|| Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)
					|| entityField.getAnnotation(Transient.class) != null) {
				continue;
			}
			EntityProperty entityProperty = new EntityProperty(null, entityField);
			if (properties.contains(entityProperty)) {
				continue;
			}
			properties.add(entityProperty);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("The public fields of {} are as follows {}", clazz.getSimpleName(), properties);
		}
		// 再查找JavaBean定义的属性
		entityClass = bound[0];
		try {
			for (PropertyDescriptor pd : Introspector.getBeanInfo(entityClass, stopClass).getPropertyDescriptors()) {
				if (pd.getReadMethod() == null || getAnnotation(pd, Transient.class) != null) {
					continue;
				}
				properties.add(new EntityProperty(pd, null));
			}
		} catch (IntrospectionException e) {
			LOG.catching(e);
			throw new InnerDataStateException(e);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("{}'s public fields and JavaBean properties are as follows {}", clazz.getSimpleName(),
					properties);
		}
		return properties;
	}

	/**
	 * 分析出实体类中的属性 注意：若是继承于某个实体类的类，则仍然只返回实体类的属性
	 * 
	 * @param clazz 被标注了Entity、Embeddable的实体类
	 * @return 作为映射关系部分的属性
	 */
	static <T> Set<EntityProperty> getEntityProperty(Class<T> clazz) {
		Set<EntityProperty> properties = new HashSet<EntityProperty>();
		Class<?>[] bound = findEntityBound(clazz);
		Class<?> entityClass = bound[0];
		if (AccessType.FIELD == getAccessType(entityClass)) {
			properties = getEntityPropertyByField(clazz);
		} else {
			properties = getEntityPropertyByJpaDefinition(clazz);
		}
		return properties;
	}

	/**
	 * 从属性描述器中获取泛型的实际Class，例如某JavaBean有属性Set<String> roles，则返回{String.class}
	 * 
	 * @param p JavaBean属性描述器
	 * @return 该属性的泛型类
	 */
	static Class<?>[] getGenericClass(PropertyDescriptor p) {
		List<Class<?>> ls = new ArrayList<Class<?>>();
		Method method = p.getReadMethod();
		if (method == null) {
			method = p.getWriteMethod();
		}
		ElementCollection elementCollection = method.getAnnotation(ElementCollection.class);
		OneToMany oneToMany = method.getAnnotation(OneToMany.class);
		ManyToMany manyToMany = method.getAnnotation(ManyToMany.class);
		Class<?> targetClass = getTargetClass(elementCollection, oneToMany, manyToMany);
		if (!void.class.equals(targetClass)) {
			return new Class<?>[] {targetClass};
		}
		String genericString = method.toGenericString();
		LOG.debug("genericString: {}", genericString);
		Matcher m = GENERIC_PATTERN.matcher(genericString);
		if (m.find()) {
			for (String className : m.group(1).split(",")) {
				try {
					ls.add(Class.forName(className.trim()));
				} catch (ClassNotFoundException e) {
					LOG.warn("ClassNotFoundException", e);
				}
			}
		}
		Class<?>[] cs = new Class<?>[ls.size()];
		return ls.toArray(cs);
	}

	/**
	 * 从Field字段中获取泛型的实际Class，例如某Field是Set<String> roles，则返回{String.class}
	 * 
	 * @param f Field字段
	 * @return 该字段的泛型类
	 */
	static Class<?>[] getGenericClass(Field f) {
		ElementCollection elementCollection = f.getAnnotation(ElementCollection.class);
		OneToMany oneToMany = f.getAnnotation(OneToMany.class);
		ManyToMany manyToMany = f.getAnnotation(ManyToMany.class);
		Class<?> targetClass = getTargetClass(elementCollection, oneToMany, manyToMany);
		if (!void.class.equals(targetClass)) {
			return new Class<?>[] {targetClass};
		}
		List<Class<?>> ls = new ArrayList<Class<?>>();
		String genericString = f.getGenericType().toString();
		LOG.debug("genericString: {}", genericString);
		Matcher m = GENERIC_PATTERN.matcher(genericString);
		if (m.find()) {
			for (String className : m.group(1).split(",")) {
				try {
					ls.add(Class.forName(className.trim()));
				} catch (ClassNotFoundException e) {
					LOG.warn("ClassNotFoundException", e);
				}
			}
		}
		Class<?>[] cs = new Class<?>[ls.size()];
		return ls.toArray(cs);
	}

	/**
	 * 返回关联关系的类型
	 * 
	 * @param elementCollection 嵌入式集合关系，其实例无独立生命周期，主关系被删除后，会被级联删除
	 * @param oneToMany 一对多关系，其实例具有独立的生命周期
	 * @param manyToMany 多对多关系，其实例具有独立生命周期
	 * @return 关联关系的类型
	 */
	static private Class<?> getTargetClass(ElementCollection elementCollection, OneToMany oneToMany, ManyToMany manyToMany) {
		if (elementCollection != null) {
			return elementCollection.targetClass();
		}
		if (oneToMany != null) {
			return oneToMany.targetEntity();
		}
		if (manyToMany != null) {
			return manyToMany.targetEntity();
		}
		return void.class;
	}
}
