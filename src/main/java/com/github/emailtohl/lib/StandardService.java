package com.github.emailtohl.lib;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Timestamp;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Pageable;

import com.github.emailtohl.lib.exception.InnerDataStateException;
import com.github.emailtohl.lib.exception.NotAcceptableException;
import com.github.emailtohl.lib.jpa.Paging;

/**
 * 抽象的服务，主要就是增删改查功能。
 * 
 * 标准化参数名、参数类型以及返回后，不仅利于维护，更利于在切面层进行扩展。
 * 校验依赖javax.el-api、el-impl、hibernate-validator
 * 
 * @author HeLei
 *
 * @param <E> 实体的类型
 * @param <ID> 实体ID的类型
 */
public abstract class StandardService<E, ID extends Serializable> {
	/**
	 * 以字符串方式存储着当前唯一识别用户的信息，例如可以用“:”作为分隔符： id:name:authorities
	 */
	public static final ThreadLocal<String> CURRENT_USER_INFO = new ThreadLocal<String>();
	/**
	 * 手动校验
	 */
	protected static final Validator VALIDATOR;
	/**
	 * 日志
	 */
	protected final Logger LOG = LogManager.getLogger(getClass());

	static {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		VALIDATOR = factory.getValidator();
	}

	/**
	 * 创建一个实体
	 * 
	 * @param entity 实体对象
	 * @return 保存好ID的实体对象
	 */
	public abstract E create(E entity);

	/**
	 * 根据ID获取实体
	 * 若未找到可抛出java.util.NoSuchElementException或com.github.emailtohl.lib.exception.NotFoundException
	 * 若有冲突，可抛出com.github.emailtohl.lib.exception.ConflictException
	 * 
	 * @param id 实体的ID
	 * @return 实体对象
	 */
	public abstract E read(ID id);

	/**
	 * 分页查询
	 * 
	 * @param example 查询参数
	 * @param pageable 分页排序
	 * @return Paging封装的分页信息，一般JPA底层返回的是Page对象，但该对象不利于JSON等序列化。
	 *         所以在将持久化状态的实体转瞬态时，同时改变分页对象
	 */
	public abstract Paging<E> query(E example, Pageable pageable);

	/**
	 * 查询列表
	 * 
	 * @param example 查询参数
	 * @return 结果列表
	 */
	public abstract List<E> query(E example);

	/**
	 * 修改实体内容，并指明哪些属性忽略
	 * 若未找到可抛出java.util.NoSuchElementException或com.github.emailtohl.lib.exception.NotFoundException
	 * 
	 * @param id 实体ID
	 * @param newEntity 修改的实体对象
	 * @return 返回null表示没找到该实体
	 */
	public abstract E update(ID id, E newEntity);

	/**
	 * 根据ID删除实体
	 * 
	 * @param id 实体的id
	 */
	public abstract void delete(ID id);

	/**
	 * 屏蔽实体中的敏感信息，如密码；将持久化状态的实体转存到瞬时态的实体对象上以便于调用者序列化 本方法提取简略信息，不做关联查询，主要用于列表中
	 * 
	 * @param entity 持久化状态的实体对象
	 * @return 瞬时态的实体对象
	 */
	protected abstract E toTransient(E entity);

	/**
	 * 屏蔽实体中的敏感信息，如密码；将持久化状态的实体转存到瞬时态的实体对象上以便于调用者序列化 本方法提取详细信息，做关联查询
	 * 
	 * @param entity 持久化状态的实体对象
	 * @return 瞬时态的实体对象
	 */
	protected abstract E transientDetail(E entity);

	/**
	 * 手动校验对象是否符合约束条件
	 * 
	 * @param entity 被校验的实体对象
	 */
	public void validate(E entity) {
		Set<ConstraintViolation<E>> violations = VALIDATOR.validate(entity);
		if (violations.size() > 0) {
			violations.forEach(v -> LOG.debug(v));
			throw new NotAcceptableException(new ConstraintViolationException(violations));
		}
	}

	/**
	 * 判断字符串是否存在文本 
	 * <p>hasText(null) = false</p>
	 * <p>hasText("") = false</p>
	 * <p>hasText(" ") = false</p>
	 * <p>hasText("12345") = true</p>
	 * <p>hasText(" 12345 ") = true</p>
	 * 
	 * @param text 被判断的字符串文本
	 * @return 字符串是否为null或空字符串
	 */
	public boolean hasText(String text) {
		if (text == null) {
			return false;
		}
		if (text.isEmpty()) {
			return false;
		}
		int strLen = text.length();
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(text.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 对JavaBean的字符串属性进行裁剪，主要用于提交表单的情况
	 * 
	 * @param o 传入参数对象
	 * @return 因为字符串的不变性（形参不会被方法内部修改），所以需要返回修改后的值
	 * 若是字符串，则返回裁剪空白后的字符串，否则原样返回
	 */
	public Object trimStringProperty(Object o) {
		class Runner {
			// 跟踪是否使用过，防止循环引用
			Set<Object> used = new HashSet<>();
			Object exec(Object o) {
				if (o == null) {
					return o;
				}
				if (used.contains(o)) {
					return o;
				}
				used.add(o);
				if (o instanceof String) {
					return ((String) o).trim();
				}
				if (isValueTypeInstance(o)) {
					return o;
				}
				if (o instanceof Collection) {
					@SuppressWarnings("unchecked")
					Collection<Object> c = (Collection<Object>) o;
					List<Object> temp = c.stream().map(this::exec).collect(Collectors.toList());
					c.clear();
					temp.forEach(i -> {
						c.add(i);
					});
					return o;
				}
				if (o instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<Object, Object> m = (Map<Object, Object>) o;
					m = m.entrySet().stream().map(e -> {
						e.setValue(exec(e.getValue()));
						return e;
					}).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
					return o;
				}
				try {
					for (PropertyDescriptor pd : Introspector.getBeanInfo(o.getClass(), Object.class)
							.getPropertyDescriptors()) {
						Method getter = pd.getReadMethod(), setter = pd.getWriteMethod();
						if (getter == null || setter == null) {
							continue;
						}
						getter.setAccessible(true);
						Object value = getter.invoke(o, new Object[] {});
						if (value == null) {
							continue;
						}
						setter.setAccessible(true);
						setter.invoke(o, exec(value));
					}
				} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e1) {
					LOG.catching(e1);
				}
				return o;
			}
		}
		return new Runner().exec(o);
	}
	
	/**
	 * 对JavaBean的字符串属性补齐右通配符，主要用于查询时
	 * 
	 * @param o 传入参数对象
	 * @return 因为字符串的不变性（形参不会被方法内部修改），所以需要返回修改后的值
	 * 若是字符串，则返回补全通配符后的字符串，否则原样返回
	 */
	public Object wildcardStringProperty(Object o) {
		class Runner {
			// 跟踪是否使用过，防止循环引用
			Set<Object> used = new HashSet<>();
			Object exec(Object o) {
				if (o == null) {
					return o;
				}
				if (used.contains(o)) {
					return o;
				}
				used.add(o);
				if (o instanceof String && hasText((String) o)) {
					return ((String) o).trim() + '%';
				}
				if (isValueTypeInstance(o)) {
					return o;
				}
				if (o instanceof Collection) {
					@SuppressWarnings("unchecked")
					Collection<Object> c = (Collection<Object>) o;
					List<Object> temp = c.stream().map(this::exec).collect(Collectors.toList());
					c.clear();
					temp.forEach(i -> {
						c.add(i);
					});
					return o;
				}
				if (o instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<Object, Object> m = (Map<Object, Object>) o;
					m = m.entrySet().stream().map(e -> {
						e.setValue(exec(e.getValue()));
						return e;
					}).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
					return o;
				}
				try {
					for (PropertyDescriptor pd : Introspector.getBeanInfo(o.getClass(), Object.class)
							.getPropertyDescriptors()) {
						Method getter = pd.getReadMethod(), setter = pd.getWriteMethod();
						if (getter == null || setter == null) {
							continue;
						}
						getter.setAccessible(true);
						Object value = getter.invoke(o, new Object[] {});
						if (value == null) {
							continue;
						}
						setter.setAccessible(true);
						setter.invoke(o, exec(value));
					}
				} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e1) {
					LOG.catching(e1);
				}
				return o;
			}
		}
		return new Runner().exec(o);
	}
	
	/**
	 * 利用Java自身序列化机制克隆一个对象
	 * @param <T> 被克隆的类型，该类型要实现Serializable
	 * @param o 被克隆的对象
	 * @return 原对象的克隆
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Serializable> T clone(T o) {
		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			out = new ObjectOutputStream(bout);
			out.writeObject(o);
			in = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
			return (T) in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			LOG.warn("The clone method execution failed", e);
			throw new InnerDataStateException(e);
		} finally {
			if (out != null) {
				try {
					out.close();
					out = null;
				} catch (IOException e) {
					LOG.catching(e);
				}
			}
			if (out != null) {
				try {
					out.close();
					out = null;
				} catch (IOException e) {
					LOG.catching(e);
				}
			}
		}
	}
	
	/**
	 * 判断对象是否值类型，若实例为null，返回false
	 * 
	 * @param o 被判断的类型实例
	 * @return 值对象返回true，否则返回false
	 */
	public boolean isValueTypeInstance(Object o) {
		return o instanceof String || o instanceof Number || o instanceof Enum || o instanceof Character
				|| o instanceof Boolean || o instanceof Date || o instanceof Calendar || o instanceof Timestamp
				|| o instanceof Temporal || o instanceof TimeZone || o instanceof TemporalAmount;
	}
}
