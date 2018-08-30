package com.github.emailtohl.lib.entities.session;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.github.emailtohl.lib.jpa.InitialValueAsCondition;

@Entity
@Table(name = "SPRING_SESSION")
public class SpringSession {
	private String primaryId;
	private String sessionId;
	private long creationTime;
	private long lastAccessTime;
	private int maxInactiveInterval;
	private long expiryTime;
	private String principalName;
	private Set<SpringSessionAttributes> springSessionAttributes = new HashSet<>();
	
	@Id
	@Column(name = "PRIMARY_ID")
	public String getPrimaryId() {
		return primaryId;
	}
	public void setPrimaryId(String primaryId) {
		this.primaryId = primaryId;
	}

	@Column(name = "SESSION_ID", nullable = true)
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	@Column(name = "CREATION_TIME", nullable = true)
	public long getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}
	
	@Column(name = "LAST_ACCESS_TIME", nullable = true)
	public long getLastAccessTime() {
		return lastAccessTime;
	}
	public void setLastAccessTime(long lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}
	
	@InitialValueAsCondition
	@Column(name = "MAX_INACTIVE_INTERVAL", nullable = true)
	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}
	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}
	
	@Column(name = "EXPIRY_TIME", nullable = true)
	public long getExpiryTime() {
		return expiryTime;
	}
	public void setExpiryTime(long expiryTime) {
		this.expiryTime = expiryTime;
	}
	
	@Column(name = "PRINCIPAL_NAME", nullable = true)
	public String getPrincipalName() {
		return principalName;
	}
	public void setPrincipalName(String principalName) {
		this.principalName = principalName;
	}
	
	@OneToMany(mappedBy = "id.springSession", cascade = { CascadeType.REMOVE }, orphanRemoval = true)
	@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
	public Set<SpringSessionAttributes> getSpringSessionAttributes() {
		return springSessionAttributes;
	}
	public void setSpringSessionAttributes(Set<SpringSessionAttributes> springSessionAttributes) {
		this.springSessionAttributes = springSessionAttributes;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((primaryId == null) ? 0 : primaryId.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpringSession other = (SpringSession) obj;
		if (primaryId == null) {
			if (other.getPrimaryId() != null)
				return false;
		} else if (!primaryId.equals(other.getPrimaryId()))
			return false;
		return true;
	}
	
}
