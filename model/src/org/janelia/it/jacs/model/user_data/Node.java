
package org.janelia.it.jacs.model.user_data;
// Generated Aug 17, 2006 3:17:24 PM by Hibernate Tools 3.2.0.beta6a

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;


/**
 * Node generated by hbm2java
 */
public class Node implements java.io.Serializable, IsSerializable {

    // Common visibility types
    public static final String VISIBILITY_PRIVATE_DEPRECATED = "deprecated";
    public static final String VISIBILITY_PRIVATE = "private";
    public static final String VISIBILITY_PUBLIC = "public";
    public static final String VISIBILITY_INACTIVE = "inactive"; // used for loading to filestore before db

    // Data type constants
    public static final String HTML_DATA_TYPE = "html";
    public static final String MICROBE_DATA_TYPE = "microbe";
    public static final String DIRECTORY_DATA_TYPE = "directory";
    public static final String FILE_DATA_TYPE = "file";
    public static final String DIRECTORY_AGGREGATE_DATA_TYPE = "directory aggregate";
    public static final String USER_DATA_TYPE = "user";
    // Fields

    private Long objectId;
    private Task task;                                   // Task that created the node, eg. BlastTask,
    private String description;                          // Longer Description of node (open ended field)
    private String visibility;                           // User's private data, or available to public
    // to ensure homogeniety of feature type
    private String dataType;                             // Type flag used by the nodes differently
    private String name;                                 // Short description of data node 
    private String owner;                                // Owner of node - user login
    protected Long length;
    private Integer ord;
    private String relativeSessionPath;

    // Constructors

    /**
     * default constructor
     */
    public Node() {
    }

    /**
     * full constructor
     *
     * @param owner               - person who owns the node
     * @param task                - task which created this node
     * @param name                - name of the node
     * @param description         - description of the node
     * @param visibility          - visibility of the node to others
     * @param relativeSessionPath - name of the work session this node belongs to
     * @param dataType            - tag for the node
     */
    public Node(String owner, Task task, String name, String description, String visibility, String dataType,
                String relativeSessionPath) {
        this.task = task;
        this.name = name;
        this.description = description;
        this.visibility = visibility;
        this.dataType = dataType;
        setOwner(owner);
        this.relativeSessionPath = relativeSessionPath;
    }


    // Property accessors
    public Long getObjectId() {
        return this.objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public Task getTask() {
        return this.task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisibility() {
        return this.visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getDataType() {
        return this.dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
        if (null!=owner && owner.contains(":")) {
        	this.owner = owner.split(":")[1];
        }
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public String toString() {
        return "Node{" +
                "dataType='" + dataType + '\'' +
                ", objectId=" + ((objectId == null) ? "null" : objectId.toString()) +
                ", owner='" + ((owner == null) ? "null" : owner) + '\'' +
                ", task=" + ((task == null) ? "null" : task.toString()) +
                ", description='" + description + '\'' +
                ", visibility='" + visibility + '\'' +
                ", name='" + name + '\'' +
                ", relativeSessionPath='" + relativeSessionPath + '\'' +
                '}';
    }

    public Integer getOrd() {
        return ord;
    }

    public void setOrd(Integer ord) {
        this.ord = ord;
    }

    public String getRelativeSessionPath() {
        return relativeSessionPath;
    }

    public void setRelativeSessionPath(String relativeSessionPath) {
        this.relativeSessionPath = relativeSessionPath;
    }
}


