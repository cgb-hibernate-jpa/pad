package com.github.emailtohl.lib.model;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.validation.constraints.NotNull;

import com.github.emailtohl.lib.jpa.EntityBase;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class BillingDetails extends EntityBase {
	private static final long serialVersionUID = 8115915240285958633L;

    @NotNull
    protected String owner;

    // ...

    protected BillingDetails() {
    }

    protected BillingDetails(String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
