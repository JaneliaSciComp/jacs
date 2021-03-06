package org.janelia.model.jacs2.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;

/**
 * A reference to a DomainObject in a specific collection.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Reference {
    @JsonIgnore
    private String targetClassname;
    @JsonIgnore
    private Number targetId;

    @JsonCreator
    public static Reference createFor(String strRef) {
        Preconditions.checkArgument(StringUtils.isNotBlank(strRef));
        String[] s = strRef.split("#");
        String className = s[0];
        Number id = new BigInteger(s[1]);
        return new Reference(className, id);
    }

    public static Reference createFor(Class<?> targetClass, Number targetId) {
        return new Reference(targetClass.getSimpleName(), targetId);
    }

    public static Reference createFor(DomainObject domainObject) {
        Preconditions.checkArgument(domainObject != null);
        return new Reference(domainObject.getClass().getSimpleName(), domainObject.getId());
    }

    public Reference() {
    }

    public Reference(String targetClassname, Number targetId) {
        this.targetClassname = targetClassname;
        this.targetId = targetId;
    }

    public String getTargetClassname() {
        return targetClassname;
    }

    public void setTargetClassname(String targetClassname) {
        this.targetClassname = targetClassname;
    }

    public Number getTargetId() {
        return targetId;
    }

    public void setTargetId(Number targetId) {
        this.targetId = targetId;
    }

    @JsonIgnore
    public String getTargetRefId() {
        return targetClassname + "#" + targetId;
    }

    @JsonValue
    @Override
    public String toString() {
        return getTargetRefId();
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}
