package com.github.emailtohl.lib.jpa;

import java.util.Date;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;

import com.github.emailtohl.lib.event.CreateEntityEvent;
import com.github.emailtohl.lib.event.DeleteEntityEvent;
import com.github.emailtohl.lib.event.UpdateEntityEvent;

/**
 * 本类一旦被引入实体类的@EntityListeners注解中，将会被JPA引擎自动创建
 * 若将Spring的ApplicationEventPublisher传入本类，则可将实体CRUD事件发布到Spring上下文中
 * @author HeLei
 */
public class EntityListener {
	private static final Logger LOG = LogManager.getLogger();
	private static ApplicationEventPublisher Event_Publisher;
	
	public EntityListener() {}
	
	public EntityListener(ApplicationEventPublisher publisher) {
		Event_Publisher = publisher;
	}

	/**
	 * 保存前处理
	 * 
	 * @param entity 基类
	 */
	@PrePersist
	public void prePersist(EntityBase entity) {
		entity.setCreateTime(new Date());
		entity.setModifyTime(new Date());
	}

	/**
	 * 更新前处理
	 * 
	 * @param entity 基类
	 */
	@PreUpdate
	public void preUpdate(EntityBase entity) {
		entity.setModifyTime(new Date());
	}

	@PostLoad
	void readTrigger(EntityBase entity) {
		LOG.debug("entity read.");
	}

	@PostPersist
	void afterInsertTrigger(EntityBase entity) {
		LOG.debug("entity inserted into database.");
		if (Event_Publisher != null) {
			CreateEntityEvent event = new CreateEntityEvent(entity);
			Event_Publisher.publishEvent(event);
		}
	}

	@PostUpdate
	void afterUpdateTrigger(EntityBase entity) {
		LOG.debug("entity just updated in the database.");
		if (Event_Publisher != null) {
			UpdateEntityEvent event = new UpdateEntityEvent(entity);
			Event_Publisher.publishEvent(event);
		}
	}

	@PreRemove
	void beforeDeleteTrigger(EntityBase entity) {
		LOG.debug("entity about to be deleted.");
	}

	@PostRemove
	void afterDeleteTrigger(EntityBase entity) {
		LOG.debug("entity about deleted from database.");
		if (Event_Publisher != null) {
			DeleteEntityEvent event = new DeleteEntityEvent(entity);
			Event_Publisher.publishEvent(event);
		}
	}
	
}
