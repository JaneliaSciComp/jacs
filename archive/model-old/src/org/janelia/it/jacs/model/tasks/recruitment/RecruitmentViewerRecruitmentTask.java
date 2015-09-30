
package org.janelia.it.jacs.model.tasks.recruitment;

import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 17, 2007
 * Time: 1:05:55 PM
 */
public class RecruitmentViewerRecruitmentTask extends RecruitmentViewerTask {

    public static final String DISPLAY_NAME = "Recruitment Viewer Recruit Data";

    public RecruitmentViewerRecruitmentTask() {
        super();
    }

    public RecruitmentViewerRecruitmentTask(String genomeProjectNodeId, String genBankFileName, Set inputNodes, String owner, List events,
                                            Set parameters, String subject, String query, Long genomeSize, String giNumber) {
        super(inputNodes, owner, events, parameters, subject, query);
        setParameter(GENOME_PROJECT_NODE_ID, genomeProjectNodeId);
        setParameter(GENBANK_FILE_NAME, genBankFileName);
        setParameter(GENOME_SIZE, genomeSize.toString());
        setParameter(GI_NUMBER, giNumber);
        this.taskName = "Recruitment Viewer Recruitment Task";
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
        if (key.equals(GENOME_PROJECT_NODE_ID)) {
            return new TextParameterVO(value, value.length() > 500 ? value.length() : 500);
        }
        if (key.equals(GENBANK_FILE_NAME)) {
            return new TextParameterVO(value, value.length() > 500 ? value.length() : 500);
        }
        if (key.equals(GI_NUMBER)) {
            return new TextParameterVO(value, value.length() > 500 ? value.length() : 500);
        }
        if (key.equals(GENOME_SIZE)) {
            Long tmpValue = new Long(value);
            return new LongParameterVO(tmpValue, tmpValue, tmpValue);
        }
        // no match
        return null;
    }

}
