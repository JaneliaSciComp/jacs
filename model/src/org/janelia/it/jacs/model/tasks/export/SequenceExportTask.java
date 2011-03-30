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

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 23, 2008
 * Time: 11:20:05 AM
 */
public class SequenceExportTask extends ExportTask {

    public SequenceExportTask() {
    }

    public SequenceExportTask(String exportFormatType, List<String> accessionList,
                              List<SortArgument> exportAttributeList) {
        super(exportFormatType, accessionList, exportAttributeList);
        String filename = "sequence";
        if (accessionList != null && accessionList.get(0) != null)
            filename += ("_" + accessionList.get(0));
        setParameter(PARAM_SUGGESTED_FILENAME, formatSuggestedFilename(filename + "." + exportFormatType));
        this.taskName = "Sequence Export Task";
    }

    public String getDataType() {
        return "Sequences";
    }

    public String getDisplayName() {
        return "Export Sequences";
    }
}