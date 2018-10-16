package com.github.emailtohl.lib.event;

import com.github.emailtohl.lib.jpa.EntityBase;
/**
 * 删除实体事件
 * @author HeLei
 */
public class DeleteEntityEvent extends BaseEvent {
	private static final long serialVersionUID = -634816873583838517L;
	public final EntityBase entity;

	public DeleteEntityEvent(EntityBase entity) {
		this.entity = entity;
	}
}