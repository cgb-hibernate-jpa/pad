package com.github.emailtohl.pad.entities.oauth2;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "oauth_refresh_token")
public class OauthRefreshToken {
	private String token_id;
	private byte[] token;
	private byte[] authentication;

	@Id
	@Column(name = "token_id")
	public String getToken_id() {
		return token_id;
	}

	public void setToken_id(String token_id) {
		this.token_id = token_id;
	}

	@Type(type = "org.hibernate.type.ImageType")
	@Column(name = "token")
	public byte[] getToken() {
		return token;
	}

	public void setToken(byte[] token) {
		this.token = token;
	}

	@Type(type = "org.hibernate.type.ImageType")
	@Column(name = "authentication")
	public byte[] getAuthentication() {
		return authentication;
	}

	public void setAuthentication(byte[] authentication) {
		this.authentication = authentication;
	}

}
