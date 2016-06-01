package org.janelia.it.jacs.model.domain;

/**
 * A place for constants.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainConstants {

    public static final String VALUE_PROCESSING = "Processing";
    public static final String VALUE_COMPLETE = "Complete";
    public static final String VALUE_ERROR = "Error";
    public static final String VALUE_MARKED = "Marked for Rerun";
    public static final String VALUE_BLOCKED = "Blocked";
    public static final String VALUE_DESYNC = "Desync";
    public static final String VALUE_RETIRED = "Retired";

    public static final String VALUE_COMPRESSION_LOSSLESS = "Lossless";
    public static final String VALUE_COMPRESSION_LOSSLESS_AND_H5J = "Lossless and H5J";
    public static final String VALUE_COMPRESSION_VISUALLY_LOSSLESS_AND_PBD = "Visually Lossless and PBD";
    public static final String VALUE_COMPRESSION_VISUALLY_LOSSLESS = "Visually Lossless";

    public static final String NAME_DEFAULT_WORKSPACE                = "Default Workspace";
    public static final String NAME_SHARED_DATA                      = "Shared Data";
    public static final String NAME_SPLIT_PICKING                    = "Split Picking";
    public static final String NAME_DATA_SETS                        = "Data Sets";
    public static final String NAME_FLY_LINE_RELEASES                = "Fly Line Releases";
    public static final String NAME_ALIGNMENT_BOARDS                 = "Alignment Boards";
    public static final String NAME_RETIRED_DATA                     = "Retired Data";
    public static final String NAME_BLOCKED_DATA                     = "Blocked Data";
    public static final String NAME_SEARCH_RESULTS                   = "Search Results";
    
    public static final String PREFERENCE_CATEGORY_SAMPLE_RESULT = "SampleResult";
    public static final String PREFERENCE_CATEGORY_IMAGE_TYPE = "ImageType";
    public static final String PREFERENCE_VALUE_LATEST = "Latest";
    public static final String PREFERENCE_CATEGORY_TABLE_COLUMNS = "TableColumns";
    public static final String PREFERENCE_CATEGORY_DOMAIN_OBJECT_TITLES = "DomainObjectTitles";
    public static final String PREFERENCE_CATEGORY_DOMAIN_OBJECT_SUBTITLES = "DomainObjectSubtitles";
    public static final String PREFERENCE_CATEGORY_NEURON_SEPARATION_VISIBILITY = "NeuronSepVisibility";

    public static final String GENERAL_USER_GROUP_KEY = "group:workstation_users";
    
    public static final String ERROR_ONTOLOGY_NAME = "Image Evaluation";
    public static final String ERROR_ONTOLOGY_CATEGORY = "Report";

    public static final String SCALITY_PATH_PREFIX = "/scality/";

}
