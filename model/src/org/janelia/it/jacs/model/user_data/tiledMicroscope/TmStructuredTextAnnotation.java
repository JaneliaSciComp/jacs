package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.IOException;
import java.io.Serializable;

/**
 * this class represents a structured text annotation to be attached to other tiled
 * microscope things, most typically TmGeoAnnotations
 *
 * djo, 10/14
 *
 */
public class TmStructuredTextAnnotation implements IsSerializable, Serializable {
    Long id;

    // parent could be a neuron, workspace, or geometric annotation, and we record
    //  what it is
    Long parentId;
    int parentType;

    public final static int GEOMETRIC_ANNOTATION = 1;
    public final static int NEURON = 2;
    public final static int WORKSPACE = 3;

    // data will be stored in some kind of structured text, initially json;
    //  we record a version number that we will update when we change the format
    public static final int FORMAT_VERSION = 1;
    String dataString;

	/** No-args c'tor required for use with Protostuff/protobuf */
	public TmStructuredTextAnnotation() {		
	}

    public TmStructuredTextAnnotation(Long id, Long parentId, int parentType,
       String dataString) {
        this.id = id;
        this.parentId = parentId;
        this.parentType = parentType;
        this.dataString = dataString;
    }

    /**
     * retrieve data, parsed; if we can't parse the stored string, return an empty object node instead
     */
    public JsonNode getData() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(getDataString());
        }
        catch (IOException e) {
            e.printStackTrace();
            return mapper.createObjectNode();
        }
    }

    /**
     * update the data string in the annotation with data from a new JSON object; doesn't update
     * value on error
     */
    public void setData(JsonNode node) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValueAsString(node);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public int getParentType() {
        return parentType;
    }

    public static int getFormatVersion() {
        return FORMAT_VERSION;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getDataString() {
        return dataString;
    }

    public void setDataString(String dataString) {
        this.dataString = dataString;
    }
}

