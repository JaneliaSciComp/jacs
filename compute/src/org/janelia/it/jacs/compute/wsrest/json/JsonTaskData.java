package org.janelia.it.jacs.compute.wsrest.json;

import org.janelia.it.jacs.model.user_data.FileNode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by goinac on 10/23/15.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JsonTaskData {
    private Long nodeId;
    private Long taskId;
    private String dataType;
    private String name;
    private String description;
    private String owner;
    private String path;

    JsonTaskData() {
        // needed by JAXB
    }

    public JsonTaskData(FileNode fn) {
        nodeId = fn.getObjectId();
        taskId = fn.getTask().getObjectId();
        dataType = fn.getDataType();
        name = fn.getName();
        description = fn.getDescription();
        owner = fn.getOwner();
        path = fn.getDirectoryPath();
    }

    public Long getNodeId() {
        return nodeId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getDataType() {
        return dataType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getOwner() {
        return owner;
    }

    public String getPath() {
        return path;
    }
}
