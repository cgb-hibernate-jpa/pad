package com.github.emailtohl.lib.event;

import com.github.emailtohl.lib.jpa.EntityBase;
/**
 * 修改实体事件
 * @author HeLei
 */
public class UpdateEntityEvent extends EventBase {
	private static final long serialVersionUID = 5764149071960848774L;
	public final EntityBase entity;

	public UpdateEntityEvent(EntityBase entity) {
		this.entity = entity;
	}
}