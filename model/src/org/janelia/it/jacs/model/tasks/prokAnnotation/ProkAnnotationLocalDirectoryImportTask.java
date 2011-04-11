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

package org.janelia.it.jacs.model.tasks.prokAnnotation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * This class is used by an MBean to populate the list of organism directories used for the Prok pipeline.
 * It describes dirs like /usr/local/annotation/NTPP13 to the system.
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 17, 2007
 * Time: 1:05:55 PM
 */
public class ProkAnnotationLocalDirectoryImportTask extends Task {

    public static final String DISPLAY_NAME = "Prokaryotic Annotation Import Data";

    public static final String QUERY = "query";
    public static final String PATH_TO_SOURCE_DATA = "pathToSourceData";
    public static final String GENOME_PROJECT_STATUS = "genomeProjectStatus";

//    public static final String COMPLETED_GENOME_PROJECT_STATUS = "completed";
//    public static final String UNFINISHED_GENOME_PROJECT_STATUS = "unfinished";

    public ProkAnnotationLocalDirectoryImportTask() {
        super();
    }

    public ProkAnnotationLocalDirectoryImportTask(String pathToSourceDir, String genomeProjectName, Set<Node> inputNodes,
                                                  String owner, List<Event> events, Set<TaskParameter> parameters)
            throws Exception {
        super(inputNodes, owner, events, parameters);
        // Check for a valid status
        // set the params
        setParameter(PATH_TO_SOURCE_DATA, pathToSourceDir);
        setParameter(QUERY, genomeProjectName);
        this.taskName = "Prokaryotic Annotation Import Task";
        setJobName(genomeProjectName);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(QUERY)) {
            return new TextParameterVO(value, value.length() > 500 ? value.length() : 500);
        }
        if (key.equals(GENOME_PROJECT_STATUS)) {
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