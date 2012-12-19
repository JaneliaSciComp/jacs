package org.janelia.it.jacs.model.entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Specific wrapper for data set entity that identifies valid attributes,
 * exposing them as JAXB elements.
 *
 * @author Eric Trautman
 */
@XmlRootElement
public class DataSet {

    private Entity entity;

    // required for JAXB
    @SuppressWarnings("UnusedDeclaration")
    private DataSet() {
    }

    public DataSet(Entity entity) {
        this.entity = entity;
    }

    @XmlElement
    public String getName() {
        return entity.getName();
    }

    @XmlElement
    public String getDataSetIdentifier() {
        return entity.getValueByAttributeName(
                EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
    }

    @XmlElement
    public String getSageSync() {
        return entity.getValueByAttributeName(
                EntityConstants.ATTRIBUTE_SAGE_SYNC);
    }

    public boolean hasSageSync() {
        return (getSageSync() != null);
    }

    @XmlElement
    public String getUser() {
        return entity.getOwnerKey();
    }

}
