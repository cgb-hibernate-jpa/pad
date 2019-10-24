package com.github.emailtohl.pad.model;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.emailtohl.pad.jpa.EntityBase;

@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler", "fieldHandler" })
@Audited
@Indexed
@Entity
@Table(name = "PARTICIPATOR")
public class Participator extends EntityBase {
	private static final long serialVersionUID = 3386469327429384582L;

    @NotNull
    protected String name;

    @NotNull
    protected boolean activated;

    protected Address homeAddress;
    
    protected Set<String> loginNames = new LinkedHashSet<>();

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

	public boolean isActivated() {
		return activated;
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

    @ElementCollection
	public Set<String> getLoginNames() {
		return loginNames;
	}

	public void setLoginNames(Set<String> loginNames) {
		this.loginNames = loginNames;
	}

    // ...
	
	@Override
	public Participator clone() {
		Participator cp = (Participator) super.clone();
		if (homeAddress != null) {
			cp.homeAddress = homeAddress.clone();
		}
		cp.loginNames.addAll(loginNames);
		return cp;
	}
}