
package org.janelia.it.jacs.model.tasks.blast;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

/**
 * @author Michael Press
 */
public class BlastNTask extends BlastTask {
    transient public static final String BLASTN_NAME = "blastn";
    transient public static final String DISPLAY_BLASTN = "BLASTN (nuc/nuc)";

    // Default values - default overrides
    transient public static final Long wordsize_BLASTN_DEFAULT = (long) 11;
    transient public static final Long gappedAlignmentDropoff_BLASTN_DEFAULT = (long) 30;
    transient public static final Long hitExtensionThreshold_BLASTN_DEFAULT = (long) 0;
    transient public static final Double ungappedExtensionDropoff_BLASTN_DEFAULT = 20.0;
    transient public static final Double finalGappedDropoff_BLASTN_DEFAULT = 50.0;
    transient public static final Long multiHitWindowSize_BLASTN_DEFAULT = (long) 0;

    // Custom defaults
    transient public static final String searchStrand_DEFAULT = "both";
    transient public static final Boolean gappedAlignment_DEFAULT = Boolean.TRUE;
    transient public static final Long blastnMismatchPenalty_DEFAULT = (long) -3;
    transient public static final Long blastnMatchReward_DEFAULT = (long) 1;

    // Parameter Keys
/*
    transient public static final String PARAM_mismatchPenalty = "mismatch penalty";//-q)";
    transient public static final String PARAM_matchReward = "match reward";//-r)";
    transient public static final String PARAM_gappedAlignment = "gapped alignment";//-g)";
    transient public static final String PARAM_searchStrand = "search strand";//-S)";
*/

    public BlastNTask() {
        super();
        setParameter(PARAM_wordsize, wordsize_BLASTN_DEFAULT.toString());
        setParameter(PARAM_gappedAlignmentDropoff, gappedAlignmentDropoff_BLASTN_DEFAULT.toString());
        setParameter(PARAM_hitExtensionThreshold, hitExtensionThreshold_BLASTN_DEFAULT.toString());
        setParameter(PARAM_ungappedExtensionDropoff, ungappedExtensionDropoff_BLASTN_DEFAULT.toString());
        setParameter(PARAM_finalGappedDropoff, finalGappedDropoff_BLASTN_DEFAULT.toString());
        setParameter(PARAM_multiHitWindowSize, multiHitWindowSize_BLASTN_DEFAULT.toString());
        setParameter(PARAM_mismatchPenalty, blastnMismatchPenalty_DEFAULT.toString());
        setParameter(PARAM_matchReward, blastnMatchReward_DEFAULT.toString());
        setParameter(PARAM_gappedAlignment, gappedAlignment_DEFAULT.toString());
        setParameter(PARAM_searchStrand, searchStrand_DEFAULT);

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
        if (key.equals(PARAM_searchStrand))
            return new SingleSelectVO(getSearchStrandList(), value);
        // No match
        return null;
    }

    /**
     * full constructor
     */
    public BlastNTask(Set<Node> inputNodes,
                      String owner,
                      List<Event> events,
                      Set<TaskParameter> parameters) {
        super(inputNodes, owner, events, parameters);
        constructorCommon();
    }

    public String getDisplayName() {
        return DISPLAY_BLASTN;
    }

    private void constructorCommon() {
        this.taskName = BLASTN_NAME;
    }

    public String generateCommandStringNotIncludingIOParams() throws ParameterException {
        StringBuffer sb = new StringBuffer(super.generateDefaultCommandStringNotIncludingIOParams());
        sb.append("-q ").append(((getParameterVO(PARAM_mismatchPenalty))).getStringValue()).append(" ");
        sb.append("-r ").append(((getParameterVO(PARAM_matchReward))).getStringValue()).append(" ");
        sb.append("-g ").append(((getParameterVO(PARAM_gappedAlignment))).getStringValue().equals("true") ? "T" : "F").append(" ");
        sb.append("-S ").append(searchStrandTranslator(((getParameterVO(PARAM_searchStrand))).getStringValue())).append(" ");
        return sb.toString();
    }

    // IBlastOutputFormatTask interface implementations
    // override of default getMatchReward and getMismatchPenalty methods to use BlastNTask specific params
    public BigInteger getMatchReward() throws ParameterException {
        BigInteger matchReward = null;

        ParameterVO matchRewardParam = getParameterVO(PARAM_matchReward);
        if (matchRewardParam != null) {
            matchReward = new BigInteger(matchRewardParam.getStringValue());
        }
        return matchReward;
    }

    public BigInteger getMismatchPenalty() throws ParameterException {
        BigInteger mismatchPenalty = null;

        ParameterVO mismatchPenaltyParam = getParameterVO(PARAM_mismatchPenalty);
        if (mismatchPenaltyParam != null) {
            mismatchPenalty = new BigInteger(mismatchPenaltyParam.getStringValue());
        }
        return mismatchPenalty;
    }
}
