package com.github.emailtohl.lib.model;

import java.math.BigDecimal;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.emailtohl.lib.jpa.EntityBase;

@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" })
@Audited
@Indexed
@Entity
@Access(AccessType.PROPERTY)
public class Bid extends EntityBase {
	private static final long serialVersionUID = -7815293333915832861L;
	
	protected String name;

	@NotNull
    protected Item item;

    protected Participator bidder;

    @NotNull
    protected BigDecimal amount;

    public Bid() {
    }

    public Bid(String name, Item item, Participator bidder, BigDecimal amount) {
    	this.name = name;
        this.item = item;
        this.amount = amount;
        this.bidder = bidder;
    }

    @Field
    @Column(nullable = true, unique = true)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@IndexedEmbedded(depth = 1)
	@ContainedIn
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_BID_ITEM_ID"))
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    @IndexedEmbedded(depth = 1)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_BID_BIDDER_ID"))
    public Participator getBidder() {
        return bidder;
    }

    public void setBidder(Participator bidder) {
        this.bidder = bidder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}