package com.github.emailtohl.lib.jpa;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 全文搜索接口
 * 
 * @author HeLei
 * @param <E>
 *            实体类型
 * @param <ID>
 *            实体的ID类型
 */
public interface SearchInterface<E, ID extends Serializable> extends QueryInterface<E, ID> {
	/**
	 * 分页查询与query关键字相关的实体对象
	 * 
	 * @param query
	 *            字符串关键字
	 * @param pageable
	 *            分页排序对象
	 * @return 查询结果
	 */
	Page<E> search(String query, Pageable pageable);

	/**
	 * 查询与query关键字相关的实体对象列表
	 * 
	 * @param query
	 *            字符串关键字
	 * @return 结果集合
	 */
	List<E> search(String query);
}
