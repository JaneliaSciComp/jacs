
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
public class TBlastXTask extends BlastTask {
    transient public static final String TBLASTX_NAME = "tblastx";
    transient public static final String DISPLAY_TBLASTX = "TBLASTX (nuc/nuc)";

    // Default values
    transient public static final Long wordsize_TBLASTX_DEFAULT = (long) 3;
    transient public static final Long gappedAlignmentDropoff_TBLASTX_DEFAULT = (long) 0;
    transient public static final Long hitExtensionThreshold_TBLASTX_DEFAULT = (long) 13;
    transient public static final Double ungappedExtensionDropoff_TBLASTX_DEFAULT = 7.0;
    transient public static final Double finalGappedDropoff_TBLASTX_DEFAULT = 0.0;
    transient public static final Long multiHitWindowSize_TBLASTX_DEFAULT = (long) 40;

    // Custom defaults
    transient public static final String searchStrand_DEFAULT = "both";

    // Parameter Keys
    // transient public static final String PARAM_searchStrand = "search strand";//-S)";

    public TBlastXTask() {
        super();
        setParameter(PARAM_wordsize, wordsize_TBLASTX_DEFAULT.toString());
        setParameter(PARAM_gappedAlignmentDropoff, gappedAlignmentDropoff_TBLASTX_DEFAULT.toString());
        setParameter(PARAM_hitExtensionThreshold, hitExtensionThreshold_TBLASTX_DEFAULT.toString());
        setParameter(PARAM_ungappedExtensionDropoff, ungappedExtensionDropoff_TBLASTX_DEFAULT.toString());
        setParameter(PARAM_finalGappedDropoff, finalGappedDropoff_TBLASTX_DEFAULT.toString());
        setParameter(PARAM_multiHitWindowSize, multiHitWindowSize_TBLASTX_DEFAULT.toString());
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
        if (key.equals(PARAM_searchStrand))
            return new SingleSelectVO(getSearchStrandList(), searchStrand_DEFAULT);
        // no match
        return null;
    }

    /**
     * full constructor
     */
    public TBlastXTask(Set<Node> inputNodes,
                       String owner,
                       List<Event> events,
                       Set<TaskParameter> parameters) {
        super(inputNodes, owner, events, parameters);
        constructorCommon();
    }

    public String getDisplayName() {
        return DISPLAY_TBLASTX;
    }

    private void constructorCommon() {
        this.taskName = TBLASTX_NAME;
    }

    public String generateCommandStringNotIncludingIOParams() throws ParameterException {
        StringBuffer sb = new StringBuffer(generateDefaultCommandStringNotIncludingIOParams());
        sb.append("-S ").append(searchStrandTranslator((getParameterVO(PARAM_searchStrand)).getStringValue())).append(" ");
        return sb.toString();
    }

}
