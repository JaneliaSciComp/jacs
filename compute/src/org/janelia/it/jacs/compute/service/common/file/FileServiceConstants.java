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
