package com.github.emailtohl.lib.jpa;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

/**
 * 基本JPA仓库
 * 
 * @author HeLei
 * @param <E> 实体类型
 * @param <ID> 实体的ID类型
 */
@Repository
public abstract class EntityRepository<E, ID extends Serializable> implements EntityInterface<E, ID> {
	protected final Logger LOG = LogManager.getLogger(getClass());
	protected final Class<E> entityClass;
	protected final Class<ID> idClass;
	@PersistenceUnit
	protected EntityManagerFactory entityManagerFactory;
	@PersistenceContext
	protected EntityManager entityManager;

	protected EntityRepository(Class<E> entityClass, Class<ID> idClass) {
		this.entityClass = entityClass;
		this.idClass = idClass;
	}

	@SuppressWarnings("unchecked")
	protected EntityRepository() {
		Class<?>[] classes = new Class[2];
		Class<?> tempClass = null;
		Class<?> clz = this.getClass();
		while (clz != EntityRepository.class) {
			Type genericSuperclass = clz.getGenericSuperclass();
			if (genericSuperclass instanceof ParameterizedType) {
				ParameterizedType type = (ParameterizedType) genericSuperclass;
				Type[] arguments = type.getActualTypeArguments();
				if (arguments == null) {
					continue;
				}
				if (arguments.length == 1 && arguments[0] instanceof Class) {// 若继承层次上分开声明参数类型时arguments.length就为1
					tempClass = (Class<?>) arguments[0];
				} else if (arguments.length == 2) {// 只有当参数类型有两个时，才能确定idClass和entityClass分别是哪个
					if (arguments[0] instanceof Class) {
						classes[0] = (Class<?>) arguments[0];
					}
					if (arguments[1] instanceof Class) {
						classes[1] = (Class<?>) arguments[1];
					}
				}
				if (classes[0] != null && classes[1] == null) {
					classes[1] = tempClass;
				} else if (classes[0] == null && classes[1] != null) {
					classes[0] = tempClass;
				}
				if (classes[0] != null && classes[1] != null) {
					break;
				}
			}
			clz = clz.getSuperclass();
		}
		entityClass = (Class<E>) classes[0];
		idClass = (Class<ID>) classes[1];
		if (entityClass == null) {
			String cause = "The type of entity is unknown";
			LOG.debug(cause);
			throw new IllegalStateException(cause);
		}
		if (idClass == null) {
			String cause = "The type of id is unknown";
			LOG.debug(cause);
			throw new IllegalStateException(cause);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("The type of entity is {}", entityClass.getSimpleName());
			LOG.debug("The type of id is {}", idClass.getSimpleName());
		}
	}
	
	/**
	 * 获取实体
	 * 
	 * @param primaryKey 主键
	 * @return 该主键的实体对象
	 */
	public E find(ID primaryKey) {
		return entityManager.find(entityClass, primaryKey);
	}
	
	/**
	 * 保存实体，修改实体则是通过find找到它并修改它即可
	 * 
	 * @param entity 实体对象
	 */
	public void persist(E entity) {
		entityManager.persist(entity);
	}
	
	/**
	 * 删除实体
	 * 
	 * @param entity 实体对象
	 */
	public void remove(E entity) {
		entityManager.remove(entity);
	}
}
