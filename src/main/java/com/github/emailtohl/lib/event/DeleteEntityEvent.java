package com.github.emailtohl.lib.event;

import com.github.emailtohl.lib.jpa.BaseEntity;
/**
 * 删除实体事件
 * @author HeLei
 */
public class DeleteEntityEvent extends EventBase {
	private static final long serialVersionUID = -634816873583838517L;
	public final BaseEntity entity;

	public DeleteEntityEvent(BaseEntity entity) {
		this.entity = entity;
	}
}