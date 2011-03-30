/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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