
package org.janelia.it.jacs.model.tasks.blast;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.List;
import java.util.Set;

/**
 * @author Michael Press
 */
public class BlastXTask extends BlastTask {
    transient public static final String BLASTX_NAME = "blastx";
    transient public static final String DISPLAY_BLASTX = "BLASTX (nuc/prot)";

    // Default values - default overrides
    transient public static final Long wordsize_BLASTX_DEFAULT = (long) 3;
    transient public static final Long gappedAlignmentDropoff_BLASTX_DEFAULT = (long) 15;
    transient public static final Long hitExtensionThreshold_BLASTX_DEFAULT = (long) 12;
    transient public static final Double ungappedExtensionDropoff_BLASTX_DEFAULT = 7.0;
    transient public static final Double finalGappedDropoff_BLASTX_DEFAULT = 25.0;
    transient public static final Long multiHitWindowSize_BLASTX_DEFAULT = (long) 40;

    // Custom defaults
    transient public static final Boolean gappedAlignment_DEFAULT = Boolean.TRUE;
    transient public static final String searchStrand_DEFAULT = "both";
    transient public static final Long blastxFrameshiftPenalty_DEFAULT = (long) 0;

    // Parameter Keys
/*
    transient public static final String PARAM_gappedAlignment = "gapped alignment";//-g)";
    transient public static final String PARAM_searchStrand = "search strand";//-S)";
    transient public static final String PARAM_frameshiftPenalty = "frameshift penalty";//-w)";
*/

    public BlastXTask() {
        super();
        setParameter(PARAM_wordsize, wordsize_BLASTX_DEFAULT.toString());
        setParameter(PARAM_gappedAlignmentDropoff, gappedAlignmentDropoff_BLASTX_DEFAULT.toString());
        setParameter(PARAM_hitExtensionThreshold, hitExtensionThreshold_BLASTX_DEFAULT.toString());
        setParameter(PARAM_ungappedExtensionDropoff, ungappedExtensionDropoff_BLASTX_DEFAULT.toString());
        setParameter(PARAM_finalGappedDropoff, finalGappedDropoff_BLASTX_DEFAULT.toString());
        setParameter(PARAM_multiHitWindowSize, multiHitWindowSize_BLASTX_DEFAULT.toString());
        setParameter(PARAM_gappedAlignment, gappedAlignment_DEFAULT.toString());
        setParameter(PARAM_searchStrand, searchStrand_DEFAULT);
        setParameter(PARAM_frameshiftPenalty, blastxFrameshiftPenalty_DEFAULT.toString());

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
        if (key.equals(PARAM_gappedAlignment))
            return new BooleanParameterVO(Boolean.valueOf(value));
        if (key.equals(PARAM_searchStrand))
            return new SingleSelectVO(getSearchStrandList(), value);
        if (key.equals(PARAM_frameshiftPenalty))
            return new LongParameterVO(new Long(value));
        // no match
        return null;
    }

    /**
     * full constructor
     */
    public BlastXTask(Set<Node> inputNodes,
                      String owner,
                      List<Event> events,
                      Set<TaskParameter> parameters) {
        super(inputNodes, owner, events, parameters);
        constructorCommon();
    }

    public String getDisplayName() {
        return DISPLAY_BLASTX;
    }

    private void constructorCommon() {
        this.taskName = BLASTX_NAME;
    }

    public String generateCommandStringNotIncludingIOParams() throws ParameterException {
        StringBuffer sb = new StringBuffer(generateDefaultCommandStringNotIncludingIOParams());
        sb.append("-w ").append((getParameterVO(PARAM_frameshiftPenalty)).getStringValue()).append(" ");
        sb.append("-g ").append((getParameterVO(PARAM_gappedAlignment)).getStringValue().equals("true") ? "T" : "F").append(" ");
        sb.append("-S ").append(searchStrandTranslator((getParameterVO(PARAM_searchStrand)).getStringValue())).append(" ");
        return sb.toString();
    }

}
