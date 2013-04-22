package org.janelia.it.jacs.model.entity;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 6/1/11
 * Time: 9:58 AM
 */
public class EntityConstants {

    // Entity Status
    public static final String STATUS_DEPRECATED                     = "Deprecated";
    public static final String STATUS_VALID                          = "Valid";

    // Common Entity Names
    public static final String NAME_SHARED_DATA                      = "Shared Data";
    public static final String NAME_SPLIT_PICKING                    = "Split Picking";
    public static final String NAME_DATA_SETS                        = "Data Sets";
    public static final String NAME_ALIGNMENT_BOARDS                 = "Alignment Boards";
    public static final String NAME_TRASH                            = "Trash";
    
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
    public static final String TYPE_IMAGE_TILE                       = "Image Tile";
    public static final String TYPE_NEURON_SEPARATOR_PIPELINE_RESULT = "Neuron Separator Pipeline Result";
    public static final String TYPE_MYERS_NEURON_SEPARATION_FILE     = "Myers Neuron Separation File";
    public static final String TYPE_SAMPLE_PROCESSING_RESULT         = "Sample Processing Result";
    public static final String TYPE_ALIGNMENT_RESULT                 = "Alignment Result";
    public static final String TYPE_IMAGE_2D                         = "Image 2D";
    public static final String TYPE_IMAGE_3D                         = "Image 3D";
    public static final String TYPE_MOVIE                            = "Movie";
    public static final String TYPE_NEURON_FRAGMENT                  = "Neuron Fragment";
    public static final String TYPE_NEURON_FRAGMENT_COLLECTION       = "Neuron Fragment Collection";
    public static final String TYPE_ALIGNED_BRAIN_STACK              = "Aligned Brain Stack";
    public static final String TYPE_TEXT_FILE                        = "Text File";
    public static final String TYPE_SWC_FILE                         = "SWC File";
    public static final String TYPE_V3D_ANO_FILE                     = "Vaa3d ANO File";
    public static final String TYPE_FILE                             = "File";
    public static final String TYPE_CURATED_NEURON                   = "Curated Neuron";
    public static final String TYPE_CURATED_NEURON_COLLECTION        = "Curated Neuron Collection";
    public static final String TYPE_PIPELINE_RUN                     = "Pipeline Run";
    public static final String TYPE_ERROR                            = "Error";
    public static final String TYPE_DATA_SET                         = "Data Set";
    public static final String TYPE_ALIGNMENT_BOARD                  = "Alignment Board";
    public static final String TYPE_ALIGNMENT_SPACE                  = "Alignment Space";
    public static final String TYPE_MASK_SET                         = "Mask Set";
    public static final String TYPE_MASK                             = "Mask";
    public static final String TYPE_ALIGNED_ITEM                     = "Aligned Item";
    public static final String TYPE_DEFORMATION_MATRIX               = "Deformation Matrix";
    public static final String TYPE_3D_TILE_MICROSCOPE_SAMPLE        = "3D Tile Microscope Sample";

    // Entity Data Attributes

    public static final String ATTRIBUTE_LINE                                = "Line";
    public static final String ATTRIBUTE_SLIDE_CODE                          = "Slide Code";
    public static final String ATTRIBUTE_AGE                                 = "Age";
    public static final String ATTRIBUTE_EFFECTOR                            = "Effector";
    public static final String ATTRIBUTE_SAMPLE_NAME_PATTERN                 = "Sample Name Pattern";
    public static final String ATTRIBUTE_DENORM_IDENTIFIER                   = "Denorm Identifier";
    public static final String ATTRIBUTE_DEFAULT_2D_IMAGE                    = "Default 2D Image";
    public static final String ATTRIBUTE_DEFAULT_3D_IMAGE                    = "Default 3D Image";
    public static final String ATTRIBUTE_DEFAULT_FAST_3D_IMAGE               = "Default Fast 3D Image";
    public static final String ATTRIBUTE_REFERENCE_MIP_IMAGE                 = "Reference MIP Image";
    public static final String ATTRIBUTE_SIGNAL_MIP_IMAGE                    = "Signal MIP Image";
    public static final String ATTRIBUTE_FILE_PATH                           = "File Path";
    public static final String ATTRIBUTE_IMAGE_FORMAT                        = "Image Format";
    public static final String ATTRIBUTE_ONTOLOGY_ELEMENT                    = "Ontology Element";
    public static final String ATTRIBUTE_COMMON_ROOT                         = "Common Root"; // Will cause folder to appear at top of Common tree pane
    public static final String ATTRIBUTE_IS_PROTECTED                        = "Is Protected"; // Protected from deletion/modification by owner
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
    public static final String ATTRIBUTE_MERGED_STACK                        = "Merged Stack";
    public static final String ATTRIBUTE_TILING_PATTERN                      = "Tiling Pattern";
    public static final String ATTRIBUTE_SUPPORTING_FILES                    = "Supporting Files";
    public static final String ATTRIBUTE_ALIGNMENT_QI_SCORE                  = "Alignment Inconsistency Score";
    public static final String ATTRIBUTE_ALIGNMENT_QM_SCORE                  = "Alignment Model Violation Score";
    public static final String ATTRIBUTE_RESULT_NODE_ID                      = "Result Node Id";
    public static final String ATTRIBUTE_PERFORMANCE_PROXY_IMAGE             = "Performance Proxy Image";
    public static final String ATTRIBUTE_ARTIFACT_SOURCE_ID                  = "Artifact Source Entity Id";
    public static final String ATTRIBUTE_ANATOMICAL_AREA                     = "Anatomical Area"; // e.g. "Brain", "VNC"
    public static final String ATTRIBUTE_GENDER                              = "Gender"; // "m" or "f"
    public static final String ATTRIBUTE_MOUNTING_PROTOCOL                   = "Mounting Protocol"; // e.g. "DPX PBS Mounting"
    public static final String ATTRIBUTE_NUM_CHANNELS                        = "Num Channels"; // e.g. "3"
    public static final String ATTRIBUTE_OPTICAL_RESOLUTION                  = "Optical Resolution"; // e.g. "0.38x0.38x0.37"
    public static final String ATTRIBUTE_PIXEL_RESOLUTION                    = "Pixel Resolution"; // e.g. "1024x512x218"
    public static final String ATTRIBUTE_OBJECTIVE                           = "Objective"; // e.g. "63x"
    public static final String ATTRIBUTE_BOUNDING_BOX                        = "Bounding Box"; // e.g. "30,10,10,300,500,600"
    public static final String ATTRIBUTE_MASK_ENTITY_COLLECTION              = "Mask Entity Collection";
    public static final String ATTRIBUTE_VISITED                             = "Visited"; // A tag that marks an entity as having been visited by some pipeline process 
    public static final String ATTRIBUTE_CHANNEL_DYE_NAMES                   = "Channel Dye Names"; // e.g. "Alexa Fluor 488,Alexa Fluor 633,DY-547"
    public static final String ATTRIBUTE_CHANNEL_COLORS                      = "Channel Colors"; // e.g. "#7e5200,#ff00ff,#00ff00"
    public static final String ATTRIBUTE_METADATA_FILE                       = "Metadata File";
    public static final String ATTRIBUTE_SEARCH_TASK_ID                      = "Search Task Id"; // Id of the search task that created this folder and results

    // Alignment Spaces
    public static final String ATTRIBUTE_ALIGNMENT_SPACE                     = "Alignment Space"; // e.g. "Unified 20x Alignment Space"
    public static final String ATTRIBUTE_ITEM                                = "Item";            // The actual entity item wrapped by an Aligned Item
    public static final String ATTRIBUTE_VISIBILITY                          = "Visibility";      // e.g. "true"
    public static final String ATTRIBUTE_COLOR                               = "Color";           // RGBA hex format, e.g. "ff0000ff"

    public static final String ATTRIBUTE_MASK_IMAGE                          = "Mask Image";
    public static final String ATTRIBUTE_CHAN_IMAGE                          = "Chan Image";
    
    public static final String ATTRIBUTE_ALIGNED_CONSOLIDATED_LABEL          = "Aligned Consolidated Label";
    
    // Fly Line Attributes
    public static final String ATTRIBUTE_SPLIT_PART                          = "Split Part";
    public static final String ATTRIBUTE_REPRESENTATIVE_SAMPLE               = "Representative Sample";
    public static final String ATTRIBUTE_ORIGINAL_FLYLINE                    = "Original Fly Line";
    public static final String ATTRIBUTE_BALANCED_FLYLINE                    = "Balanced Fly Line";
    
    // Sample/Data Set Attributes
    public static final String ATTRIBUTE_ROBOT_ID                            = "Robot Id";
    public static final String ATTRIBUTE_CHANNEL_SPECIFICATION               = "Channel Specification";
    public static final String ATTRIBUTE_CROSS_LABEL                         = "Cross Label";
    public static final String ATTRIBUTE_DATA_SET_IDENTIFIER                 = "Data Set Identifier";
    public static final String ATTRIBUTE_PIPELINE_PROCESS                    = "Pipeline Process";
    public static final String ATTRIBUTE_SAGE_SYNC                           = "SAGE Sync";
    public static final String ATTRIBUTE_SAGE_ID                             = "SAGE Id";
    
    // Error Attributes
    public static final String ATTRIBUTE_MESSAGE                             = "Message";
    
    // Ontology Type Values
    public static final String VALUE_ONTOLOGY_TERM_TYPE_INTERVAL            = "Interval";
    public static final String VALUE_ONTOLOGY_TERM_TYPE_ENUM                = "Enum";
    public static final String VALUE_ONTOLOGY_TERM_TYPE_TEXT                = "Text";
    public static final String VALUE_ONTOLOGY_TERM_TYPE_ENUM_TEXT           = "EnumText";

    /**
     * @deprecated Use ATTRIBUTE_MASK_ENTITY_COLLECTION instead. This will be removed someday.
     */
    public static final String ATTRIBUTE_NEURON_FRAGMENTS                    = "Neuron Fragments";
    
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
