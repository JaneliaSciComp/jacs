package org.janelia.it.jacs.model.status;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Supports formatting of error messages for XML web services.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlType(name="", propOrder={"summary", "details"})
public class RestfulWebServiceFailure {

    private String summary;
    private String details;

    @SuppressWarnings("UnusedDeclaration")
    private RestfulWebServiceFailure() {
    }

    public RestfulWebServiceFailure(String summary,
                                    Exception causedBy) {
        this.summary = summary;
        if (causedBy != null) {
            this.details = causedBy.getMessage();
        }
    }

    @XmlElement
    public String getSummary() {
        return summary;
    }

    @XmlElement
    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "RestfulWebServiceFailure{" +
                "details='" + details + '\'' +
                ", summary='" + summary + '\'' +
                '}';
    }
}
