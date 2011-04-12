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

package org.janelia.it.jacs.compute.service.metageno.anno;

import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.SubmitJobAndWaitHelper;
import org.janelia.it.jacs.compute.service.metageno.MetaGenoPerlConfig;
import org.janelia.it.jacs.compute.service.metageno.SimpleGridJobRunner;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.blast.BlastPTask;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.tasks.blast.TeragridSimpleBlastTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 19, 2009
 * Time: 2:33:19 PM
 */
public class PandaBlastpService extends MgAnnoBaseService {

    public static final String ANNOTATION_INPUT_DATA_TYPE = "BTAB";

    protected Boolean useTeraGrid = SystemConfigurationProperties.getBoolean("MgAnnotation.TeraGrid.UseForBlast");
    protected String tgPandaDbName = SystemConfigurationProperties.getString("MgAnnotation.TeraGrid.PandaDbName");
    protected String tgBlastpProgram = SystemConfigurationProperties.getString("MgAnnotation.TeraGrid.BlastpProgram");
    protected Long tgPandaDbSize = SystemConfigurationProperties.getLong("MgAnnotation.TeraGrid.PandaDbSize");
    protected String tgAccessionMap = SystemConfigurationProperties.getString("MgAnnotation.TeraGrid.SqliteAccessionMap");
    protected String tgPandaBlastpParams = SystemConfigurationProperties.getString("MgAnnotation.TeraGrid.PandaBlastpParameters");

    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            logger.info(getClass().getName() + " execute() start");

            Long blastTaskId;
            BlastResultFileNode resultNode;
            //execute BLAST
            //create BLAST file node that stores all blast outputs
            if (useTeraGrid) {
                resultNode = (BlastResultFileNode) setupAndStartTgBlastp();
            }
            else {
                resultNode = (BlastResultFileNode) setupAndStartBlastp();
            }

            //get paths of btab and xml files
            String resultFilePath = resultNode.getFilePathByTag(BlastResultFileNode.TAG_BTAB);
            String xmlFilePath = resultNode.getFilePathByTag(BlastResultFileNode.TAG_XML);

            //copy btab and XML result files to annotation result node    (source ,destination)
            FileUtil.copyFileUsingSystemCall(resultFilePath, resultFile.getAbsolutePath() + ".btab");
            FileUtil.copyFileUsingSystemCall(xmlFilePath, resultFile.getAbsolutePath() + ".xml");

            //parse btab
            File pandaParseDir = new File(resultFile.getAbsolutePath() + "_pandaParseDir");
            pandaParseDir.mkdirs();
            File parsedFile = new File(resultFile.getAbsolutePath() + ".btab.parsed");
            String parserStr = MetaGenoPerlConfig.getCmdPrefix() + parserCmd +
                    " --input_file " + resultFile.getAbsolutePath() + ".btab" +
                    " --input_type " + ANNOTATION_INPUT_DATA_TYPE +
                    " --output_file " + parsedFile.getAbsolutePath() +
                    " --work_dir " + snapshotDir;

            SimpleGridJobRunner job = new SimpleGridJobRunner(workingDir, parserStr, queue, parentTask.getParameter("project"), parentTask.getObjectId());

            if (!job.execute()) {
                throw new Exception("Grid job failed with cmd=" + parserStr);
            }
            addParsedFile(parsedFile);

            // Step 4: Clean parse directories
            File[] parseFiles = pandaParseDir.listFiles();
            for (File f : parseFiles) {
                f.delete();
            }

            pandaParseDir.delete();

            logger.info(getClass().getName() + " execute() finish");
        }
        catch (Exception e) {
            if (parentTaskErrorFlag) {
                logger.info("Parent task has error -returning");
            }
            else {
                this.setParentTaskToErrorStatus(parentTask, this.getClass().getName() + " : " + e.getMessage());
                throw new ServiceException(e);
            }
        }
    }

    protected void init(IProcessData processData) throws MissingDataException, IOException {
        super.init(processData);
        setup(getClass().getSimpleName(), ".blastp_out");
    }

    protected Node setupAndStartBlastp() throws Exception {
        
        BlastPTask blastpTask = new BlastPTask();

        //set the owner
        blastpTask.setOwner(parentTask.getOwner());

        //specify the blast subject database
        blastpTask.setParameter(BlastPTask.PARAM_subjectDatabases, pandaBlastpDbId);

        //specify the input node
        if (isSubFileMode()) {
            String sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            Long inputFileId = MetaGenoAnnotationSetupService.createPeptideFastaFileNode(blastpTask.getOwner(), "FastaFileNode for " + blastpTask.getTaskName(),
                    "BlastpTask for " + blastpTask.getTaskName(), inputFile.getAbsolutePath(), logger, sessionName);
            blastpTask.setParameter(BlastTask.PARAM_query, inputFileId.toString());
        }
        else {
            blastpTask.setParameter(BlastPTask.PARAM_query, fileId);
        }

        blastpTask.setJobName("blastp " + fileId);
        blastpTask.setParameter(BlastTask.PARAM_project, parentTask.getParameter("project"));

        //define number of alignments/hits to keep 
        blastpTask.setParameter(BlastTask.PARAM_databaseAlignments, pandaBlastpAlignments);
        blastpTask.setParameter(BlastTask.PARAM_bestHitsToKeep, pandaBlastpHits);
        blastpTask.setParameter(BlastTask.PARAM_filter, "T");
        blastpTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(-5));

        blastpTask.setParentTaskId(parentTask.getObjectId());

        //test log 
        logger.info(getClass().getName() + " @@@@STARTING BLASTP SERVICE...");

        //submit blastp task
        ComputeBeanRemote computeBean = getComputeBean();
        blastpTask = (BlastPTask) computeBean.saveOrUpdateTask(blastpTask);
        SubmitJobAndWaitHelper jobHelper = new SubmitJobAndWaitHelper("BlastWithGridMerge", blastpTask.getObjectId());
        return jobHelper.startAndWaitTillDone();
    }

    protected Node setupAndStartTgBlastp() throws Exception {
        TeragridSimpleBlastTask blastpTask = new TeragridSimpleBlastTask();
        blastpTask.setOwner(parentTask.getOwner());

        blastpTask.setParameter(TeragridSimpleBlastTask.PARAM_tg_db_name, tgPandaDbName);
        blastpTask.setParameter(TeragridSimpleBlastTask.PARAM_mpi_blast_program, tgBlastpProgram);
        blastpTask.setParameter(TeragridSimpleBlastTask.PARAM_tg_db_size, tgPandaDbSize.toString());
        blastpTask.setParameter(TeragridSimpleBlastTask.PARAM_path_to_sqlite_map_db, tgAccessionMap);
        blastpTask.setParameter(TeragridSimpleBlastTask.PARAM_mpi_blast_parameters, tgPandaBlastpParams);

        if (isSubFileMode()) {
            String sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            Long inputFileId = MetaGenoAnnotationSetupService.createPeptideFastaFileNode(blastpTask.getOwner(), "FastaFileNode for " + blastpTask.getTaskName(),
                    "BlastpTask for " + blastpTask.getTaskName(), inputFile.getAbsolutePath(), logger, sessionName);
            blastpTask.setParameter(BlastTask.PARAM_query, inputFileId.toString());

        }
        else {
            blastpTask.setParameter(BlastPTask.PARAM_query, fileId);
        }
        blastpTask.setJobName("blastp " + fileId);
        blastpTask.setParameter(BlastTask.PARAM_project, parentTask.getParameter("project"));
        blastpTask.setParentTaskId(parentTask.getObjectId());
        ComputeBeanRemote computeBean = getComputeBean();
        blastpTask = (TeragridSimpleBlastTask) computeBean.saveOrUpdateTask(blastpTask);
        SubmitJobAndWaitHelper jobHelper = new SubmitJobAndWaitHelper("TeragridSimpleBlast", blastpTask.getObjectId());
        return jobHelper.startAndWaitTillDone();
    }

}