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

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.recruitment.FRVSamplingFastaGenerationTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 10, 2009
 * Time: 11:38:40 AM
 */
public class SegmentAndOverlapFastaService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        // Get the values from the task
        org.janelia.it.jacs.compute.api.ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        Task task = null;
        try {
            // Prep for execution
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            String scriptBaseDir = SystemConfigurationProperties.getString("Perl.ModuleBase") + SystemConfigurationProperties.getString("RecruitmentViewer.PerlBaseDir");

            // Split up the Fasta file into a striped version

            FastaFileNode tmpNode = (FastaFileNode) computeBean.getNodeById(Long.valueOf(task.getParameter(FRVSamplingFastaGenerationTask.PARAM_query)));
            SystemCall call = new SystemCall(logger);
            int exitCode;
            String filenameBase = tmpNode.getFastaFilePath();
            File originalFile = new File(filenameBase + ".original");
            // Copy the file to a backup
            // We only want to back-up the original once, so check for it
            if (!originalFile.exists()) {
                exitCode = call.emulateCommandLine("mv " + filenameBase + " " + originalFile.getAbsolutePath(), true);
                if (0 != exitCode) {
                    throw new ServiceException("There was a problem moving the fasta file to be striped.");
                }
            }
            // Numbers below are listed in base pairs
            long sizeOfChunkProduced = SystemConfigurationProperties.getLong("Recruitment.StripeChunkSize");
            long amountOfOverlap = SystemConfigurationProperties.getLong("Recruitment.OverlapAmount");
            String command = SystemConfigurationProperties.getString("Perl.Path") + " " + scriptBaseDir + "/chopUpFastaIntoOverlapsSegments.pl " +
                    filenameBase + ".original" + " " + sizeOfChunkProduced + " " + amountOfOverlap + " > " + filenameBase;
            command = "export PATH=$PATH:" + scriptBaseDir + ";export PERL5LIB=" + scriptBaseDir + ";" + command;
            exitCode = call.emulateCommandLine(command, true);
            if (0 != exitCode) {
                throw new ServiceException("There was a problem striping the fasta file.");
            }
        }
        catch (Exception e) {
            if (null != task) {
                System.out.println("\n\n\nError striping the FRV molecule data for user " + task.getOwner() + ", task=" + task.getObjectId() + "\nERROR:" + e.getMessage());
                // Try to record the error
                try {
                    computeBean.saveEvent(task.getObjectId(), Event.ERROR_EVENT, "Error executing the FRV pipeline", new Date());
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            throw new ServiceException(e);
        }
    }

}
