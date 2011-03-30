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

package org.janelia.it.jacs.compute.service.profileComparison;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.profileComparison.ProfileComparisonTask;
import org.janelia.it.jacs.model.user_data.profileComparison.ProfileComparisonResultNode;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * User: tsafford, naxelrod
 * Date: Sep 03, 2009
 */
public class ProfileComparisonSubmitJobService extends SubmitDrmaaJobService {
    private static final String CONFIG_PREFIX = "profileComparisonConfiguration.";

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "profileComparison";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        //ProfileComparisonTask profileComparisonTask = (ProfileComparisonTask) task;
        ProfileComparisonResultNode tmpResultNode = (ProfileComparisonResultNode) resultFileNode;

        // Creating the default config file for the Drmaa Template
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess) {
            logger.error("Unable to create configFile for ProfileComparison process.");
        }

        String perlPath = SystemConfigurationProperties.getString("Perl.Path");
        String basePath = SystemConfigurationProperties.getString("Perl.ModuleBase");
        String pipelineCmd = perlPath + " " + basePath + SystemConfigurationProperties.getString("ProfileComparison.PerlBase") +
                SystemConfigurationProperties.getString("ProfileComparison.Cmd");
        String tmpDirectoryName = SystemConfigurationProperties.getString("Upload.ScratchDir");
        List<String> inputFiles = Task.listOfStringsFromCsvString(task.getParameter(ProfileComparisonTask.PARAM_inputFile));

        // Takes a list of files, smart enough to figure out the file type based on extension
        String fullCmd = pipelineCmd + " -o " + tmpResultNode.getDirectoryPath();
        for (String inputFile : inputFiles) {
            fullCmd += " -f " + tmpDirectoryName + File.separator + inputFile;
        }
        fullCmd = "export PATH=$PATH:" + basePath + ";export PERL5LIB=$PERL5LIB:" + basePath +
                SystemConfigurationProperties.getString("ProfileComparison.PerlBase") + ";" + fullCmd;
        StringBuffer script = new StringBuffer();
        script.append(fullCmd).append("\n");
        writer.write(script.toString());
        setJobIncrementStop(1);
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + NORMAL_QUEUE);
        jt.setNativeSpecification(NORMAL_QUEUE);
    }

}