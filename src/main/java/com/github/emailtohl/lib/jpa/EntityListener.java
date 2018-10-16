package com.github.emailtohl.lib.jpa;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.github.emailtohl.lib.event.CreateEntityEvent;
import com.github.emailtohl.lib.event.DeleteEntityEvent;
import com.github.emailtohl.lib.event.UpdateEntityEvent;

/**
 * 创建日期、修改日期处理
 * 
 * @author HeLei
 */
@Component
public class EntityListener {
	private static final Logger LOG = LogManager.getLogger();
	private static ApplicationEventPublisher EVENT_PUBLISHER;
	@Autowired
	private volatile ApplicationEventPublisher publisher;
	
	@PostConstruct
	public void init() {
		EVENT_PUBLISHER = publisher;
	}

	/**
	 * 保存前处理
	 * 
	 * @param entity 基类
	 */
	@PrePersist
	public void prePersist(BaseEntity entity) {
		entity.setCreateDate(new Date());
		entity.setModifyDate(new Date());
	}

	/**
	 * 更新前处理
	 * 
	 * @param entity 基类
	 */
	@PreUpdate
	public void preUpdate(BaseEntity entity) {
		entity.setModifyDate(new Date());
	}

	@PostLoad
	void readTrigger(BaseEntity entity) {
		LOG.debug("entity read.");
	}

	@PostPersist
	void afterInsertTrigger(BaseEntity entity) {
		LOG.debug("entity inserted into database.");
		if (EVENT_PUBLISHER != null) {
			CreateEntityEvent event = new CreateEntityEvent(entity);
			EVENT_PUBLISHER.publishEvent(event);
		}
	}

	@PostUpdate
	void afterUpdateTrigger(BaseEntity entity) {
		LOG.debug("entity just updated in the database.");
		if (EVENT_PUBLISHER != null) {
			UpdateEntityEvent event = new UpdateEntityEvent(entity);
			EVENT_PUBLISHER.publishEvent(event);
		}
	}

	@PreRemove
	void beforeDeleteTrigger(BaseEntity entity) {
		LOG.debug("entity about to be deleted.");
	}

	@PostRemove
	void afterDeleteTrigger(BaseEntity entity) {
		LOG.debug("entity about deleted from database.");
		if (EVENT_PUBLISHER != null) {
			DeleteEntityEvent event = new DeleteEntityEvent(entity);
			EVENT_PUBLISHER.publishEvent(event);
		}
	}
	
}
