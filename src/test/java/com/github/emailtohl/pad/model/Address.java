package com.github.emailtohl.pad.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

import com.github.emailtohl.pad.exception.InnerDataStateException;

@Embeddable
public class Address implements Serializable, Cloneable {
	private static final long serialVersionUID = -3505117115993155305L;

	@NotNull
    protected String street;

    @NotNull
    protected String zipcode;

    @NotNull
    protected String city;

    public Address() {
    }

    public Address(String street, String zipcode, String city) {
        this.street = street;
        this.zipcode = zipcode;
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @Column(length = 5)
    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
    
    @Override
    public Address clone() {
    	try {
			return (Address) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InnerDataStateException(e);
		}
    }
}