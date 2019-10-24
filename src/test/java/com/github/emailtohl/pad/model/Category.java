package com.github.emailtohl.pad.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.emailtohl.pad.entities.SelfRef;
import com.github.emailtohl.pad.jpa.EntityBase;

@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" })
@Audited
@Indexed
@Entity
public class Category extends EntityBase implements SelfRef {
	private static final long serialVersionUID = 8972855909524370689L;

    @NotNull
    protected String name;

    // The root of the tree has no parent, column has to be nullable!
    protected Category parent;

    protected Set<Item> items = new HashSet<Item>();

    public Category() {
    }

    public Category(String name) {
        this.name = name;
    }

    public Category(String name, Category parent) {
        this.name = name;
        this.parent = parent;
    }

    @Field
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @ManyToOne
    @JoinColumn(
        name = "PARENT_ID",
        foreignKey = @ForeignKey(name = "FK_CATEGORY_PARENT_ID")
    )
    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    @ContainedIn
    @ManyToMany(targetEntity = Item.class, cascade = CascadeType.PERSIST)
    @JoinTable(name = "CATEGORY_ITEM",
       joinColumns = @JoinColumn(
           name = "CATEGORY_ID",
           foreignKey = @ForeignKey(name = "FK_CATEGORY_ITEM_CATEGORY_ID")
       ),
       inverseJoinColumns = @JoinColumn(
           name = "ITEM_ID",
           foreignKey = @ForeignKey(name = "FK_CATEGORY_ITEM_ITEM_ID")
       ))
    public Set<Item> getItems() {
        return items;
    }

    public void setItems(Set<Item> items) {
        this.items = items;
    }

    // ...
    @Override
    public Category clone() {
    	Category cp = (Category) super.clone();
    	if (parent != null) {
    		cp.parent = parent.clone();
    	}
    	// 不再返回items，避免相互回调
    	return cp;
    }
}
