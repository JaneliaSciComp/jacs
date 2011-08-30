package org.janelia.it.jacs.model.entity;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 6/1/11
 * Time: 9:58 AM
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
    public static final String STATUS_DEPRECATED = "Deprecated";
    public static final String STATUS_VALID      = "Valid";

    // Entity Types
    public static final String TYPE_ANNOTATION                       = "Annotation";
    public static final String TYPE_LSM_STACK                        = "LSM Stack";
    public static final String TYPE_ONTOLOGY_ROOT                    = "Ontology Root";
    public static final String TYPE_ONTOLOGY_ELEMENT                 = "Ontology Element";
    public static final String TYPE_FOLDER                           = "Folder";
    public static final String TYPE_SAMPLE                           = "Sample";
    public static final String TYPE_LSM_STACK_PAIR                   = "LSM Stack Pair";
    public static final String TYPE_NEURON_SEPARATOR_PIPELINE_RESULT = "Neuron Separator Pipeline Result";
    public static final String TYPE_TIF_2D                           = "Tif 2D Image";
    public static final String TYPE_TIF_3D                           = "Tif 3D Image";
    public static final String TYPE_TIF_3D_LABEL_MASK                = "Tif 3D Label Mask";

    // Entity Data Attributes
    public static final String ATTRIBUTE_FILE_PATH                           = "File Path";
    public static final String ATTRIBUTE_ONTOLOGY_ELEMENT                    = "Ontology Element";
    public static final String ATTRIBUTE_COMMON_ROOT                         = "Common Root"; // Will cause folder to appear at top of Common tree pane
    public static final String ATTRIBUTE_ENTITY                              = "Entity"; // A generic attribute so that a folder can contain any entity
    public static final String ATTRIBUTE_ONTOLOGY_TERM_TYPE                  = "Ontology Term Type";
    public static final String ATTRIBUTE_IS_PUBLIC                           = "Is Public";
    public static final String ATTRIBUTE_ONTOLOGY_TERM                       = "Ontology Term";
    public static final String ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER   = "Ontology Term Type Interval Lower Bound";
    public static final String ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER   = "Ontology Term Type Interval Upper Bound";
    public static final String ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID   = "Annotation Ontology Key Entity Id";
    public static final String ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID = "Annotation Ontology Value Entity Id";
    public static final String ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM        = "Annotation Ontology Key Term";
    public static final String ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM      = "Annotation Ontology Value Term";
    public static final String ATTRIBUTE_ANNOTATION_ONTOLOGY_ROOT_ID         = "Annotation Ontology Root Id";
    public static final String ATTRIBUTE_ANNOTATION_TARGET_ID                = "Annotation Target Id";
    public static final String ATTRIBUTE_ANNOTATION_SESSION_ID               = "Annotation Session Id";
    public static final String ATTRIBUTE_INPUT                               = "ATTRIBUTE_INPUT";
    
}
