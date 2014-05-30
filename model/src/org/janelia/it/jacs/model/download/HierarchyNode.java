
package org.janelia.it.jacs.model.download;
// Generated Oct 30, 2006 10:53:18 AM by Hibernate Tools 3.2.0.beta6a


import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * HierarchyNode generated by hbm2java
 */
public class HierarchyNode implements Serializable, IsSerializable {

    // Fields

    private Long objectId;
    private String name;
    private String description;
    private List<DataFile> dataFiles = new ArrayList<DataFile>();
    private List<HierarchyNode> children = new ArrayList<HierarchyNode>();

    // Constructors

    /**
     * default constructor
     */
    public HierarchyNode() {
    }

    /**
     * full constructor
     */
    public HierarchyNode(String name, String description, List dataFiles, List children) {
        this.name = name;
        this.description = description;
        this.dataFiles = dataFiles;
        this.children = children;
    }


    // Property accessors
    public Long getObjectId() {
        return this.objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DataFile> getDataFiles() {
        return this.dataFiles;
    }

    public void setDataFiles(List<DataFile> dataFiles) {
        this.dataFiles = dataFiles;
    }

    public List<HierarchyNode> getChildren() {
        return this.children;
    }

    public void setChildren(List<HierarchyNode> children) {
        this.children = children;
    }

}


