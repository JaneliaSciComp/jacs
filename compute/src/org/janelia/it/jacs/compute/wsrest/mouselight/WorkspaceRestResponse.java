package org.janelia.it.jacs.compute.wsrest.mouselight;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by murphys on 3/24/2016.
 */

@XmlRootElement
public class WorkspaceRestResponse {

    @SuppressWarnings("UnusedDeclaration")
    public WorkspaceRestResponse() {
    }

    @XmlElement
    public String getMessage() {
        return "this is a test message";
    }

}
