package org.janelia.it.jacs.model.entity.json;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.janelia.it.jacs.model.domain.sample.DataSet;

/**
 * Specific wrapper for data set entity that identifies valid attributes,
 * exposing them as JAXB elements.
 *
 * @author Eric Trautman
 */
@XmlRootElement
public class JsonDataSet {

    private DataSet dataSet;

    // required for JAXB
    @SuppressWarnings("UnusedDeclaration")
    private JsonDataSet() {
    }

    public JsonDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    @XmlElement
    public String getName() {
        return dataSet.getName();
    }

    @XmlElement
    public String getDataSetIdentifier() {
        return dataSet.getIdentifier();
    }

    @XmlElement
    public String getSageSync() {
        return dataSet.isSageSync() ? "SAGE Sync" : "";
    }

    public boolean hasSageSync() {
        return (getSageSync() != null);
    }

    @XmlElement
    public String getUser() {
        return dataSet.getOwnerKey();
    }

}
