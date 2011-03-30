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

package org.janelia.it.jacs.model.tasks.export;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 23, 2008
 * Time: 11:20:05 AM
 */
public class BlastResultExportTask extends ExportTask {

    public static final String SEQUENCES_QUERY = "query";
    public static final String SEQUENCES_SUBJECT = "subject";
    public static final String SEQUENCES_SELECTED = "selected";
    public static final String SEQUENCES_ALL = "all";

    public static final String BLAST_TASK_ID = "blastTaskId";
    public static final String EXPORT_SEQUENCE_TYPE = "exportSequenceType";

    public BlastResultExportTask() {
    }

    public BlastResultExportTask(String blastTaskId, String exportSequenceType, String exportFormatType,
                                 List<String> accessionList,
                                 List<SortArgument> exportAttributeList) {
        super(exportFormatType, accessionList, exportAttributeList);
        setParameter(BLAST_TASK_ID, blastTaskId);
        setParameter(EXPORT_SEQUENCE_TYPE, exportSequenceType);
        String suggestedFilename = "blast_results";
        if (SEQUENCES_QUERY.equals(exportSequenceType)) {
            suggestedFilename = "matching_queries";
        }
        else if (SEQUENCES_SUBJECT.equals(exportSequenceType)) {
            suggestedFilename = "matching_subjects";
        }
        String tmpMimeType = ("blastXml".equalsIgnoreCase(exportFormatType)) ? "xml" : exportFormatType;
        setParameter(PARAM_SUGGESTED_FILENAME, formatSuggestedFilename(
                suggestedFilename + "_" + blastTaskId + "." + tmpMimeType));

        this.taskName = "Blast Result Export Task";
    }

    public String getDataType() {
        return "BlastResults";
    }

    public String getDisplayName() {
        return "Export Blast Results";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        ParameterVO pvo = super.getParameterVO(key);
        if (pvo != null)
            return pvo;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(BLAST_TASK_ID)) {
            Long tmpLong = new Long(value);
            return new LongParameterVO(tmpLong, tmpLong, tmpLong);
        }
        if (key.equals(EXPORT_SEQUENCE_TYPE)) {
            return new TextParameterVO(value);
        }
        // No match
        return null;
    }

}
