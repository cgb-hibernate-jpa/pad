package com.github.emailtohl.lib.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.emailtohl.lib.exception.InnerDataStateException;

@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" })
@Indexed
@Embeddable
public class Image implements Serializable, Cloneable {
	private static final long serialVersionUID = 7558966940624793980L;

	@NotNull
    @Column(nullable = false)
    protected String name;

    @NotNull
    @Column(nullable = false)
    protected String filename;

    @NotNull
    protected int width;

    @NotNull
    protected int height;

    public Image() {
    }

    public Image(String name, String filename, int width, int height) {
        this.name = name;
        this.filename = filename;
        this.width = width;
        this.height = height;
    }

    @Field
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    // Whenever value-types are managed in collections, overriding equals/hashCode is a good idea!

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Image image = (Image) o;

        if (width != image.width) return false;
        if (height != image.height) return false;
        if (!filename.equals(image.filename)) return false;
        if (!name.equals(image.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + filename.hashCode();
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }
    // ...
    
    @Override
    public Image clone() {
    	try {
			return (Image) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InnerDataStateException(e);
		}
    }
}
