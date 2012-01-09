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

import org.janelia.it.jacs.compute.engine.data.IProcessData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for ProcessDef and SequenceDef.  A SeriesDef would be executed by an ILauncher implementation
 * at runtime ... ultimately by SeriesLauncher
 *
 * @author Tareq Nabeel
 */
public abstract class SeriesDef extends ActionDef implements Serializable {

    /**
     * Series definition can contain other action definitions
     */
    private List<ActionDef> childDefs = new ArrayList<ActionDef>();

    /**
     * The input parameters specified in the operation definition
     */
    private Set<Parameter> localInputParameters = new HashSet<Parameter>();

    /**
     * The output parameters specified in the oepration definition
     */
    private Set<Parameter> localOutputParameters = new HashSet<Parameter>();

    /**
     * Whether or not the SeriesLauncher would wait on possible asynchrounous actions within this Series
     */
    private boolean waitOnAsyncActions;

    /**
     * Amount of time in milliseconds that the SeriesLauncher would wait on possible asynchrounous
     * actions within this Series
     */
    private long maxJMSWaitTime;

    private boolean containsUpdateStatusOnSuccessAction;

    protected abstract void validateChildActionDef(ActionDef actionDef);

    protected abstract String getDefaultPojoProcessor();

    protected abstract String getDefaultLocalSlsbProcessor();


    /**
     * Returns input parameters specified for this sequence.
     *
     * @return the input parameters
     */
    public Set<Parameter> getLocalInputParameters() {
        return localInputParameters;
    }

    /**
     * Returns output parameters specified for this sequence.
     *
     * @return the output parameters
     */
    public Set<Parameter> getLocalOutputParameters() {
        return localOutputParameters;
    }

    /**
     * Whether or not we're going to wait on asynchronous actions to complete
     *
     * @return returns boolean if the definition will join on async actions
     */
    public boolean joinOnAsyncActions() {
        return waitOnAsyncActions && containsAysncActions();
    }

    /**
     * Whether or not we're going to wait on asynchronous actions to complete
     *
     * @return returns boolean if the definition will wait on async actions
     */
    public boolean isWaitOnAsyncOperations() {
        return waitOnAsyncActions;
    }

    /**
     * Sets whether or not we're going to wait on asynchronous actions to complete
     *
     * @param waitOnAsyncActions boolean whether the action should wait on async processing
     */
    protected void setWaitOnAsyncActions(boolean waitOnAsyncActions) {
        this.waitOnAsyncActions = waitOnAsyncActions;
    }

    /**
     * Adds a child action def to this Series definition
     *
     * @param actionDef an Action definition
     */
    protected void addChildDef(ActionDef actionDef) {
        validateChildActionDef(actionDef);
        childDefs.add(actionDef);
        actionDef.setParentDef(this); // this would have been set in constructor but i'm paranoid
        setContainsUpdateStatusOnSuccessAction(actionDef);
    }

    /**
     * Returns true if this process or sequence definition contains an immediate child
     * that is asynchronous
     *
     * @return true if a child is asynchronous, false otherwise
     */
    public boolean containsAysncActions() {
        // We're only interested in immediate children actions
        for (ActionDef actionDef : childDefs) {
            if (actionDef.isProcessorAsync()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this process or sequence contains only asynchronous actions
     *
     * @return returns boolean if the child actions contain only async actions
     */
    public boolean containsOnlyAysncActions() {
        // We're only interested in immediate children actions
        for (ActionDef actionDef : childDefs) {
            if (!actionDef.isProcessorAsync()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether or not this Series definition contains an operation that would update the process status.  It makes
     * sense only for the last asynchronous operation in a process to have updateProcessStatus setting
     *
     * @return returns boolean if the definition will save successful event upon completion
     */
    public boolean containsUpdateStatusOnSuccessAction() {
        return this.containsUpdateStatusOnSuccessAction;
    }

    /**
     * Sets whether or not this Series definition contains an operation that would update the process status.  It makes
     * sense only for the last asynchronous operation in a process to have updateProcessStatus setting
     *
     * @param containsUpdateStatusOnSuccessAction
     *         boolean whether the definition will update status on success
     */
    protected void setContainsUpdateStatusOnSuccessAction(boolean containsUpdateStatusOnSuccessAction) {
        this.containsUpdateStatusOnSuccessAction = containsUpdateStatusOnSuccessAction;
    }

    /**
     * Returns the amount of time in milliseconds that the SeriesLauncher would wait on possible asynchrounous
     * actions within this Series
     *
     * @return returns a long of the max JMS wait time
     */
    public long getMaxJMSWaitTime() {
        return maxJMSWaitTime;
    }

    /**
     * Sets the amount of time in milliseconds that the SeriesLauncher would wait on possible asynchronous
     * actions within this Series
     *
     * @param maxJMSWaitTime the max amount of time that SeriesLauncher will wait on asynchronous actions to complete
     */
    protected void setMaxJMSWaitTime(long maxJMSWaitTime) {
        this.maxJMSWaitTime = maxJMSWaitTime;
    }

    /**
     * Returns the first operation within this series
     *
     * @return the first operation definition
     */
    public OperationDef getFirstOperation() {
        return getFirstOperation(getChildActionDefs());
    }

    /**
     * Returns the first operation within this series
     *
     * @param actionDefs a list of actions defined
     * @return the first operation definition
     */
    private OperationDef getFirstOperation(List<ActionDef> actionDefs) {
        for (ActionDef actionDef : actionDefs) {
            if (actionDef.isSeriesDef()) {
                SeriesDef seriesDef = (SeriesDef) actionDef;
                return getFirstOperation(seriesDef.getChildActionDefs());
            }
            else if (actionDef.isOperation()) {
                return (OperationDef) actionDef;
            }
        }
        return null;
    }


    public Set<Parameter> getInputParameters() {
        Set<Parameter> parameters = new HashSet<Parameter>(localInputParameters);
        getAllParameters(getChildActionDefs(), parameters, true);
        return parameters;
    }

    public Set<Parameter> getOutputParameters() {
        Set<Parameter> parameters = new HashSet<Parameter>(localOutputParameters);
        getAllParameters(getChildActionDefs(), parameters, false);
        return parameters;
    }

    private Set<Parameter> getAllParameters(List<ActionDef> actionDefs, Set<Parameter> parameters, boolean input) {
        for (ActionDef actionDef : actionDefs) {
            if (actionDef.isSeriesDef()) {
                SeriesDef seriesDef = (SeriesDef) actionDef;
                getAllParameters(seriesDef.getChildActionDefs(), parameters, input);
            }
            else if (actionDef.isOperation()) {
                parameters.addAll(input ? actionDef.getInputParameters() : actionDef.getOutputParameters());
            }
        }
        return null;
    }

    /**
     * For this process or sequence to be executable, it must be executable based on it's
     * own attributes and it must contain at least one action that is executable.  This is to avoid
     * unncessary creation of a SeriesLauncher
     *
     * @return true if this series is executable, false otherwise
     */
    public boolean isExecutable(IProcessData processData) {
        // For this SeriesDef to be executed, it must be executable itself
        // and must contain at least one executable action
        if (super.isExecutable(processData)) {
            for (ActionDef actionDef : childDefs) {
                if (actionDef.isExecutable(processData)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return the child actions of this Series defintiion
     *
     * @return the child actions
     */
    public List<ActionDef> getChildActionDefs() {
        return childDefs;
    }

    public String toString() {
        return super.toString() +
                ",waitOnAsyncActions=" + waitOnAsyncActions + ",containsUpdateStatusOnSuccessAction=" + containsUpdateStatusOnSuccessAction;
    }

    /**
     * Sets the containsUpdateStatusOperation attribute for this series defintiion
     *
     * @param actionDef The operation or sequence definition that we're about to add
     */
    private void setContainsUpdateStatusOnSuccessAction(ActionDef actionDef) {
        if (actionDef.updateProcessStatusOnSuccess()) {
            SeriesDef parentDef = actionDef.getParentDef();
            while (parentDef != null) {
                parentDef.setContainsUpdateStatusOnSuccessAction(true);
                parentDef = parentDef.getParentDef();
//                if (parentDef.isProcess()) {
//                    // Subprocesses should be responsible for managing it's
//                    // own status.. so we break out of the loop so that inner process
//                    // containsUpdateStatusOperation and outer process does not
//                    // For outer process to containUpdateStatusOperation, it needs
//                    // to contain a sequence that contains such an operation
//                    parentDef.setContainsUpdateStatusAction(true);
//                    break;
//                }
            }
        }
    }
}
