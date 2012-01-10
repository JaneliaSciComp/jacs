
package org.janelia.it.jacs.model.tasks.recruitment;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 2, 2007
 * Time: 4:53:11 PM
 */
public class RecruitmentViewerTask extends Task {

    public static final String DISPLAY_NAME = "Recruitment Viewer Data";
    public static final String SUBJECT = "subject";
    public static final String QUERY = "query";
    public static final String COMBINED_HITS_PATH = "pathToCombinedHits";
    public static final String GENOME_SIZE = "genomeSize";
    public static final String GI_NUMBER = "giNumber";
    public static final String GENBANK_FILE_NAME = "genbankFileName";
    public static final String GENOME_PROJECT_NODE_ID = "genomeProjectNodeId";
    // Comma-separated list of blast database id's
    public static final String BLAST_DATABASE_IDS = "blastDatabaseIds";

    public RecruitmentViewerTask() {
        super();
    }

    public RecruitmentViewerTask(Set inputNodes, String owner, List events, Set parameters, String subject, String query) {
        super(inputNodes, owner, events, parameters);
        this.taskName = "Recruitment Viewer Task";
        setParameter(SUBJECT, subject);
        setParameter(QUERY, query);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(SUBJECT))
            return new TextParameterVO(value, 255);
        if (key.equals(QUERY))
            return new TextParameterVO(value, 255);
        // no match
        return null;
    }

    public String getSubject() throws ParameterException {
        return ((TextParameterVO) getParameterVO(SUBJECT)).getTextValue();
    }

    public String getQuery() throws ParameterException {
        return ((TextParameterVO) getParameterVO(QUERY)).getTextValue();
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

}
