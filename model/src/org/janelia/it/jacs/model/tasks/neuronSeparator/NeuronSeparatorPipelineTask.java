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

package org.janelia.it.jacs.model.tasks.neuronSeparator;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 */
public class NeuronSeparatorPipelineTask extends Task {
    transient public static final String TASK_NAME = "neuronSeparatorPipeline";
    transient public static final String DISPLAY_NAME = "Neuron Separation Pipeline";
    // Sample input file

    // Parameter Keys - Subset of the params needed by the child tasks of this pipeline

    // Note: Only one of these parameters should be populated
    transient public static final String PARAM_inputLsmFilePathList = "input lsm file path list";
    transient public static final String PARAM_inputLsmEntityIdList = "input lsm entity id list";
    transient public static final String PARAM_outputSampleEntityId = "output sample entity id";

    // Default values - default overrides

    public NeuronSeparatorPipelineTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public NeuronSeparatorPipelineTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_inputLsmFilePathList, "");
        setParameter(PARAM_inputLsmEntityIdList, "");
        setParameter(PARAM_outputSampleEntityId, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_inputLsmFilePathList)) {
            return new TextParameterVO(value,1000);
        }
        else if (key.equals(PARAM_inputLsmEntityIdList)) {
            return new TextParameterVO(value, 1000);
        }
        else if (key.equals(PARAM_outputSampleEntityId)) {
            return new TextParameterVO(value, 1000);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return true;
    }

}