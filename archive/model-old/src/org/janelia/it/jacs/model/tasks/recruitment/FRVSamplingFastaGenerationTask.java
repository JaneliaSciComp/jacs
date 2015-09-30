
package org.janelia.it.jacs.model.tasks.recruitment;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Feb 15, 2010
 * Time: 12:02:11 AM
 */
public class FRVSamplingFastaGenerationTask extends Task {
    public static final String DISPLAY_NAME = "FRV Sampling Fasta Generation Task";
    public static final String PARAM_query = "query";

    public FRVSamplingFastaGenerationTask() {
        super();
    }

    public FRVSamplingFastaGenerationTask(String fastaNodeId, String owner) {
        super(null, owner, null, null);
        setParameter(PARAM_query, fastaNodeId);
        this.taskName = DISPLAY_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_query)) {
            return new TextParameterVO(key);
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

}