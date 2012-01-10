
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
public class BlastPTask extends BlastTask {
    transient public static final String BLASTP_NAME = "blastp";
    transient public static final String DISPLAY_BLASTP = "BLASTP (prot/prot)";

    // Default values - default overrides
    transient public static final Long wordsize_BLASTP_DEFAULT = (long) 3;
    transient public static final Long gappedAlignmentDropoff_BLASTP_DEFAULT = (long) 15;
    transient public static final Long hitExtensionThreshold_BLASTP_DEFAULT = (long) 11;
    transient public static final Double ungappedExtensionDropoff_BLASTP_DEFAULT = 7.0;
    transient public static final Double finalGappedDropoff_BLASTP_DEFAULT = 25.0;
    transient public static final Long multiHitWindowSize_BLASTP_DEFAULT = (long) 40;

    // Custom defaults
    transient public static final Boolean gappedAlignment_DEFAULT = Boolean.TRUE;

    // Parameter Keys
    //transient public static final String PARAM_gappedAlignment = "gapped alignment";//-g)";

//    static {
//        Task.addKey(PARAM_gappedAlignment);
//    }

    public BlastPTask() {
        super();
        setParameter(PARAM_wordsize, wordsize_BLASTP_DEFAULT.toString());
        setParameter(PARAM_gappedAlignmentDropoff, gappedAlignmentDropoff_BLASTP_DEFAULT.toString());
        setParameter(PARAM_hitExtensionThreshold, hitExtensionThreshold_BLASTP_DEFAULT.toString());
        setParameter(PARAM_ungappedExtensionDropoff, ungappedExtensionDropoff_BLASTP_DEFAULT.toString());
        setParameter(PARAM_finalGappedDropoff, finalGappedDropoff_BLASTP_DEFAULT.toString());
        setParameter(PARAM_multiHitWindowSize, multiHitWindowSize_BLASTP_DEFAULT.toString());
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
        if (key.equals(PARAM_gappedAlignment))
            return new BooleanParameterVO(Boolean.valueOf(value));
        // no match
        return null;
    }

    /**
     * full constructor
     */
    public BlastPTask(Set<Node> inputNodes,
                      String owner,
                      List<Event> events,
                      Set<TaskParameter> parameters) {
        super(inputNodes, owner, events, parameters);
        constructorCommon();
    }

    public String getDisplayName() {
        return DISPLAY_BLASTP;
    }

    private void constructorCommon() {
        this.taskName = BLASTP_NAME;
    }

    public String generateCommandStringNotIncludingIOParams() throws ParameterException {
        StringBuffer sb = new StringBuffer(generateDefaultCommandStringNotIncludingIOParams());
        sb.append("-g ").append((getParameterVO(PARAM_gappedAlignment)).getStringValue().equals("true") ? "T" : "F").append(" ");
        return sb.toString();
    }

}
