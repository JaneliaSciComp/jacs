/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
