package org.janelia.it.jacs.model.entity;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 6/1/11
 * Time: 9:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class EntityConstants {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // During development, this file needs to by synced with the MBean WorkstationDataManager setupEntityTypes() method.
    //
    // Eventually, a code-generation Ant task will sync this file with the database, by both adding to this file
    // from the database, and also adding to the database from this file.
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Entity Status
    public static final String STATUS_DEPRECATED = "STATUS_DEPRECATED";

    // Entity Types
    public static final String TYPE_LSM_STACK = "TYPE_LSM_STACK";
    public static final String TYPE_ONTOLOGY_ROOT = "TYPE_ONTOLOGY_ROOT";
    public static final String TYPE_ONTOLOGY_ELEMENT = "TYPE_ONTOLOGY_ELEMENT";
    public static final String TYPE_FOLDER = "TYPE_FOLDER";

    public static final long TYPE_ONTOLOGY_ROOT_ID = 1616533546980081843L;
    //public static final long TYPE_ONTOLOGY_ROOT_ID = 1;

    public static final long TYPE_ONTOLOGY_ELEMENT_ID = 1616533546954916019L;
    //public static final long TYPE_ONTOLOGY_ELEMENT_ID = 2;

    // Entity Data Attributes
    public static final String ATTRIBUTE_FILE_PATH = "ATTRIBUTE_FILE_PATH";
    public static final String ATTRIBUTE_ONTOLOGY_ELEMENT = "ATTRIBUTE_ONTOLOGY_ELEMENT";

    public static final long ATTRIBUTE_ONTOLOGY_ELEMENT_ID = 1616533546833281203L;
    //public static final long ATTRIBUTE_ONTOLOGY_ELEMENT_ID = 1;
}
