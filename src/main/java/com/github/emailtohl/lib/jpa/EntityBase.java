package com.github.emailtohl.lib.jpa;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.emailtohl.lib.exception.InnerDataStateException;

/**
 * Entity 基类
 * <b>注意：依赖本基类的equals和hashCode方法会使你的实体对象在瞬时状态（没有id）时不能正确地存入集合（如HashSet）中</b>
 * 
 * @author HeLei
 */
// 忽略JPA/Hibernate懒加载属性
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "handler", "fieldHandler" })
// 再对象图中防止循环依赖
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "_id")
@EntityListeners(EntityListener.class)
// @MappedSuperclass 用于被继承的在基类上面，让所有继承它的实体类都具备同样的属性
@MappedSuperclass
public abstract class EntityBase implements Serializable, Cloneable {
	private static final long serialVersionUID = -411374988586534072L;
	private static final ObjectMapper OMAPPER = new ObjectMapper();
	private static final ConcurrentHashMap<Class<? extends EntityBase>, Field[]> FIELDS_CACHE = new ConcurrentHashMap<Class<? extends EntityBase>, Field[]>();
	protected static final Logger LOG = LogManager.getLogger();
	/**
	 * "ID"属性名称
	 */
	public static final String ID_PROPERTY_NAME = "id";

	/**
	 * "创建时间"属性名称
	 */
	public static final String CREATE_TIME_PROPERTY_NAME = "createTime";

	/**
	 * "修改时间"属性名称
	 */
	public static final String MODIFY_TIME_PROPERTY_NAME = "modifyTime";
	
	/**
	 * "并发控制的版本号"属性名称
	 */
	public static final String VERSION_PROPERTY_NAME = "version";
	
	public static final String[] PROPERTY_NAMES = { ID_PROPERTY_NAME, CREATE_TIME_PROPERTY_NAME,
			MODIFY_TIME_PROPERTY_NAME, VERSION_PROPERTY_NAME };

	/**
	 * id
	 * 此字段使用java.lang.Long类型,可以用null表示实体未存储时缺失值的状态
	 */
	protected Long id;

	/**
	 * 创建时间
	 */
	protected Date createTime;
	/**
	 * 修改时间
	 */
	protected Date modifyTime;
	
	/**
	 * 本字段存在的意义在于并发修改同一记录时，抛出OptimisticLockException异常提醒用户，使用的乐观锁并发控制策略
	 * 假如获取本实例时，version = 0， 在提交事务时，JPA提供程序会执行如下语句
	 * 
	 * update item set name = ?, version = 1 where id = ? and version = 0
	 * 若jdbc返回0，要么item不存在，要么不再有版本0，此时会抛javax.persistence.OptimisticLockException异常
	 * 需捕获此异常给用户适当提示。
	 */
	protected Integer version;
	
	/**
	 * 获取ID
	 * @return ID
	 */
	@org.hibernate.search.annotations.DocumentId// 全文索引id，可选，默认是@Id
	@Id
	// MySQL/SQLServer: @GeneratedValue(strategy = GenerationType.AUTO)
	// Oracle: @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequenceGenerator")
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}
	/**
	 * 设置ID
	 * @param id ID
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * 获取创建时间
	 * @return 创建时间
	 */
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	// 将日期类型转为string，直到秒级
	@org.hibernate.search.annotations.DateBridge(resolution = org.hibernate.search.annotations.Resolution.SECOND)
	@Column(nullable = false, updatable = false, name = "create_time")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreateTime() {
		return createTime;
	}
	/**
	 * 设置创建时间
	 * @param createTime 创建时间
	 */
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	/**
	 * 获取修改时间
	 * @return 修改时间
	 */
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	// 将日期类型转为string，直到秒级
	@org.hibernate.search.annotations.DateBridge(resolution = org.hibernate.search.annotations.Resolution.SECOND)
	@Column(nullable = false, name = "modify_time")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getModifyTime() {
		return modifyTime;
	}
	/**
	 * 设置修改时间
	 * @param modifyTime 修改时间
	 */
	public void setModifyTime(Date modifyTime) {
		this.modifyTime = modifyTime;
	}
	
	@Version
	protected Integer getVersion() {
		return version;
	}
	protected void setVersion(Integer version) {
		this.version = version;
	}

	/**
	 * 重写hashCode方法
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/**
	 * 重写equals方法
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Class<?> thisClass = getClass(), otherClass = obj.getClass();
		// 两者都不在同一继承结构上，包括JPA提供程序生成的代理
		// 由于this是EntityBase的实例，所以这种判断涵盖other instanceof EntityBase
		if (!thisClass.isAssignableFrom(otherClass) && !otherClass.isAssignableFrom(thisClass))
			return false;
		EntityBase other = (EntityBase) obj;
		if (id == null || other.getId() == null) {// 注意此处不能直接访问other的字段：other.id，因为other可能是JPA提供程序生成的代理
			return false;
		} else {
			return id.equals(other.getId());
		}
	}
	
	@Override
	public String toString() {
		try {
            return OMAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
        	return String.format("{\"id\":%d}", id);
        }
	}
	
	/**
	 * 创建一个克隆对象，仅复制值类型的属性
	 * 值类型，即字符串、数字、布尔、枚举、日期等
	 * 这些类型的值作为最基本的属性，一般具有不变性，可复制，可映射为数据库字段
	 */
	@Override
	public EntityBase clone() {
		Class<? extends EntityBase> clz = this.getClass();
		EntityBase cp;
		try {
			cp = clz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			LOG.warn("The clone method execution on EntityBase failed", e);
			throw new InnerDataStateException(e);
		}
		cp.id = id;
		cp.createTime = createTime;
		cp.modifyTime = modifyTime;
		cp.version = version;
		for (Field f : getValueTypeFields(clz)) {
			try {
				EntityInspector.injectField(f, cp, f.get(this));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOG.catching(e);
			}
		}
		return cp;
	}
	
	/**
	 * 获取值类型的Fields
	 * @param clz EntityBase的子类
	 * @return 值类型的Fields
	 */
	@SuppressWarnings("unchecked")
	private Field[] getValueTypeFields(Class<? extends EntityBase> clazz) {
		Field[] fields = FIELDS_CACHE.get(clazz);
		if (fields != null) {
			return fields;
		}
		synchronized (FIELDS_CACHE) {
			fields = FIELDS_CACHE.get(clazz);
			if (fields != null) {
				return fields;
			}
			List<Field> arrFields = new ArrayList<Field>();
			Class<? extends EntityBase> clz = clazz;
			while (!clz.equals(EntityBase.class)) {
				for (Field f : clz.getDeclaredFields()) {
					int modifiers = f.getModifiers();
					// isStrict 内部类连接外围类的引用
					if (Modifier.isStatic(modifiers) || Modifier.isStrict(modifiers) || Modifier.isFinal(modifiers)) {
						continue;
					}
					if (EntityInspector.isValueType(f.getType())) {
						f.setAccessible(true);
						arrFields.add(f);
					}
				}
				clz = (Class<? extends EntityBase>) clz.getSuperclass();
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("{} 's value type fields is:", clazz.getName());
				for (Field f : arrFields) {
					LOG.debug("type: {}, field name: {}", f.getType().getName(), f.getName());
				}
			}
			fields = arrFields.toArray(new Field[arrFields.size()]);
			FIELDS_CACHE.put(clazz, fields);
		}
		return fields;
	}
	
	/**
	 * Spring 的BeanUtils.copyProperties方法在复制时需要指明忽略什么属性
	 * 而本类在实体复制时往往需要忽略id，creationTime，modifyTime，version的属性，因为他们是提供给JPA提供程序使用
	 * @param other 忽略的属性列表
	 * @return 包括基类中需要忽略的所有属性
	 */
	public static String[] getIgnoreProperties(String... other) {
		List<String> ls = new ArrayList<String>();
		for (String t : PROPERTY_NAMES) {
			ls.add(t);
		}
		for (String t : other) {
			ls.add(t);
		}
		return ls.toArray(new String[ls.size()]);
	}
	
}
