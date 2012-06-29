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
    public static final String TYPE_SUPPORTING_DATA                  = "Supporting Data";
    public static final String TYPE_SAMPLE                           = "Sample";
    public static final String TYPE_SCREEN_SAMPLE                    = "Screen Sample";
    public static final String TYPE_SCREEN_SAMPLE_CROSS              = "Screen Sample Cross";
    public static final String TYPE_FLY_LINE                         = "Fly Line";
    public static final String TYPE_LSM_STACK_PAIR                   = "LSM Stack Pair";
    public static final String TYPE_NEURON_SEPARATOR_PIPELINE_RESULT = "Neuron Separator Pipeline Result";
    public static final String TYPE_SAMPLE_PROCESSING_RESULT         = "Sample Processing Result";
    public static final String TYPE_ALIGNMENT_RESULT                 = "Alignment Result";
    public static final String TYPE_IMAGE_2D                         = "Image 2D";
    public static final String TYPE_IMAGE_3D                         = "Image 3D";
    public static final String TYPE_NEURON_FRAGMENT                  = "Neuron Fragment";
    public static final String TYPE_NEURON_FRAGMENT_COLLECTION       = "Neuron Fragment Collection";
    public static final String TYPE_ALIGNED_BRAIN_STACK              = "Aligned Brain Stack";
    public static final String TYPE_TEXT_FILE                        = "Text File";
    public static final String TYPE_SWC_FILE                         = "SWC File";
    public static final String TYPE_V3D_ANO_FILE                     = "Vaa3d ANO File";
    public static final String TYPE_FILE                             = "File";

    // Entity Data Attributes
    
    public static final String ATTRIBUTE_DEFAULT_2D_IMAGE                    = "Default 2D Image";
    public static final String ATTRIBUTE_DEFAULT_3D_IMAGE                    = "Default 3D Image";
    public static final String ATTRIBUTE_REFERENCE_MIP_IMAGE                 = "Reference MIP Image";
    public static final String ATTRIBUTE_SIGNAL_MIP_IMAGE                    = "Signal MIP Image";
    public static final String ATTRIBUTE_FILE_PATH                           = "File Path";
    public static final String ATTRIBUTE_IMAGE_FORMAT                        = "Image Format";
    public static final String ATTRIBUTE_ONTOLOGY_ELEMENT                    = "Ontology Element";
    public static final String ATTRIBUTE_COMMON_ROOT                         = "Common Root"; // Will cause folder to appear at top of Common tree pane
    public static final String ATTRIBUTE_ENTITY                              = "Entity"; // A generic attribute so that a folder can contain any entity
    public static final String ATTRIBUTE_ONTOLOGY_TERM_TYPE                  = "Ontology Term Type";
    public static final String ATTRIBUTE_IS_PUBLIC                           = "Is Public";
    public static final String ATTRIBUTE_ONTOLOGY_TERM                       = "Ontology Term";
    public static final String ATTRIBUTE_ONTOLOGY_TERM_TYPE_ENUMTEXT_ENUMID  = "Ontology Term Type EnumText Enum Id";
    public static final String ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER   = "Ontology Term Type Interval Lower Bound";
    public static final String ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER   = "Ontology Term Type Interval Upper Bound";
    public static final String ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID   = "Annotation Ontology Key Entity Id";
    public static final String ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID = "Annotation Ontology Value Entity Id";
    public static final String ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM        = "Annotation Ontology Key Term";
    public static final String ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM      = "Annotation Ontology Value Term";
    public static final String ATTRIBUTE_ANNOTATION_ONTOLOGY_ROOT_ID         = "Annotation Ontology Root Id";
    public static final String ATTRIBUTE_ANNOTATION_TARGET_ID                = "Annotation Target Id";
    public static final String ATTRIBUTE_ANNOTATION_SESSION_ID               = "Annotation Session Id";
    public static final String ATTRIBUTE_NUMBER                              = "Number";
    public static final String ATTRIBUTE_RESULT                              = "Result";
    public static final String ATTRIBUTE_IS_ZIPPED                           = "Is Zipped";
    public static final String ATTRIBUTE_LSM_STACK_1                         = "LSM Stack 1";
    public static final String ATTRIBUTE_LSM_STACK_2                         = "LSM Stack 2";
    public static final String ATTRIBUTE_MERGED_STACK                        = "Merged Stack";
    public static final String ATTRIBUTE_TILING_PATTERN                      = "Tiling Pattern";
    public static final String ATTRIBUTE_SUPPORTING_FILES                    = "Supporting Files";
    public static final String ATTRIBUTE_NEURON_FRAGMENTS                    = "Neuron Fragments";
    public static final String ATTRIBUTE_INPUT                               = "ATTRIBUTE_INPUT";
    public static final String ATTRIBUTE_ALIGNMENT_QI_SCORE                  = "Alignment Inconsistency Score";
    public static final String ATTRIBUTE_ALIGNMENT_QM_SCORE                  = "Alignment Model Violation Score";
    public static final String ATTRIBUTE_RESULT_NODE_ID                      = "Result Node Id";
    public static final String ATTRIBUTE_PERFORMANCE_PROXY_IMAGE             = "Performance Proxy Image";
    public static final String ATTRIBUTE_ARTIFACT_SOURCE_ID                  = "Artifact Source Entity Id";
    public static final String ATTRIBUTE_SPLIT_PART                          = "Split Part";
    public static final String ATTRIBUTE_REPRESENTATIVE_SAMPLE               = "Representative Sample";
    public static final String ATTRIBUTE_ORIGINAL_FLYLINE                    = "Original Fly Line";
    public static final String ATTRIBUTE_BALANCED_FLYLINE                    = "Balanced Fly Line";
    public static final String ATTRIBUTE_ROBOT_ID                            = "Robot Id";
    
    /**
     * @deprecated Use TYPE_IMAGE_2D instead. This will be removed someday.
     */
    public static final String TYPE_TIF_2D                           = "Tif 2D Image";

    /**
     * @deprecated Use TYPE_IMAGE_3D instead. This will be removed someday.
     */
    public static final String TYPE_TIF_3D                           = "Tif 3D Image";

    /**
     * @deprecated Use TYPE_IMAGE_3D instead. This will be removed someday.
     */
    public static final String TYPE_TIF_3D_LABEL_MASK                = "Tif 3D Label Mask";
    
    /**
     * @deprecated Use TYPE_IMAGE_3D instead. This will be removed someday.
     */
    public static final String TYPE_STITCHED_V3D_RAW                 	     = "Stitched V3D Raw Stack";
    
    /**
     * @deprecated Use ATTRIBUTE_DEFAULT_2D_IMAGE instead. This will be removed someday.
     */
    public static final String ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH          = "Default 2D Image File Path";
    
}
