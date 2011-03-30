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

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 14, 2007
 * Time: 10:11:38 AM
 */
public class FrvNonGridDataAndAnnotationService extends FrvNonGridServiceBase {

    protected void recruit() throws IOException, ParameterException, InterruptedException {
//        Placed in process def
        logger.debug("\nFrvNonGridDataAndAnnotationService recruit started");
        String basePath = SystemConfigurationProperties.getString("Perl.ModuleBase") +
                SystemConfigurationProperties.getString("RecruitmentViewer.PerlBaseDir");
        String sampleInfoName = SystemConfigurationProperties.getString("RecruitmentViewer.SampleFile.Name");
        String pathToAnnotationFile = null;
        // Since annotations don't exist for user-uploaded sequences, there is no
        if (User.SYSTEM_USER_LOGIN.equalsIgnoreCase(dataFileNode.getOwner())) {
            GenomeProjectFileNode gpNode = (GenomeProjectFileNode) computeDAO.getNodeById(Long.valueOf(task.getParameter(RecruitmentViewerTask.GENOME_PROJECT_NODE_ID)));
            pathToAnnotationFile = gpNode.getDirectoryPath() + File.separator +
                    task.getParameter(RecruitmentViewerFilterDataTask.GENBANK_FILE_NAME);
        }
        RecruitmentDataHelper helper = new RecruitmentDataHelper(dataFileNode.getDirectoryPath(),
                resultFileNode.getDirectoryPath(), pathToAnnotationFile,
                basePath + File.separator + sampleInfoName, task.getSampleListAsCommaSeparatedString(), Integer.toString(task.getPercentIdMin()),
                Integer.toString(task.getPercentIdMax()), Double.toString(task.getReferenceBegin()), Double.toString(task.getReferenceEnd()),
                task.getMateBits(), task.getAnnotationFilterString(), task.getMateSpanPoint(), task.getColorizationType());
        helper.regenerateDataAndAnnotations();
        saveNumRecruitedReads();
        logger.debug("\nFrvNonGridDataAndAnnotationService recruit() complete");
    }

}