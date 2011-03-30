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

package org.janelia.it.jacs.compute.service.blast.createDB;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.tasks.blast.CreateMpiBlastDatabaseTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 3, 2008
 * Time: 2:53:59 PM
 */
public class CreateMpiBlastDBService extends SubmitDrmaaJobService {
    private static final String CONFIG_PREFIX = "mpiFormatDbConfiguration.";

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "mpiFormatDb";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        CreateMpiBlastDatabaseTask mpiBlastDatabaseTask = (CreateMpiBlastDatabaseTask) task;
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean success = configFile.createNewFile();
        if (!success) {
            throw new ServiceException("Could not create the Mpi Blast config file.");
        }
        FastaFileNode fastaFile = (FastaFileNode) computeDAO.getNodeById(Long.valueOf(task.getParameter(CreateMpiBlastDatabaseTask.PARAM_FASTA_NODE_ID)));

        String fullCmd = "mpiformatdb --nfrags " + mpiBlastDatabaseTask.getNumberFragments() + " -i " + fastaFile.getFastaFilePath();

        StringBuffer script = new StringBuffer();
        script.append(fullCmd).append("\n");
        writer.write(script.toString());
        setJobIncrementStop(1);
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + NORMAL_QUEUE);
        jt.setNativeSpecification(NORMAL_QUEUE);
    }

    private void checkForFormatDbErrors(File blastDBOutputDir) throws IOException, InterruptedException {
        // grep 'ERROR' /db/cameradb/dma/system/**/formatdb.log | wc -l
        String formatdbLogPath = blastDBOutputDir.getAbsolutePath() + File.separator + "formatdb.log";
        int count = FileUtil.getCountUsingUnixCall("grep 'ERROR' " + formatdbLogPath + " | wc -l");
        logger.info(blastDBOutputDir.getName() + " formatdb error count=" + count);
        if (count > 0) {
            throw new RuntimeException("formatdb failed");
        }
    }

}