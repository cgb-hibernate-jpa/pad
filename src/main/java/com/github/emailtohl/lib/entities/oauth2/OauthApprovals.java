package com.github.emailtohl.lib.entities.oauth2;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "oauth_approvals")
public class OauthApprovals {
	private Id id = new Id();
	private String scope;
	private String status;
	private Date expiresAt;
	private Date lastModifiedAt;

	@EmbeddedId
	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@org.hibernate.search.annotations.DateBridge(resolution = org.hibernate.search.annotations.Resolution.DAY)
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "expiresAt")
	public Date getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Date expiresAt) {
		this.expiresAt = expiresAt;
	}

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@org.hibernate.search.annotations.DateBridge(resolution = org.hibernate.search.annotations.Resolution.DAY)
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "lastModifiedAt")
	public Date getLastModifiedAt() {
		return lastModifiedAt;
	}

	public void setLastModifiedAt(Date lastModifiedAt) {
		this.lastModifiedAt = lastModifiedAt;
	}
	
	@Embeddable
	public static class Id implements Serializable {
		private static final long serialVersionUID = 1439199517471398711L;
		private String userId;
		private String clientId;

		public Id() {
		}

		public Id(String userId, String clientId) {
			this.userId = userId;
			this.clientId = clientId;
		}

		@Column(name = "userId")
		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		@Column(name = "clientId")
		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, clientId);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof Id) {
				Id that = (Id) obj;
				return userId.equals(that.getUserId()) && clientId.equals(that.getClientId());
			}
			return false;
		}

	}

}

