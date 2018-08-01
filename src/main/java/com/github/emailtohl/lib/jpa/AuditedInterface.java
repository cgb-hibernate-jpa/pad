package com.github.emailtohl.lib.jpa;

import java.io.Serializable;
import java.util.List;

import com.github.emailtohl.lib.jpa.AuditedRepository.Tuple;

/**
 * 查询实体对象历史修订版的接口
 * 
 * @author HeLei
 *
 * @param <E>
 *            实体类型
 * @param <ID>
 *            实体的ID类型
 */
public interface AuditedInterface<E, ID extends Serializable> extends SearchInterface<E, ID> {

	/**
	 * 查询某个实体的历次修订版本
	 * 
	 * @param id
	 *            实体对象的id
	 * @return 在Tuple#defaultRevisionEntity中获取到修订id
	 */
	List<Tuple<E>> getRevisions(ID id);

	/**
	 * 查询某个实体在某个修订版时的历史记录
	 * 
	 * @param id
	 *            实体的id
	 * @param revision
	 *            版本号
	 * @return 某修订版时候的记录
	 */
	E getEntityAtRevision(ID id, Number revision);

	/**
	 * 将实体回滚到某历史版本上
	 * 
	 * @param id
	 *            实体的id
	 * @param revision
	 *            修订号
	 */
	void rollback(ID id, Number revision);
}
