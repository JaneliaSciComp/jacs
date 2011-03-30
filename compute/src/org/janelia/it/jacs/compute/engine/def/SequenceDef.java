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

import org.janelia.it.jacs.compute.engine.launcher.SequenceLauncher;

/**
 * This class represents the definition of a sequence within a sequence or process definition.  A sequence
 * action is executed by an ILauncher implementation at runtime ... ultimately by SequenceLauncher
 *
 * @author Tareq Nabeel
 */
public class SequenceDef extends SeriesDef {
    private static final ProcessorType DEFAULT_PROCESSOR_TYPE = ProcessorType.POJO;
    private static final String DEFAULT_LOCAL_MDB_PROCESSOR = "queue/AnonymousSequenceLauncher";
    private static final String DEFAULT_POJO_PROCESSOR = SequenceLauncher.class.getName();
    private static final String DEFAULT_LOCAL_SLSB_PROCESSOR = "compute/SequenceLauncherSLSB/local";

    /**
     * Logic for initializing Process defintiion should be contained within a DefLoader
     *
     * @param seriesDef series defintion passed in
     */
    protected SequenceDef(SeriesDef seriesDef) {
        if (seriesDef == null) {
            throw new IllegalArgumentException("Sequence has to be contained within a process or sequence");
        }
        setParentDef(seriesDef);
    }

    /**
     * Used by DefLoader
     */
    protected void validateChildActionDef(ActionDef actionDef) {
        if (actionDef == null) {
            throw new IllegalArgumentException("Child action def cannot be null");
        }
        if (!actionDef.isOperation() && !actionDef.isSequence()) {
            throw new IllegalArgumentException("Process can only contain operations and other sequences");
        }
    }

    /**
     * Adds a child action def to this Series definition
     *
     * @param actionDef an Action definition
     */
    protected void addChildDef(ActionDef actionDef) {
        super.addChildDef(actionDef);
    }

    /**
     * Returns action type represented by seqeunces
     *
     * @return action type represented by seqeunces
     */
    public ActionType getActionType() {
        return ActionType.SEQUENCE;
    }

    /**
     * Returns the default processor type for seqeunces
     */
    protected ProcessorType getDefaultProcessorType() {
        return DEFAULT_PROCESSOR_TYPE;
    }

    /**
     * Returns the default mdb processor for seqeunces
     */
    protected String getDefaultMdbProcessor() {
        return DEFAULT_LOCAL_MDB_PROCESSOR;
    }

    /**
     * Returns the default pojo processor for seqeunces
     */
    protected String getDefaultPojoProcessor() {
        return DEFAULT_POJO_PROCESSOR;
    }

    /**
     * Returns the default stateless session bean processor for seqeunces
     */
    protected String getDefaultLocalSlsbProcessor() {
        return DEFAULT_LOCAL_SLSB_PROCESSOR;
    }

}
