
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

    private boolean included = false;
    
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

    public boolean isIncluded() {
		return included;
	}

	public void setIncluded(boolean included) {
		this.included = included;
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
