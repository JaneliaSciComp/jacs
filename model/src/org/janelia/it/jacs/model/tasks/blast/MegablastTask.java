
package org.janelia.it.jacs.model.tasks.blast;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.List;
import java.util.Set;

public class MegablastTask extends BlastTask {
    transient public static final String MEGABLAST_NAME = "megablast";
    transient public static final String DISPLAY_MEGABLAST = "MEGABLAST (nuc/nuc)";

    // Default values
    transient public static final Long wordsize_MEGABLAST_DEFAULT = (long) 28;
    transient public static final Long gappedAlignmentDropoff_MEGABLAST_DEFAULT = (long) 20;
    transient public static final Long hitExtensionThreshold_MEGABLAST_DEFAULT = (long) 0;
    transient public static final Double ungappedExtensionDropoff_MEGABLAST_DEFAULT = 20.0;
    transient public static final Double finalGappedDropoff_MEGABLAST_DEFAULT = 50.0;
    transient public static final Long multiHitWindowSize_MEGABLAST_DEFAULT = (long) 0;

    // Custom defaults
    transient public static final Boolean gappedAlignment_DEFAULT = Boolean.TRUE;
    transient public static final Long blastnMismatchPenalty_DEFAULT = (long) -3;
    transient public static final Long blastnMatchReward_DEFAULT = (long) 1;

    // Parameter Keys
/*
    transient public static final String PARAM_mismatchPenalty = "mismatch penalty (-q)";
    transient public static final String PARAM_matchReward = "match reward (-r)";
    transient public static final String PARAM_gappedAlignment = "gapped alignment (-g)";
*/

    public MegablastTask() {
        super();
        setParameter(PARAM_wordsize, wordsize_MEGABLAST_DEFAULT.toString());
        setParameter(PARAM_gappedAlignmentDropoff, gappedAlignmentDropoff_MEGABLAST_DEFAULT.toString());
        setParameter(PARAM_hitExtensionThreshold, hitExtensionThreshold_MEGABLAST_DEFAULT.toString());
        setParameter(PARAM_ungappedExtensionDropoff, ungappedExtensionDropoff_MEGABLAST_DEFAULT.toString());
        setParameter(PARAM_finalGappedDropoff, finalGappedDropoff_MEGABLAST_DEFAULT.toString());
        setParameter(PARAM_multiHitWindowSize, multiHitWindowSize_MEGABLAST_DEFAULT.toString());
        setParameter(PARAM_mismatchPenalty, blastnMismatchPenalty_DEFAULT.toString());
        setParameter(PARAM_matchReward, blastnMatchReward_DEFAULT.toString());
        setParameter(PARAM_gappedAlignment, gappedAlignment_DEFAULT.toString());

        constructorCommon();
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        ParameterVO pvo = super.getParameterVO(key);
        if (pvo != null)
            return pvo;
        if (key.equals(PARAM_wordsize))
            return new LongParameterVO(new Long(value));
        if (key.equals(PARAM_gappedAlignmentDropoff))
            return new LongParameterVO(new Long(value));
        if (key.equals(PARAM_hitExtensionThreshold))
            return new LongParameterVO(new Long(value));
        if (key.equals(PARAM_ungappedExtensionDropoff))
            return new DoubleParameterVO(new Double(value));
        if (key.equals(PARAM_finalGappedDropoff))
            return new DoubleParameterVO(new Double(value));
        if (key.equals(PARAM_multiHitWindowSize))
            return new LongParameterVO(new Long(value));
        if (key.equals(PARAM_mismatchPenalty))
            return new LongParameterVO(new Long(value));
        if (key.equals(PARAM_matchReward))
            return new LongParameterVO(new Long(value));
        if (key.equals(PARAM_gappedAlignment))
            return new BooleanParameterVO(Boolean.valueOf(value));
        // No match
        return null;
    }

    /**
     * full constructor
     */
    public MegablastTask(Set<Node> inputNodes,
                         String owner,
                         List<Event> events,
                         Set<TaskParameter> parameters) {
        super(inputNodes, owner, events, parameters);
        constructorCommon();
    }

    public String getDisplayName() {
        return DISPLAY_MEGABLAST;
    }

    private void constructorCommon() {
        this.taskName = MEGABLAST_NAME;
    }

    public String generateCommandStringNotIncludingIOParams() throws ParameterException {
        StringBuffer sb = new StringBuffer(generateDefaultCommandStringNotIncludingIOParams());
        sb.append("-q ").append((getParameterVO(PARAM_mismatchPenalty)).getStringValue()).append(" ");
        sb.append("-r ").append((getParameterVO(PARAM_matchReward)).getStringValue()).append(" ");
        sb.append("-g ").append((getParameterVO(PARAM_gappedAlignment)).getStringValue().equals("true") ? "T" : "F").append(" ");
        return sb.toString();
    }

}
