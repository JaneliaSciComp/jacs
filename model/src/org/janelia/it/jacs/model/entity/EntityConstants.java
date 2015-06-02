package org.janelia.it.jacs.model.entity;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 6/1/11
 * Time: 9:58 AM
 */
public class EntityConstants {

    public static final String VALUE_SC_GUID                         = "GUID";
    public static final String VALUE_SC_NAME                         = "Name";
    public static final String VALUE_SC_DATE_CREATED                 = "Date Created";
    public static final String VALUE_SC_DATE_UPDATED                 = "Date Updated";
    public static final String VALUE_SC_SORT_ORDER_ASC               = "Ascending";
    public static final String VALUE_SC_SORT_ORDER_DESC              = "Descending";
    
    public static final String VALUE_PROCESSING                      = "Processing";
    public static final String VALUE_COMPLETE                        = "Complete";
    public static final String VALUE_ERROR                           = "Error";
    public static final String VALUE_MARKED                          = "Marked for Rerun";
    public static final String VALUE_BLOCKED                         = "Blocked";
    public static final String VALUE_DESYNC                          = "Desync";
    public static final String VALUE_RETIRED                         = "Retired";
    
    public static final String VALUE_COMPRESSION_UNCOMPRESSED               = "Uncompressed";
    public static final String VALUE_COMPRESSION_LOSSLESS                   = "Lossless";
    public static final String VALUE_COMPRESSION_LOSSLESS_AND_H5J           = "Lossless and H5J";
    public static final String VALUE_COMPRESSION_VISUALLY_LOSSLESS_AND_PBD  = "Visually Lossless and PBD";
    public static final String VALUE_COMPRESSION_VISUALLY_LOSSLESS          = "Visually Lossless";
    
    // Singleton Entity Names
    public static final String NAME_DEFAULT_WORKSPACE                = "Default Workspace";
    public static final String NAME_SHARED_DATA                      = "Shared Data";
    public static final String NAME_SPLIT_PICKING                    = "Split Picking";
    public static final String NAME_DATA_SETS                        = "Data Sets";
    public static final String NAME_ALIGNMENT_BOARDS                 = "Alignment Boards";
    public static final String NAME_RETIRED_DATA                     = "Retired Data";
    public static final String NAME_BLOCKED_DATA                     = "Blocked Data";
    public static final String NAME_SEARCH_RESULTS                   = "Search Results";
    
    // Virtual types (not persisted)
    public static final String IN_MEMORY_TYPE_PLACEHOLDER_ENTITY     = "Placeholder Entity";
    public static final String IN_MEMORY_TYPE_VIRTUAL_ENTITY         = "Virtual Entity";
    public static final String IN_MEMORY_ATTRIBUTE_TITLE             = "Virtual Title";
    
    // Common Types
    public static final String TYPE_ANNOTATION                       = "Annotation";
    public static final String TYPE_FOLDER                           = "Folder";
    public static final String TYPE_FILE                             = "File";
    public static final String TYPE_TEXT_FILE                        = "Text File";
    public static final String TYPE_ONTOLOGY_ROOT                    = "Ontology Root";
    public static final String TYPE_ONTOLOGY_ELEMENT                 = "Ontology Element";
    
    // Imaging Types
    public static final String TYPE_IMAGE_2D                         = "Image 2D";
    public static final String TYPE_IMAGE_3D                         = "Image 3D";
    public static final String TYPE_MOVIE                            = "Movie";
    public static final String TYPE_COMPARTMENT_SET                  = "Compartment Set";
    public static final String TYPE_COMPARTMENT                      = "Compartment";
    
    // Single Neuron Imaging Types
    public static final String TYPE_WORKSPACE                        = "Workspace";
    public static final String TYPE_ERROR                            = "Error";
    public static final String TYPE_DATA_SET                         = "Data Set";
    public static final String TYPE_SAMPLE                           = "Sample";
    public static final String TYPE_SUPPORTING_DATA                  = "Supporting Data";
    public static final String TYPE_LSM_STACK                        = "LSM Stack";
    public static final String TYPE_IMAGE_TILE                       = "Image Tile";
    public static final String TYPE_PIPELINE_RUN                     = "Pipeline Run";
    public static final String TYPE_SAMPLE_PROCESSING_RESULT         = "Sample Processing Result";
    public static final String TYPE_ALIGNMENT_RESULT                 = "Alignment Result";
    public static final String TYPE_NEURON_SEPARATOR_PIPELINE_RESULT = "Neuron Separator Pipeline Result";
    public static final String TYPE_MYERS_NEURON_SEPARATION_FILE     = "Myers Neuron Separation File";
    public static final String TYPE_NEURON_FRAGMENT                  = "Neuron Fragment";
    public static final String TYPE_NEURON_FRAGMENT_COLLECTION       = "Neuron Fragment Collection";
    public static final String TYPE_CURATED_NEURON                   = "Curated Neuron";
    public static final String TYPE_CURATED_NEURON_COLLECTION        = "Curated Neuron Collection";
    public static final String TYPE_CELL_COUNTING_RESULT             = "Cell Counting Result";
    
    // Screen Imaging Types
    public static final String TYPE_FLY_LINE                         = "Fly Line";
    public static final String TYPE_SCREEN_SAMPLE                    = "Screen Sample";
    public static final String TYPE_SCREEN_SAMPLE_CROSS              = "Screen Sample Cross";
    public static final String TYPE_ALIGNED_BRAIN_STACK              = "Aligned Brain Stack";
    public static final String TYPE_SWC_FILE                         = "SWC File";
    public static final String TYPE_V3D_ANO_FILE                     = "Vaa3d ANO File";
    
    // Alignment Board Types
    public static final String TYPE_ALIGNMENT_BOARD                  = "Alignment Board";
    public static final String TYPE_ALIGNMENT_SPACE                  = "Alignment Space";
    public static final String TYPE_ALIGNED_ITEM                     = "Aligned Item";
    public static final String TYPE_DEFORMATION_MATRIX               = "Deformation Matrix";
    
    // Tile Microscope 
    public static final String TYPE_3D_TILE_MICROSCOPE_SAMPLE        = "3D Tile Microscope Sample";
    public static final String TYPE_TILE_MICROSCOPE_WORKSPACE        = "Tile Microscope Workspace";
    public static final String TYPE_TILE_MICROSCOPE_NEURON           = "Tile Microscope Neuron";
    public static final String TYPE_PROPERTY_SET                     = "Property Set";
    
    // Entity Data Attributes
    public static final String ATTRIBUTE_STATUS                              = "Status";
    public static final String ATTRIBUTE_COMRESSION_TYPE                     = "Compression Type";
    public static final String ATTRIBUTE_LINE                                = "Line";
    public static final String ATTRIBUTE_CROSS_BARCODE                       = "Cross Barcode";
    public static final String ATTRIBUTE_SLIDE_CODE                          = "Slide Code";
    public static final String ATTRIBUTE_AGE                                 = "Age";
    public static final String ATTRIBUTE_EFFECTOR                            = "Effector";
    public static final String ATTRIBUTE_SAMPLE_NAME_PATTERN                 = "Sample Name Pattern";
    public static final String ATTRIBUTE_SAMPLE_IMAGE_TYPE                   = "Sample Image Type";
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
    public static final String ATTRIBUTE_ANNOTATION_IS_COMPUTATIONAL         = "Annotation Is Computational";
    public static final String ATTRIBUTE_NUMBER                              = "Number";
    public static final String ATTRIBUTE_NAME                                = "Name";
    public static final String ATTRIBUTE_RESULT                              = "Result";
    public static final String ATTRIBUTE_IS_ZIPPED                           = "Is Zipped";
    public static final String ATTRIBUTE_MERGED_STACK                        = "Merged Stack";
    public static final String ATTRIBUTE_SUPPORTING_FILES                    = "Supporting Files";
    public static final String ATTRIBUTE_ALIGNMENT_NCC_SCORE                 = "Alignment Normalized Cross Correlation Score";
    public static final String ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORE       = "Alignment Inconsistency Score";
    public static final String ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORES      = "Alignment Inconsistency Scores";
    public static final String ATTRIBUTE_ALIGNMENT_QI_SCORE                  = "Alignment Qi Score";
    public static final String ATTRIBUTE_ALIGNMENT_QI_SCORES                 = "Alignment Qi Scores";
    public static final String ATTRIBUTE_ALIGNMENT_MODEL_VIOLATION_SCORE     = "Alignment Model Violation Score";
    public static final String ATTRIBUTE_ALIGNMENT_VERIFY_MOVIE              = "Alignment Verify Movie";
    public static final String ATTRIBUTE_ALIGNMENT_BOARD_USER_SETTINGS       = "Alignment Board User Settings";
    public static final String ATTRIBUTE_ALIGNMENT_OVERLAP_COEFFICIENT       = "Alignment Overlap Coefficient";
    public static final String ATTRIBUTE_ALIGNMENT_OBJECT_PEARSON_COEFFICIENT= "Alignment Object Pearson Coefficient";
    public static final String ATTRIBUTE_RESULT_NODE_ID                      = "Result Node Id";
    public static final String ATTRIBUTE_PERFORMANCE_PROXY_IMAGE             = "Performance Proxy Image";
    public static final String ATTRIBUTE_ARTIFACT_SOURCE_ID                  = "Artifact Source Entity Id";
    public static final String ATTRIBUTE_ANATOMICAL_AREA                     = "Anatomical Area"; // e.g. "Brain", "VNC"
    public static final String ATTRIBUTE_GENDER                              = "Gender"; // "m" or "f"
    public static final String ATTRIBUTE_MOUNTING_PROTOCOL                   = "Mounting Protocol"; // e.g. "DPX PBS Mounting"
    public static final String ATTRIBUTE_TISSUE_ORIENTATION                  = "Tissue Orientation"; // e.g. "face_up", "face_down"
    public static final String ATTRIBUTE_VT_LINE                             = "VT Line"; // e.g. "face_up", "face_down"
    public static final String ATTRIBUTE_NUM_CHANNELS                        = "Num Channels"; // e.g. "3"
    public static final String ATTRIBUTE_OPTICAL_RESOLUTION                  = "Optical Resolution"; // e.g. "0.38x0.38x0.37"
    public static final String ATTRIBUTE_PIXEL_RESOLUTION                    = "Pixel Resolution"; // e.g. "1024x512x218"
    public static final String ATTRIBUTE_OBJECTIVE                           = "Objective"; // e.g. "63x"
    public static final String ATTRIBUTE_BOUNDING_BOX                        = "Bounding Box"; // e.g. "30,10,10,300,500,600"
    public static final String ATTRIBUTE_MASK_ENTITY_COLLECTION              = "Mask Entity Collection";
    public static final String ATTRIBUTE_VISITED                             = "Visited"; // A tag that marks an entity as having been visited by some pipeline process 
    public static final String ATTRIBUTE_CHANNEL_DYE_NAMES                   = "Channel Dye Names"; // e.g. "Alexa Fluor 488,Alexa Fluor 633,DY-547"
    public static final String ATTRIBUTE_CHANNEL_COLORS                      = "Channel Colors"; // e.g. "#7e5200,#ff00ff,#00ff00"
    public static final String ATTRIBUTE_SEARCH_TASK_ID                      = "Search Task Id"; // Id of the search task that created this folder and results
    public static final String ATTRIBUTE_PROPERTY                            = "Property"; // has string "<property>=<value>"
    public static final String ATTRIBUTE_WORKSPACE_SAMPLE_IDS                = "Workspace Sample Ids";
    public static final String ATTRIBUTE_CELL_COUNT                          = "Cell Count";
    public static final String ATTRIBUTE_INPUT_IMAGE                         = "Input Image";
    public static final String ATTRIBUTE_SOURCE_SEPARATION                   = "Source Separation";
    public static final String ATTRIBUTE_IS_WARPED_SEPARATION                = "Is Warped Separation";
    public static final String ATTRIBUTE_MICRON_TO_VOXEL_MATRIX              = "Micron to Voxel Matrix";
    public static final String ATTRIBUTE_VOXEL_TO_MICRON_MATRIX              = "Voxel to Micron Matrix";
    public static final String ATTRIBUTE_SCALITY_BPID                        = "Scality BPID";
        
    // Geometric Tree
    //
    //  "<id>:<parentId>:<index>:<x,y,z>:<comment>"
    //
    public static final String ATTRIBUTE_GEO_TREE_COORDINATE                 = "Geometric Tree Coordinate";
    public static final String ATTRIBUTE_GEO_ROOT_COORDINATE                 = "Root Geometric Tree Element";

    // Anchored path
    // "<id>:<attributeID1>:<attributeID2>:x,y,z:(repeat x,y,z)"
    public static final String ATTRIBUTE_ANCHORED_PATH                       = "Anchored Path";

    // TmStructuredTextAnnotation
    // "<id>:<parentId>:<parentType>:<FORMAT_VERSION>:<dataString>"
    public static final String ATTRIBUTE_STRUCTURED_TEXT                     = "Structured Text";

    // Alignment Spaces
    public static final String ATTRIBUTE_ALIGNMENT_SPACE                     = "Alignment Space"; // e.g. "Unified 20x Alignment Space"
    public static final String ATTRIBUTE_ITEM                                = "Item";            // The actual entity item wrapped by an Aligned Item
    public static final String ATTRIBUTE_VISIBILITY                          = "Visibility";      // e.g. "true"
    public static final String ATTRIBUTE_COLOR                               = "Color";           // RGBA hex format, e.g. "ff0000ff"
    public static final String ATTRIBUTE_RENDER_METHOD                       = "Render Method";   // Special overrides to normal rendering.  Ex: Passthrough
    public static final String ATTRIBUTE_INCLUSION_STATUS                    = "Inclusion Status";// Member of enum AlignmentItem.InclusionStatus' mixed case constants

    public static final String ATTRIBUTE_MASK_IMAGE                          = "Mask Image";
    public static final String ATTRIBUTE_CHAN_IMAGE                          = "Chan Image";
    public static final String ATTRIBUTE_SLIGHTLY_LOSSY_IMAGE                = "Slightly Lossy Image"; // Lossy-compressed version of the image
    
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
    public static final String ATTRIBUTE_LSM_STORAGE_PATTERN                 = "LSM Storage Pattern";
    public static final String ATTRIBUTE_PIPELINE_PROCESS                    = "Pipeline Process";
    public static final String ATTRIBUTE_SAGE_SYNC                           = "SAGE Sync";
    public static final String ATTRIBUTE_SAGE_ID                             = "SAGE Id";
    
    // Error Attributes
    public static final String ATTRIBUTE_DESCRIPTION                         = "Description";
    public static final String ATTRIBUTE_CLASSIFICATION                      = "Classification";
    
    // Ontology Type Values
    public static final String VALUE_ONTOLOGY_TERM_TYPE_INTERVAL             = "Interval";
    public static final String VALUE_ONTOLOGY_TERM_TYPE_ENUM                 = "Enum";
    public static final String VALUE_ONTOLOGY_TERM_TYPE_TEXT                 = "Text";
    public static final String VALUE_ONTOLOGY_TERM_TYPE_ENUM_TEXT            = "EnumText";

    /**
     * @deprecated Use ATTRIBUTE_DEFAULT_2D_IMAGE instead. This will be removed someday.
     */
    public static final String ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH           = "Default 2D Image File Path";

}
