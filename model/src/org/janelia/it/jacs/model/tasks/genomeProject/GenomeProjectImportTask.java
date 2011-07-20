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

package org.janelia.it.jacs.model.tasks.genomeProject;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 17, 2007
 * Time: 1:05:55 PM
 */
public class GenomeProjectImportTask extends Task {

    public static final String DISPLAY_NAME = "Genome Project Import Data";

    public static final String PARAM_PATH_TO_SOURCE_DATA = "pathToSourceData";
    public static final String PARAM_GENOME_PROJECT_NAME = "genomeProjectName";
    public static final String PARAM_GENBANK_FILE_LIST = "genbankFileList";
    public static final String PARAM_GENOME_PROJECT_STATUS = "genomeProjectStatus";

    public static final String COMPLETE_GENOME_PROJECT_STATUS = "complete";
    public static final String DRAFT_GENOME_PROJECT_STATUS = "draft";


    public GenomeProjectImportTask() {
        super();
    }

    public GenomeProjectImportTask(String pathToSourceDir, String genomeProjectName, String projectStatus,
                                   List genbankFileNames, Set<Node> inputNodes, String owner, List<Event> events,
                                   Set<TaskParameter> parameters)
            throws Exception {
        super(inputNodes, owner, events, parameters);
        // Check for a valid status
        if (!COMPLETE_GENOME_PROJECT_STATUS.equalsIgnoreCase(projectStatus) &&
                !DRAFT_GENOME_PROJECT_STATUS.equalsIgnoreCase(projectStatus)) {
            throw new Exception("Not a valid Genome Project status");
        }

        // set the params
        setParameter(PARAM_PATH_TO_SOURCE_DATA, pathToSourceDir);
        setParameter(PARAM_GENOME_PROJECT_NAME, genomeProjectName);
        setParameter(PARAM_GENOME_PROJECT_STATUS, projectStatus);
        setParameter(PARAM_GENBANK_FILE_LIST, csvStringFromCollection(genbankFileNames));
        this.taskName = "Genome Project Import Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_GENOME_PROJECT_NAME)) {
            return new TextParameterVO(value, value.length() > 500 ? value.length() : 500);
        }
        if (key.equals(PARAM_GENOME_PROJECT_STATUS)) {
            Boolean tmpValue = Boolean.valueOf(value);
            return new BooleanParameterVO(tmpValue);
        }
        if (key.equals(PARAM_GENBANK_FILE_LIST)) {
            // NOTE: if this directory grows, might have to make a distinction between NC_ (Genbank analysis) and
            // CP (RefSeq) Genbank files.  Both will have extension .gbk.
            return new MultiSelectVO(Task.listOfStringsFromCsvString(value), Task.listOfStringsFromCsvString(value));
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }
}