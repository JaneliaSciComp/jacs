
package org.janelia.it.jacs.model.vo;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 25, 2006
 * Time: 4:15:31 PM
 */
public abstract class ParameterVO implements Serializable, IsSerializable {
    private Long objectId;

    /**
     * required for GWT
     */
    public ParameterVO() {
    }

    public abstract boolean isValid();

    public Long getObjectId() {
        return objectId;
    }

    public abstract String getType();

    public abstract String getStringValue();

    public String toString() {
        return "ParameterVO{" +
                ", objectId=" + objectId +
                '}';
    }
}