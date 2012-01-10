
package org.janelia.it.jacs.compute.service.blast;

import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;

/**
 * This inteface contains constants specific to Blast process data
 *
 * @author Tareq Nabeel
 */
public interface BlastProcessDataConstants extends ProcessDataConstants {

    public static final String BLAST_TYPE = "blastType";
    public static final String DATA_SET_NAME = "subjectDatabase";
    public static final String FAST_TEXT = "FAST_TEXT";
    public static final String BLAST_PARAMETERS = "BLAST_PARAMETERS";
    public static final String BLAST_PARTITION_LIST = "BLAST_PARTITION_LIST";
    public static final String BLAST_NUMBER_OF_HITS_TO_KEEP = "BLAST_NUMBER_OF_HITS_TO_KEEP";
    public static final String BLAST_MESSAGE_IDS = "BLAST_MESSAGE_IDS";
    public static final String BLAST_FASTA_INPUT_FILE = "BLAST_FASTA_INPUT_FILE";
    public static final String BLAST_DEST_OUTPUT_DIR = "BLAST_DEST_OUTPUT_DIR";
    public static final String BLAST_QUERY_DATA_NODE = "BLAST_QUERY_DATA_NODE";
    public static final String BLAST_QUERY_FILES = "BLAST_QUERY_FILES";
    public static final String BLAST_QUERY_OUTPUT_FILE_MAP = "BLAST_QUERY_OUTPUT_FILE_MAP";
    public static final String TOTAL_BLAST_HITS = "TOTAL_BLAST_HITS";

}
