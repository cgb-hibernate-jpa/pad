package com.github.emailtohl.lib.event;

import com.github.emailtohl.lib.jpa.BaseEntity;
/**
 * 创建实体事件
 * @author HeLei
 */
public class CreateEntityEvent extends BaseEvent {
	private static final long serialVersionUID = 4477445467998909245L;
	public final BaseEntity entity;

	public CreateEntityEvent(BaseEntity entity) {
		this.entity = entity;
	}
}
