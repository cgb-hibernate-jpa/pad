package com.github.emailtohl.pad.entities.session;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "SPRING_SESSION_ATTRIBUTES")
public class SpringSessionAttributes {
	private Id id = new Id();
	private byte[] attributeBytes;
	
	@EmbeddedId
	public Id getId() {
		return id;
	}
	public void setId(Id id) {
		this.id = id;
	}
	
	@Type(type = "org.hibernate.type.ImageType")
	@Column(name = "ATTRIBUTE_BYTES")
	public byte[] getAttributeBytes() {
		return attributeBytes;
	}
	public void setAttributeBytes(byte[] attributeBytes) {
		this.attributeBytes = attributeBytes;
	}
	
	@Embeddable
	public static class Id implements Serializable {
		private static final long serialVersionUID = 8965376857051223666L;
		private SpringSession springSession;
		private String attributeName;
		
		@JsonBackReference
		@ManyToOne(optional =  false)
		@JoinColumn(name = "SESSION_PRIMARY_ID")
		public SpringSession getSpringSession() {
			return springSession;
		}
		public void setSpringSession(SpringSession springSession) {
			this.springSession = springSession;
		}
		
		@Column(name = "ATTRIBUTE_NAME")
		public String getAttributeName() {
			return attributeName;
		}
		public void setAttributeName(String attributeName) {
			this.attributeName = attributeName;
		}

		@Override
		public int hashCode() {
			return Objects.hash(springSession, attributeName);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Id) {
				Id that = (Id) obj;
				return springSession.equals(that.getSpringSession()) && attributeName.equals(that.getAttributeName());
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "Id [springSession=" + springSession + ", attributeName=" + attributeName + "]";
		}

	}
}
