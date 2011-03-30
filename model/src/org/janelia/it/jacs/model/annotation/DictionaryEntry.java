package org.janelia.it.jacs.model.annotation;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 3/2/11
 * Time: 4:01 PM
 *
 */
public class DictionaryEntry {
    private long uniqueIdentifier;
    private String namespace;
    private String owner;
    private String shorthand;
    private String longhand;
    private String description;
    private Date createdDate;
    private boolean deprecated;

    public DictionaryEntry(long uniqueIdentifier, String namespace, String owner, String shorthand, String longhand, String description, Date createdDate, boolean deprecated) {
        this.uniqueIdentifier = uniqueIdentifier;
        this.namespace = namespace;
        this.owner = owner;
        this.shorthand = shorthand;
        this.longhand = longhand;
        this.description = description;
        this.createdDate = createdDate;
        this.deprecated = deprecated;
    }

    public long getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    public void setUniqueIdentifier(long uniqueIdentifier) {
        this.uniqueIdentifier = uniqueIdentifier;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getShorthand() {
        return shorthand;
    }

    public void setShorthand(String shorthand) {
        this.shorthand = shorthand;
    }

    public String getLonghand() {
        return longhand;
    }

    public void setLonghand(String longhand) {
        this.longhand = longhand;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }
}
