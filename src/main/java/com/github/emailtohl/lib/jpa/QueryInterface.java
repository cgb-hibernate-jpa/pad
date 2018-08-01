package com.github.emailtohl.lib.jpa;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 动态查询接口
 * 
 * @author HeLei
 * @param <E>
 *            实体类型
 * @param <ID>
 *            实体的ID类型
 */
public interface QueryInterface<E, ID extends Serializable> extends EntityInterface<E, ID> {
	/**
	 * 根据参数对象分页查询
	 * 
	 * @param example
	 *            参数对象
	 * @param pageable
	 *            分页排序信息
	 * @return 结果列表
	 */
	Page<E> queryForPage(E example, Pageable pageable);

	/**
	 * 根据参数对象查询列表
	 * 
	 * @param example
	 *            参数对象
	 * @return 结果列表
	 */
	List<E> queryForList(E example);
}
