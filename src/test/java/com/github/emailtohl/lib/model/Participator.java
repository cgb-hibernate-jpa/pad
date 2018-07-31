package com.github.emailtohl.lib.model;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

import com.github.emailtohl.lib.jpa.BaseEntity;

@Audited
@Indexed
@Entity
@Table(name = "PARTICIPATOR")
public class Participator extends BaseEntity {
	private static final long serialVersionUID = 3386469327429384582L;

    @NotNull
    protected String name;

    @NotNull
    protected boolean activated;

    protected Address homeAddress;

    public Participator() {
    }

    public Participator(String name) {
        this.name = name;
    }

    @Field
    public String getName() {
        return name;
    }

    public void setName(String name) {
		this.name = name;
	}

	public void setActivated(boolean activated) {
        this.activated = activated;
    }

	@IndexedEmbedded(depth = 1)
    public Address getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(Address homeAddress) {
        this.homeAddress = homeAddress;
    }

    // ...
}