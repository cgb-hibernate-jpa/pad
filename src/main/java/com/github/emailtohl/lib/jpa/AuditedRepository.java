package com.github.emailtohl.lib.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

/**
 * 获取实体历史信息 需要为实体及其关联类添加上org.hibernate.envers.Audited注解
 * 
 * @author HeLei
 * @param <E>
 *            实体类型
 * @param <ID>
 *            实体的ID类型
 */
public abstract class AuditedRepository<E, ID extends Serializable> extends SearchRepository<E, ID>
		implements AuditedInterface<E, ID> {

	public AuditedRepository() {
	}

	public AuditedRepository(Class<E> entityClass, Class<ID> idClass, String[] onFields) {
		super(entityClass, idClass, onFields);
	}

	/**
	 * 查询某个实体的历次修订版本
	 * 
	 * @param id
	 *            实体对象的id
	 * @return 在Tuple#defaultRevisionEntity中获取到修订id
	 */
	@SuppressWarnings("unchecked")
	public List<Tuple<E>> getRevisions(ID id) {
		AuditReader auditReader = AuditReaderFactory.get(entityManager);
		AuditQuery query = auditReader.createQuery().forRevisionsOfEntity(entityClass, false, true);
		query.add(AuditEntity.id().eq(id));
		List<Object[]> result = query.getResultList();
		List<Tuple<E>> ls = new ArrayList<Tuple<E>>();
		for (Object[] o : result) {
			Tuple<E> tuple = new Tuple<E>((E) o[0]/* 在版本时的详情 */,
					(DefaultRevisionEntity) o[1]/* 版本详情：(id = 201, revisionDate = 2017-2-10 21:17:40) */,
					(RevisionType) o[2])/* 增(ADD)、改(MOD)、删(DEL) */;
			ls.add(tuple);
		}
		return ls;
	}

	/**
	 * 查询某个实体在某个修订版时的历史记录
	 * 
	 * @param id
	 *            实体的id
	 * @param revision
	 *            版本号
	 * @return 某修订版时候的记录
	 */
	public E getEntityAtRevision(ID id, Number revision) {
		AuditReader auditReader = AuditReaderFactory.get(entityManager);
		return auditReader.find(entityClass, id, revision);
	}

	/**
	 * 将实体回滚到某历史版本上
	 * 
	 * @param id
	 *            实体的id
	 * @param revision
	 *            修订号
	 */
	public void rollback(ID id, Number revision) {
		AuditReader auditReader = AuditReaderFactory.get(entityManager);
		E bygone = auditReader.find(entityClass, id, revision);
		entityManager.unwrap(Session.class).replicate(bygone, ReplicationMode.IGNORE);
	}

	/**
	 * 将实体历史版本的信息进行封装，包含历史版本的快照，修订版的版本号，修改的类型等
	 * 
	 * @param <E>
	 *            实体类
	 */
	public static class Tuple<E> {
		public final E entity;
		public final DefaultRevisionEntity defaultRevisionEntity;
		public final RevisionType revisionType;

		public Tuple(E entity, DefaultRevisionEntity defaultRevisionEntity, RevisionType revisionType) {
			this.entity = entity;
			this.defaultRevisionEntity = defaultRevisionEntity;
			this.revisionType = revisionType;
		}
	}
}
