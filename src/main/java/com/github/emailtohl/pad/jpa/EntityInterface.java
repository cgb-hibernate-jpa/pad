package com.github.emailtohl.pad.jpa;

import java.io.Serializable;

/**
 * 基本的JPA接口
 * 
 * @author HeLei
 * @param <E> 实体类型
 * @param <ID> 实体的ID类型
 */
public interface EntityInterface<E, ID extends Serializable> {
	/**
	 * 获取实体
	 * 
	 * @param primaryKey 主键
	 * @return 该主键的实体对象
	 */
	E find(ID primaryKey);

	/**
	 * 保存实体，修改实体则是通过find找到它并修改它即可
	 * 
	 * @param entity 实体对象
	 */
	void persist(E entity);

	/**
	 * 删除实体
	 * 
	 * @param entity 实体对象
	 */
	void remove(E entity);
}
