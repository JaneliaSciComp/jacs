
package org.janelia.it.jacs.model.tasks.recruitment;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Feb 15, 2010
 * Time: 12:02:11 AM
 */
public class GenomeProjectRecruitmentSamplingTask extends Task {
    public static final String DISPLAY_NAME = "Recruitment Sampling Task";
    public static final String BLASTABLE_DATABASE_NODES = "blastableDatabaseNodes";

    public GenomeProjectRecruitmentSamplingTask() {
        super();
        this.taskName = DISPLAY_NAME;
    }

    public GenomeProjectRecruitmentSamplingTask(String commaSeparatedListOfBlastDBNodeIds, String owner) {
        super(null, owner, null, null);
        setParameter(BLASTABLE_DATABASE_NODES, commaSeparatedListOfBlastDBNodeIds);
        this.taskName = DISPLAY_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(BLASTABLE_DATABASE_NODES)) {
            List<String> selectList = Task.listOfStringsFromCsvString(value);
            return new MultiSelectVO(selectList, selectList);
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

}
