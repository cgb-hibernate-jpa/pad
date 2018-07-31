package com.github.emailtohl.lib.event;

import com.github.emailtohl.lib.jpa.BaseEntity;
/**
 * 修改实体事件
 * @author HeLei
 */
public class UpdateEntityEvent extends BaseEvent {
	private static final long serialVersionUID = 5764149071960848774L;
	public final BaseEntity entity;

	public UpdateEntityEvent(BaseEntity entity) {
		this.entity = entity;
	}
}