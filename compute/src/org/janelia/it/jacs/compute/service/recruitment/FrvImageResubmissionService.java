
package org.janelia.it.jacs.compute.service.recruitment;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 21, 2007
 * Time: 10:49:15 AM
 */
public class FrvImageResubmissionService extends FrvImageService {

    public void init(IProcessData processData) throws Exception {
        this.task = ProcessDataHelper.getTask(processData);
        RecruitmentResultFileNode resultNode = (RecruitmentResultFileNode) new ComputeDAO(logger).getResultNodeByTaskId(this.task.getObjectId());
        // There should at least be one RecruitmentResultFileNode since we are supposed to update the images
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultNode.getObjectId());
        super.init(processData);
    }

}
