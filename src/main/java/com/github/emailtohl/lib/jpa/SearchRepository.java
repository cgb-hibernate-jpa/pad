package com.github.emailtohl.lib.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.exception.EmptyQueryException;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.github.emailtohl.lib.exception.InnerDataStateException;

/**
 * 搜索的公共类
 * 
 * @author HeLei
 *
 * @param <E>
 *            实体类型
 * @param <ID>
 *            实体的ID类型
 */
public abstract class SearchRepository<E, ID extends Serializable> extends QueryRepository<E, ID> implements SearchInterface<E, ID> {
	private static volatile boolean IS_INIT = false;
	protected final String[] onFields;

	public SearchRepository(Class<E> entityClass, Class<ID> idClass, String... onFields) {
		super(entityClass, idClass);
		this.onFields = onFields;
	}
	
	public SearchRepository(Class<E> entityClass, Class<ID> idClass) {
		super(entityClass, idClass);
		this.onFields = new Closure().getOnfields();
	}

	public SearchRepository() {
		super();
		this.onFields = new Closure().getOnfields();
	}

	/**
	 * 初始化索引域
	 */
	private class Closure {
		private final Set<Class<?>> used = new HashSet<Class<?>>();
		private final Set<String> fields = new HashSet<String>();

		String[] getOnfields() {
			setFields(entityClass, fields, "");
			return fields.toArray(new String[fields.size()]);
		}

		private void setFields(Class<?> clazz, Set<String> fields, String parentPath) {
			if (used.contains(clazz)) {
				return;
			}
			used.add(clazz);
			for (EntityProperty prop : getEntityProperties(clazz)) {
				// @IndexedEmbedded指定在主业务实体的索引中包含关联业务实体的搜索内容，可以通过搜索关联业务实体的内容得到主业务实体的查询结果。
				// @ContainedIn指定更新关联实体时同时更新主业务实体中索引的内容，如果不指定@ContainedIn会导致关联实体内容修改后得到错误的搜索结果。
				// @IndexedEmbedded和@ContainedIn可以同时出现在一个属性上，意味着其关联的业务实体对应的属性上也应当同时出现这两个注解。
				IndexedEmbedded indexedEmbeddedAnno = prop.getAnnotation(IndexedEmbedded.class);
				Field fieldAnno = prop.getAnnotation(Field.class);
				if (indexedEmbeddedAnno != null) {
					// IndexedEmbedded既可以注解在@ManyToOne这样的实体属性上，也可以注解在@OneToMany这样的集合属性上
					Class<?> embclz = prop.getType();
					if (Collection.class.isAssignableFrom(embclz)) {// 如果是集合属性的情况
						embclz = indexedEmbeddedAnno.targetElement();
						if (void.class.equals(embclz)) {// 如果没有指定目标类，那就分析该泛型类
							Class<?>[] genericClasses = prop.getGenericClass();
							if (genericClasses.length != 1) {
								throw new IllegalArgumentException(String.format(
										"The entity %s type of the collection is unknown" + embclz.getSimpleName()));
							} else {
								embclz = genericClasses[0];
							}
						}
					}
					setFields(embclz, fields, parentPath.isEmpty() ? prop.name : parentPath + '.' + prop.name);
				} else if (fieldAnno != null) {
					String field = (parentPath.isEmpty() ? "" : parentPath + '.')
							+ (fieldAnno.name().isEmpty() ? prop.name : fieldAnno.name());
					fields.add(field);
				}
			}
		}

	}

	protected FullTextQuery getLuceneQuery(String query) {
		FullTextEntityManager fem = Search.getFullTextEntityManager(entityManager);
		if (!IS_INIT) {
			synchronized (entityClass) {
				if (!IS_INIT) {
					try {
						fem.createIndexer().startAndWait();
						IS_INIT = true;
					} catch (InterruptedException e) {
						LOG.error(entityClass + "init failed", e);
						throw new InnerDataStateException(e);
					}
				}
			}
		}
		QueryBuilder builder = fem.getSearchFactory().buildQueryBuilder().forEntity(entityClass).get();
		Query lucene = builder.keyword().onFields(onFields).matching(query).createQuery();
		return fem.createFullTextQuery(lucene, entityClass);
	}

	/**
	 * 分页查询与query关键字相关的实体对象
	 * @param query 字符串关键字
	 * @param pageable 分页排序对象
	 * @return 查询结果
	 */
	public Page<E> search(String query, Pageable pageable) {
		FullTextQuery ftq;
		try {
			ftq = getLuceneQuery(query);
		} catch (EmptyQueryException e) {
			LOG.catching(e);
			return new PageImpl<>(new ArrayList<>());
		}
		int total = ftq.getResultSize();
		ftq.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
		@SuppressWarnings("unchecked")
		List<E> ls = ftq.getResultList();
		return new PageImpl<>(ls, pageable, total);
	}

	/**
	 * 查询与query关键字相关的实体对象列表
	 * @param query 字符串关键字
	 * @return 结果集合
	 */
	@SuppressWarnings("unchecked")
	public List<E> search(String query) {
		return getLuceneQuery(query).getResultList();
	}

}
