package org.janelia.it.jacs.model.annotation;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 3/2/11
 * Time: 3:48 PM
 *
 */
public class Annotation {

    private long uniqueIdentifier;
    private String namespace;
    private String owner;
    private String term;
    private String comment;
    private String conditional; // relates to
    private String value;
    private String source;
    private Date createdDate;
    private String parentIdentifier;
    private boolean deprecated;

    public Annotation(){}

    public Annotation(long uniqueIdentifier, String owner, String namespace, String term, String value, String comment,
                      String conditional, String source, Date createdDate, String parentIdentifier,
                      boolean deprecated) {
        this.uniqueIdentifier = uniqueIdentifier;
        this.namespace = namespace;
        this.owner = owner;
        this.term = term;
        this.comment = comment;
        this.conditional = conditional;
        this.value = value;
        this.source = source;
        this.createdDate = createdDate;
        this.parentIdentifier = parentIdentifier;
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

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getConditional() {
        return conditional;
    }

    public void setConditional(String conditional) {
        this.conditional = conditional;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getParentIdentifier() {
        return parentIdentifier;
    }

    public void setParentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public String toString() {
        return "Annotation{" +
                "uniqueIdentifier=" + uniqueIdentifier +
                ", namespace='" + namespace + '\'' +
                ", owner='" + owner + '\'' +
                ", term='" + term + '\'' +
                ", comment='" + comment + '\'' +
                ", conditional='" + conditional + '\'' +
                ", value='" + value + '\'' +
                ", source='" + source + '\'' +
                ", createdDate=" + createdDate +
                ", parentIdentifier='" + parentIdentifier + '\'' +
                ", deprecated=" + deprecated +
                '}';
    }
}
