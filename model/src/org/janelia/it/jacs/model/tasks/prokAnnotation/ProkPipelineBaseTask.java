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

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 4, 2010
 * Time: 4:54:37 PM
 */
public abstract class ProkPipelineBaseTask extends Task {
    transient public static final String PARAM_DB_USERNAME = "dbUsername";
    transient public static final String PARAM_DB_PASSWORD = "dbPassword";
    transient public static final String PARAM_DB_NAME = "dbName";
    transient public static final String PARAM_DIRECTORY = "directory";

    public ProkPipelineBaseTask() {
        super();
    }

    protected ProkPipelineBaseTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
    }

    protected void setDefaultValues() {
        setParameter(PARAM_DB_NAME, null);
        setParameter(PARAM_DB_USERNAME, null);
        setParameter(PARAM_DB_PASSWORD, null);
        setParameter(PARAM_DIRECTORY, null);
    }

}
