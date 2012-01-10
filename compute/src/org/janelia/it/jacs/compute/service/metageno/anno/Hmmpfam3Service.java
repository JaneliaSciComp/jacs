
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
import org.janelia.it.jacs.model.tasks.hmmer3.HMMER3Task;
import org.janelia.it.jacs.model.user_data.hmmer3.HMMER3ResultFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 19, 2009
 * Time: 2:32:52 PM
 */
public class Hmmpfam3Service extends MgAnnoBaseService {

    public static final String ANNOTATION_INPUT_DATA_TYPE = "HTAB";

    protected static int hmmpfamTimeout = SystemConfigurationProperties.getInt("MgAnnotation.LdhmmpfamRetryTimeoutMinutes");

    SystemCall sc;

    /* SCRIPT DEPENDENCIES

        hmmpfam2HtabCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/camera_htab.pl
            <none>
        hmmpfam2BsmlCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/hmmpfam2bsml
            use strict;
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev);
            use Pod::Usage;
            use Ergatis::Logger;
            use BSML::BsmlRepository;
            use BSML::BsmlBuilder;
            use BSML::BsmlParserTwig;
        parserCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/camera_parse_annotation_results_to_text_table.pl
            use lib "/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/current/annotation_tool";
            use CAMERA::Parser::BTAB;
            use CAMERA::Parser::HTAB;
            use CAMERA::Parser::ECTable;
            use CAMERA::Parser::TMHMMBSML;
            use CAMERA::Parser::LipoproteinMotifBSML;
            use CAMERA::Parser::Hypothetical;
            use CAMERA::PolypeptideSet;
            #use DBM::Deep;
            use Getopt::Long;
            #use File::Copy;

        MODULE SUMMARY
            Ergatis, BSML, CAMERA

     */

    /*
     * Command format:
     *
     *  ldhmmpfam
     *
     *      <exec> <other_opts> <database path> <input file path> > <output.raw>
     *
     *  htab_exec (hmmpfam2htab?)
     *
     *      <output.raw> | <exec>
     *
     *  hmmpfam2bsml
     *
     *      <exec> --input <output.raw> --output <output.bsml> --fasta_input <input file path>
     */

    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            logger.info(getClass().getName() + " execute() start");

            // Step 1: ldhmmpfam
            HMMER3Task hmmpfamTask =  new  HMMER3Task();

            //HmmpfamTask hmmpfamTask = new HmmpfamTask();
            hmmpfamTask.setOwner(parentTask.getOwner());
            hmmpfamTask.setParameter(HMMER3Task.PARAM_db_node_id, hmmpfam3DbId);

            if (isSubFileMode()) {
                String sessionName = ProcessDataHelper.getSessionRelativePath(processData);
                Long inputFileId = MetaGenoAnnotationSetupService.createPeptideFastaFileNode(hmmpfamTask.getOwner(), "FastaFileNode for " + hmmpfamTask.getTaskName(),
                        "HmmpfamTask for " + hmmpfamTask.getTaskName(), inputFile.getAbsolutePath(), logger, sessionName);
                hmmpfamTask.setParameter(HMMER3Task.PARAM_query_node_id, inputFileId.toString());

            }
            else {
                hmmpfamTask.setParameter(HMMER3Task.PARAM_query_node_id, fileId);
            }
            hmmpfamTask.setParameter(HMMER3Task.PARAM_project, parentTask.getParameter("project"));
            hmmpfamTask.setParentTaskId(parentTask.getObjectId());
            ComputeBeanRemote computeBean = getComputeBean();
            hmmpfamTask = (HMMER3Task) computeBean.saveOrUpdateTask(hmmpfamTask);
            SubmitJobAndWaitHelper jobHelper = new SubmitJobAndWaitHelper("HMMER3", hmmpfamTask.getObjectId());
            HMMER3ResultFileNode resultNode = (HMMER3ResultFileNode) jobHelper.startAndWaitTillDone();

            //get results
            String resultFilePath = resultNode.getFilePathByTag(HMMER3ResultFileNode.TAG_OUTPUT_FILE);
            logger.info("Fetching HMM results from "+resultFilePath);
            FileUtil.copyFileUsingSystemCall(resultFilePath, resultFile.getAbsolutePath());


//            String ldhmmpfamCmdStr=ldhmmpfamCmd +
//                   // " --threads "+ldhmmpfamThreads +  not necessary for clc hmmer
//                    " " + ldhmmpfamFullDb +
//                    " " + inputFile.getAbsolutePath() +
//                    " > " + resultFile.getAbsolutePath();
//            SimpleGridJobRunner job=new SimpleGridJobRunner(workingDir, ldhmmpfamCmdStr, queue, parentTask.getParameter("project"), hmmpfamTimeout, 3);
//            if (!job.execute()) {
//                throw new Exception("Grid job failed with cmd="+ldhmmpfamCmdStr);
//            }

            // Step 2: hmmpfam2htab
            String hmmpfam2htabStr = "unset PERL5LIB\n" +
                    "export PERL5LIB=" + MetaGenoPerlConfig.PERL5LIB + "\n" +
                    "export PERL_MOD_DIR=" + MetaGenoPerlConfig.PERL_MOD_DIR + "\n" +
                    "export SYBASE=" + MetaGenoPerlConfig.SYBASE + "\n" +
                    "cat " + resultFile.getAbsolutePath() + " | " + MetaGenoPerlConfig.PERL_EXEC + " " + MetaGenoPerlConfig.PERL_BIN_DIR +
                    "/" + hmmpfam2HtabCmd + " -d " +snapshotDir+"/hmm3.db > " + resultFile.getAbsolutePath() + ".htab";


            SimpleGridJobRunner job = new SimpleGridJobRunner(workingDir, hmmpfam2htabStr, queue, parentTask.getParameter("project"), parentTask.getObjectId());
            if (!job.execute()) {
                throw new Exception("Grid job failed with cmd=" + hmmpfam2htabStr);
            }
//            String htabValidationStr = MetaGenoPerlConfig.getCmdPrefix() + htabValidationCmd +
//                    " -raw " + resultFile.getAbsolutePath() +
//                    " -htab " + resultFile.getAbsolutePath() + ".htab" +
//                    " -threshold " + htabValidationThreshold;
//            int ev = sc.execute(htabValidationStr, false);
//            if (ev != 0) {
//                throw new Exception("SystemCall produced non-zero exit value=" + htabValidationStr);
//            }
//            else {
//                logger.info("Htab validation step completed with no errors for htab file=" + resultFile.getAbsolutePath() + ".htab");
//            }

            // Step 3: hmmpfam2bsml
//            String hmmpfam2bsmlStr = MetaGenoPerlConfig.getCmdPrefix() + hmmpfam2BsmlCmd +
//                    " --input " + resultFile.getAbsolutePath() +
//                    " --output " + resultFile.getAbsolutePath() + ".bsml" +
//                    " --fasta_input " + inputFile.getAbsolutePath();
//            job = new SimpleGridJobRunner(workingDir, hmmpfam2bsmlStr, queue, parentTask.getParameter("project"), parentTask.getObjectId());
//            if (!job.execute()) {
//                throw new Exception("Grid job failed with cmd=" + hmmpfam2bsmlStr);
//            }

            // Step 4: parse
            File parseDir = new File(resultFile.getAbsolutePath() + "_hmmpfamFullParseDir");
            parseDir.mkdirs();
            File parsedFile = new File(resultFile.getAbsolutePath() + ".htab.parsed");
            String parserStr = MetaGenoPerlConfig.getCmdPrefix() + parserCmd +
                    " --input_file " + resultFile.getAbsolutePath() + ".htab" +
                    " --input_type " + ANNOTATION_INPUT_DATA_TYPE + " " +
                    " --output_file " + parsedFile.getAbsolutePath() +
                    " --work_dir " + snapshotDir;

            job = new SimpleGridJobRunner(workingDir, parserStr, queue, parentTask.getParameter("project"), parentTask.getObjectId());
            if (!job.execute()) {
                throw new Exception("Grid job failed with cmd=" + parserStr);
            }
            addParsedFile(parsedFile);

            logger.info(getClass().getName()+"\n"+
                        "hmmpfam2htabStr:"        +hmmpfam2htabStr+"\n"+
                        "parserStr"   +resultFilePath
            );

            // Step 5: Clean parse directories
            File[] parseFiles = parseDir.listFiles();
            for (File f : parseFiles) {
                f.delete();
            }

            parseDir.delete();

            sc.cleanup();

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
        setup(getClass().getSimpleName(), ".ldhmmpfam.raw");
        File scratchDir = new File(scratchDirPath);
        sc = new SystemCall(null, scratchDir, logger);
    }

}
