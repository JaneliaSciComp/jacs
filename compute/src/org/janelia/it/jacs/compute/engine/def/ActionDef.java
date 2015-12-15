package org.janelia.it.jacs.compute.engine.def;

import org.janelia.it.jacs.compute.engine.data.IProcessData;

import java.io.Serializable;
import java.util.Set;

/**
 * Base class for all the different actions within a workflow execution
 *
 * @author Tareq Nabeel
 */
public abstract class ActionDef implements Serializable {

    /**
     * Name of the action e.g. "Blast" in <seqeunce name="Blast"
     */
    private String name;

    /**
     * Type of processor used to process this action
     */
    private ProcessorType processorType;

    /**
     * The name of the processor used to execute this action.
     * e.g. This would be the jndi queue name for LOCAL_MDB/REMOTE_MDB processorType,
     * the jndi SLSB name for LOCAL_SLSB/REMOTE_SLSB and class name for POJO processorType
     */
    private String processorName;

    /**
     * This would be the jndi queue name to forward this action to for processing.  It's used when the processorType
     * for this action is LOCAL_MDB
     */
    private String queueToLinkTo;

    /**
     * This would be the queue that this action was forwarded from
     * Every queueToLinkTo has to have a queueToLinkFrom within a process.
     * If set, this action will be skipped by the launchers as it would be picked
     * up by an MDB
     */
    private String queueToLinkFrom;

    /**
     * If specified, this condition must be met for the action to be executed
     */
    private Condition processIfCondition;

    /**
     * If specified, this action will be executed once for each occurrence
     * of forEachParam in process data.
     */
    private String forEachParam;

    /**
     * If specified, this action will be executed as long as this condition is met
     */
    private Condition loopUntilCondition;

    /**
     * The parent definition
     */
    private transient SeriesDef parentDef;

    /**
     * Whether or not the process or sequence should halt if an operation fails
     */
    protected boolean haltProcessOnError = true;

    /**
     * Needed at this point to update the process/task status if one of the containing
     * sequences or process was launched asynchronously
     */
    protected StatusUpdate statusUpdateType = StatusUpdate.ON_FAILURE;


    /**
     * Optional event that would get persisted before an operation or sequence is launched
     * It is needed because if an operation were to be launched asynchronously and that operation
     * was INTERNALLY creating an event, we would get duplicate-event-constraint-violation exceptions
     * even if we checked for pre-existence of event in DAO (because they're running in separate tx)
     * The solution is to move status update functionality to the framework
     */
    protected String startEvent;

    /**
     * process, sequence, or operation at this point
     *
     * @return the type of action as an emum
     */
    public abstract ActionType getActionType();


    protected abstract String getDefaultMdbProcessor();

    /**
     * Every action should have a default processor type.  It's POJO
     * for now
     *
     * @return the default processor type as an emum
     */
    protected abstract ProcessorType getDefaultProcessorType();

    /**
     * Returns input parameters specified for this action
     *
     * @return the input parameters
     */
    public abstract Set<Parameter> getInputParameters();

    /**
     * Returns output parameters specified for this action
     *
     * @return the output parameters
     */
    public abstract Set<Parameter> getOutputParameters();

    /**
     * Return true if this action is an operation, false otherwise
     *
     * @return true if this action is an operation, false otherwise
     */
    public boolean isOperation() {
        return getActionType().equals(ActionType.OPERATION);
    }

    /**
     * Return true if this action is a sequence, false otherwise
     *
     * @return true if this action is a sequence, false otherwise
     */
    public boolean isSequence() {
        return getActionType().equals(ActionType.SEQUENCE);
    }

    /**
     * Return true if this action is a process, false otherwise
     *
     * @return true if this action is a process, false otherwise
     */
    public boolean isProcess() {
        return getActionType().equals(ActionType.PROCESS);
    }

    /**
     * Return true if this action is a sequence or a process, false otherwise
     *
     * @return true if this action is a sequence or a process, false otherwise
     */
    public boolean isSeriesDef() {
        return isSequence() || isProcess();
    }

    /**
     * Return the name of the action e.g. "Blast" in <seqeunce name="Blast"
     *
     * @return name of the action e.g. "Blast" in <seqeunce name="Blast"
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this action
     *
     * @param name of the action e.g. "Blast" in <seqeunce name="Blast"
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Return the type of processor used to process this action
     *
     * @return Type of processor used to process this action
     */
    public ProcessorType getProcessorType() {
        return processorType;
    }

    /**
     * Sets the processorType of this action
     *
     * @param processorType type of processor used to process this action
     */
    protected void setProcessorType(ProcessorType processorType) {
        this.processorType = processorType;
    }

    /**
     * Returns true if this action is processed asynchronously, false otherwise
     *
     * @return whether or not this action is sent to a queue and processed by an MDB
     */
    public boolean isProcessorAsync() {
        return ProcessorType.LOCAL_MDB.equals(getProcessorType()) ||
                ProcessorType.REMOTE_MDB.equals(getProcessorType());
    }

    /**
     * Return name of the processor used to execute this action.
     * e.g. This would be the queue name for LOCAL_MDB/REMOTE_MDB processorType,
     * the SLSB name for LOCAL_SLSB/REMOTE_SLSB and class name for POJO processorType
     *
     * @return the name of the processor
     */
    public String getProcessorName() {
        return processorName;
    }

    /**
     * Set the processor name for this action
     *
     * @param processorName the processor name for this action
     */
    protected void setProcessorName(String processorName) {
        this.processorName = processorName;
    }

    /**
     * Return the queue to forward this action to for processing.  It's used when the processorType
     * for this action is LOCAL_MDB
     *
     * @return the name of the queue as a String
     */
    public String getQueueToLinkTo() {
        return queueToLinkTo;
    }

    /**
     * Sets the queue to forward this action to for processing.  It's used when the processorType
     * for this action is LOCAL_MDB
     *
     * @param queueToLinkTo the jndi name of the queue as a String
     */
    protected void setQueueToLinkTo(String queueToLinkTo) {
        this.queueToLinkTo = queueToLinkTo;
    }

    /**
     * Returns the jndi queue name that this action was forwarded from
     * Every queueToLinkTo has to have a queueToLinkFrom within a process.
     * If set, this action will be skipped by the launchers as it would be picked
     * up by an MDB
     *
     * @return the name of the queue
     */
    public String getQueueToLinkFrom() {
        return queueToLinkFrom;
    }

    /**
     * Set the jndi queue name that this action was forwarded from
     * Every queueToLinkTo has to have a queueToLinkFrom within a process.
     * If set, this action will be skipped by the launchers as it would be picked
     * up by an MDB
     *
     * @param queueToLinkFrom tehe name of the queue
     */
    protected void setQueueToLinkFrom(String queueToLinkFrom) {
        this.queueToLinkFrom = queueToLinkFrom;
    }

    /**
     * Return the if condition that must be met for the action to be executed
     *
     * @return the if condition
     */
    public Condition getProcessIfCondition() {
        return processIfCondition;
    }

    /**
     * Sets the condition that must be met for the action to be executed
     *
     * @param processIfCondition the if condition
     */
    protected void setProcessIfCondition(Condition processIfCondition) {
        this.processIfCondition = processIfCondition;
    }

    /**
     * Return the parameter name that this action would be executed once for each occurrence
     * of it in process data.
     *
     * @return the parameter name
     */
    public String getForEachParam() {
        return forEachParam;
    }

    /**
     * Set the parameter name that this action would be executed once for each occurence
     * of it in process data.
     *
     * @param forEachParam the parameter name
     */
    public void setForEachParam(String forEachParam) {
        this.forEachParam = forEachParam;
    }

    /**
     * Return the condition that this action will be executed iteratively when this condition is met
     *
     * @return the loop until condition
     */
    public Condition getLoopUntilCondition() {
        return loopUntilCondition;
    }

    /**
     * Set the condition that this action will be executed iteratively when this condition is met
     *
     * @param loopUntilCondition the loop until condition
     */
    protected void setLoopUntilCondition(Condition loopUntilCondition) {
        this.loopUntilCondition = loopUntilCondition;
    }

    /**
     * Returns the parent definition
     *
     * @return the parent definition
     */
    public SeriesDef getParentDef() {
        return parentDef;
    }

    /**
     * Sets the parent definition
     *
     * @param parentDef the parent definition
     */
    protected void setParentDef(SeriesDef parentDef) {
        this.parentDef = parentDef;
    }

    /**
     * Returns true if this action should be executed, false otherwise
     *
     * @param processData needed to evaluate if condition
     * @return true if this action is to be executed, false otherwise
     */
    public boolean isExecutable(IProcessData processData) {
        return queueToLinkFrom == null && (getProcessIfCondition() == null || getProcessIfCondition().isSatisfiedBy(processData));
    }

    /**
     * Whether or not the process or sequence should halt if an operation or series fails
     *
     * @return true if process or sequence should halt if an operation or series fails, false otherwise
     */
    public boolean haltProcessOnError() {
        return haltProcessOnError;
    }

    /**
     * Sets whether or not the process or sequence should halt if an operation or series fails
     *
     * @param haltProcessOnError true if process or sequence should halt if an operation or series fails, false otherwise
     */
    public void setHaltProcessOnError(boolean haltProcessOnError) {
        this.haltProcessOnError = haltProcessOnError;
    }

    /**
     * @return string representation of this action
     */
    public String toString() {
        return "\n" + getActionType() + ": name=" + name + ",processorType=" + processorType +
                ",queueToLinkFrom=" + queueToLinkFrom + ",queueToLinkTo=" + queueToLinkTo +
                ",forEachParam=" + forEachParam + ",loopUntilCondition=" + loopUntilCondition + ",haltProcessOnError=" + haltProcessOnError + ",statusUpdateType=" + statusUpdateType;
    }

    /**
     * Returns true if this operation updates the process/task status, false otherwise
     *
     * @return boolean if status success
     */
    public boolean updateProcessStatusOnSuccess() {
        return statusUpdateType == StatusUpdate.ON_SUCCESS;
    }

    /**
     * Returns true if this operation updates the process/task status on failure, false otherwise
     *
     * @return boolean if status failure
     */
    public boolean updateProcessStatusOnFailure() {
        return statusUpdateType == StatusUpdate.ON_FAILURE;
    }

    /**
     * Sets whether or not this operation updates the process status
     *
     * @param statusUpdateType Needed at this point to update the process/task status if one of the containing
     *                         sequences or process was launched asynchronously
     */
    protected void setStatusUpdateType(StatusUpdate statusUpdateType) {
        this.statusUpdateType = statusUpdateType;
    }


    public String getStartEvent() {
        return startEvent;
    }

    /**
     * Optional event that would get persisted before an operation or sequence is launched
     * It is needed because if an operation were to be launched asynchronously and that operation
     * was INTERNALLY creating an event, we would get duplicate-event-constraint-violation exceptions
     * even if we checked for pre-existence of event in DAO (because they're running in separate tx)
     * The solution is to move status update functionality to the framework
     * @param startEvent start event string
     */
    public void setStartEvent(String startEvent) {
        this.startEvent = startEvent;
    }

}
