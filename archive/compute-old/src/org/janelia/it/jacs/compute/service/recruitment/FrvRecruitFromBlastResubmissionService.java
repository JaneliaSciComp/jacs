
package org.janelia.it.jacs.compute.service.recruitment;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 21, 2007
 * Time: 10:49:15 AM
 */
public class FrvRecruitFromBlastResubmissionService extends FRVRecruitFromBlastService {

    protected void init(IProcessData processData) throws MissingDataException, IOException {
        Task tmpTask = ProcessDataHelper.getTask(processData);
        RecruitmentFileNode recruitmentNode = (RecruitmentFileNode) new ComputeDAO(_logger).getResultNodeByTaskId(tmpTask.getObjectId());
        // There should at least be one RecruitmentFileNode since we are supped to update the images
        processData.putItem(ProcessDataConstants.RECRUITMENT_FILE_NODE_ID, recruitmentNode.getObjectId());
        super.init(processData);
    }

}