
package org.janelia.it.jacs.compute.service.common.file;

import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 29, 2008
 * Time: 10:29:44 AM
 */
public interface FileServiceConstants extends ProcessDataConstants {

    // MultiFastaSplitter
    public static final String INPUT_FILE = "INPUT_FILE";
    public static final String PARTITION_LIST = "PARTITION_LIST";
    public static final String MAX_RESULTS_PER_JOB = "MAX_RESULTS_PER_JOB";
    public static final String MAX_OUTPUT_SIZE = "MAX_OUTPUT_SIZE";
    public static final String MAX_INPUT_ENTRIES_PER_JOB = "MAX_INPUT_ENTRIES_PER_JOB";
    public static final String MAX_NUMBER_OF_JOBS = "MAX_NUMBER_OF_JOBS";
    public static final String OUTPUT_ADDITIONAL_SIZE = "OUTPUT_ADDITIONAL_SIZE";
    public static final String PER_INPUT_ENTRY_SIZE_MULTIPLIER = "PER_INPUT_ENTRY_SIZE_MULTIPLIER";
    public static final String POST_SPLIT_INPUT_FILE_LIST = "POST_SPLIT_INPUT_FILE_LIST";

    // CreateOutputDirs
    public static final String OUTPUT_DIR_LIST = "OUTPUT_DIR_LIST";
    public static final String INPUT_OUTPUT_DIR_MAP = "INPUT_OUTPUT_DIR_MAP";

    // SubmitJob
    public static final String INPUT_OUTPUT_FILE_LIST_MAP = "INPUT_OUTPUT_FILE_LIST_MAP";

}
