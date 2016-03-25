package org.janelia.it.jacs.compute.wsrest.mouselight;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.util.ISO8601Utils;

/**
 * Created by murphys on 3/24/2016.
 */

@JsonInclude(Include.NON_NULL)
public class WorkspaceRestResponse {

    public WorkspaceRestResponse() {
    }

    @JsonProperty
    public String getMessage() {
        return "this is a test message";
    }

}
