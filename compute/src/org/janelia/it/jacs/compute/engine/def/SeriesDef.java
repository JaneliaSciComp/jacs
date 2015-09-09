
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
     * Exception handler sequence to use in case of an exception within this series
     */
    private SequenceDef exceptionHandlerDef = null;
    
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
        setContainsUpdateStatusOnSuccessActionFromActionDef(actionDef);
    }

    /**
     * Sets the exception handler for this series.
     */
    public void setExceptionHandlerDef(SequenceDef exceptionHandlerDef) {
		this.exceptionHandlerDef = exceptionHandlerDef;
	}

    /**
     * Returns the exception handler for this series.
     */
    public SequenceDef getExceptionHandlerDef() {
		return exceptionHandlerDef;
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

    public Set<Parameter> getInputParameters() {
        Set<Parameter> parameters = new HashSet<Parameter>(localInputParameters);
        for (ActionDef actionDef : getChildActionDefs()) {
            parameters.addAll(actionDef.getInputParameters());
        }
        return parameters;
    }

    public Set<Parameter> getOutputParameters() {
        Set<Parameter> parameters = new HashSet<Parameter>(localOutputParameters);
        for (ActionDef actionDef : getChildActionDefs()) {
            parameters.addAll(actionDef.getOutputParameters());
        }
        return parameters;
    }

    /**
     * For this process or sequence to be executable, it must be executable based on it's
     * own attributes and it must contain at least one action that is executable.  This is to avoid
     * unnecessary creation of a SeriesLauncher
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
    private void setContainsUpdateStatusOnSuccessActionFromActionDef(ActionDef actionDef) {
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
