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
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 17, 2007
 * Time: 1:05:55 PM
 */
public class GenomeProjectUpdateTask extends Task {

    public static final String DISPLAY_NAME = "Genome Project Update";
    public static final String PROJECT_MODE_BACTERIAL = "bacterial";
    public static final String PROJECT_MODE_VIRAL = "viral";
    public static final String PROJECT_MODE_DRAFT_BACTERIAL = "draft bacterial";

    private static final String COMPLETED_GENOME_PROJECT_STATUS = "completed";
    public static final String COMPLETE_GENOME_PROJECT_STATUS = "complete";
    public static final String DRAFT_GENOME_PROJECT_STATUS = "draft";

    public static final String PARAM_PROJECT_MODE = "projectMode";
    public static final String PARAM_GENOME_PROJECT_STATUS = "genomeProjectStatus";

    public GenomeProjectUpdateTask() {
        super();
    }

    public GenomeProjectUpdateTask(String projectMode, String genomeProjectStatus, Set<Node> inputNodes, String owner,
                                   List<Event> events, Set<TaskParameter> parameters)
            throws Exception {
        super(inputNodes, owner, events, parameters);
        // Check for a valid status
        if ( genomeProjectStatus.equalsIgnoreCase(COMPLETED_GENOME_PROJECT_STATUS) )  {
            genomeProjectStatus = COMPLETE_GENOME_PROJECT_STATUS;
        }
        if (!COMPLETE_GENOME_PROJECT_STATUS.equalsIgnoreCase(genomeProjectStatus) &&
                !DRAFT_GENOME_PROJECT_STATUS.equalsIgnoreCase(genomeProjectStatus)) {
            throw new Exception("Not a valid Genome Project status");
        }

        // set the params
        setParameter(PARAM_PROJECT_MODE, projectMode);
        setParameter(PARAM_GENOME_PROJECT_STATUS, projectMode);
        this.taskName = "Genome Project Update Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_GENOME_PROJECT_STATUS)) {
            Boolean tmpValue = Boolean.valueOf(value);
            return new BooleanParameterVO(tmpValue);
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }
}