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


    public TmStructuredTextAnnotation(Long id, Long parentId, int parentType,
       String dataString) {
        this.id = id;
        this.parentId = parentId;
        this.parentType = parentType;
        this.dataString = dataString;
    }

    public TmStructuredTextAnnotation(String annString) throws Exception {
        // expect: id:parentid:parenttype:formatversion:datastring
        // note that datastring will hold colons as well (it's JSON), so stop the split at 5
        String[] items = annString.split(":", 5);
        if (items.length < 5) {
            throw new Exception("could not parse annotation string " + annString);
        }

        id = new Long(items[0]);
        parentId = new Long(items[1]);

        // I'm not fond of this, but fiddling with an enum seemed overboard
        parentType = Integer.parseInt(items[2]);
        if (parentType < 1 || parentType > 3) {
            throw new Exception(String.format("annotation string %s has bad parent type %s", annString, parentType));
        }

        // here we make sure we can handle the stored data; when we have v2, and we read
        //  v1, we'll do conversion and update here
        int storedFormatVersion = Integer.parseInt(items[3]);
        if (storedFormatVersion > FORMAT_VERSION) {
            throw new Exception(String.format("annotation string %s has newer format version %d than we can handle!", annString, storedFormatVersion));
        }
        // and someday... else storedFormatVersion < FORMAT_VERSION then does an update

        dataString = items[4];
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

    public static String toStringFromArguments(Long id, Long parentID, int parentType, int formatVersion,
        String dataString) {
        return String.format("%d:%d:%d:%d:%s", id, parentID, parentType, formatVersion, dataString);
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

