package org.janelia.it.jacs.compute.wsrest.process;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by goinac on 10/8/15.
 */
@XmlRootElement(name = "serviceMessage")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceMessage {
    private String message;

    public ServiceMessage() {
    }

    public ServiceMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
