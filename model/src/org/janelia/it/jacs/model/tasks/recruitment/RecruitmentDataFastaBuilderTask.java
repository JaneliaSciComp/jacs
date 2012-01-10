
package org.janelia.it.jacs.model.tasks.recruitment;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 17, 2008
 * Time: 10:35:36 AM
 */
public class RecruitmentDataFastaBuilderTask extends Task {
    public static final String RECRUITMENT_RESULT_NODE_ID = "recruitmentResultNodeId";

    public RecruitmentDataFastaBuilderTask() {
    }

    public RecruitmentDataFastaBuilderTask(String recruitmentResultNodeId) {
        this.taskName = "Recruitment Data Fasta Builder Task";
        setParameter(RECRUITMENT_RESULT_NODE_ID, recruitmentResultNodeId);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(RECRUITMENT_RESULT_NODE_ID)) {
            return new TextParameterVO(value, value.length());
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return "Recruitment Data Fasta Builder";
    }
}
