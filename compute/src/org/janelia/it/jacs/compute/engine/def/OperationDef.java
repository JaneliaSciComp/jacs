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

package org.janelia.it.jacs.compute.engine.def;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents the definition of an operation within a sequence definition.  An operation
 * action is executed by an IService implementation at runtime
 *
 * @author Tareq Nabeel
 */
public class OperationDef extends ActionDef {
    private static final ProcessorType DEFAULT_PROCESSOR_TYPE = ProcessorType.POJO;
    private static final String DEFAULT_LOCAL_MDB_PROCESSOR = "queue/baseService";

    /**
     * The input parameters specified in the operation definition
     */
    private Set<Parameter> inputParameters = new HashSet<Parameter>();

    /**
     * The output parameters specified in the oepration definition
     */
    private Set<Parameter> outputParameters = new HashSet<Parameter>();


    /**
     * Logic for initializing Process defintiion should be contained within a DefLoader
     */
    private OperationDef() {
    }

    /**
     * Logic for initializing Process defintiion should be contained within a DefLoader
     */
    protected OperationDef(SequenceDef sequenceDef) {
        if (sequenceDef == null) {
            throw new IllegalArgumentException("Operation has to be contained within sequence");
        }
        setParentDef(sequenceDef);
    }

    /**
     * Returns input parameters specified for this operation in the process definition
     *
     * @return the input parameters
     */
    public Set<Parameter> getInputParameters() {
        return inputParameters;
    }

    /**
     * Returns output parameters specified for this operation in the process definition
     *
     * @return the output parameters
     */
    public Set<Parameter> getOutputParameters() {
        return outputParameters;
    }

    /**
     * Returns action type represented by operations
     *
     * @return action type represented by operations
     */
    public ActionType getActionType() {
        return ActionType.OPERATION;
    }

    public String getDefaultMdbProcessor() {
        return DEFAULT_LOCAL_MDB_PROCESSOR;
    }

    /**
     * @return the default processor type represented by operations
     */
    protected ProcessorType getDefaultProcessorType() {
        return DEFAULT_PROCESSOR_TYPE;
    }

    /**
     * @return String representation of the operation definition
     */
    public String toString() {
        return super.toString();
    }


}
