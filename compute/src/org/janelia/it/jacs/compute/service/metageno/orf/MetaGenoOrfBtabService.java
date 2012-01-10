
package org.janelia.it.jacs.compute.service.metageno.orf;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.metageno.MetaGenoPerlConfig;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoOrfCallerTask;
import org.janelia.it.jacs.model.user_data.metageno.MetaGenoOrfCallerResultNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 19, 2009
 * Time: 4:33:31 PM
 */
public class MetaGenoOrfBtabService implements IService {

    private static String orfBtabCmd = SystemConfigurationProperties.getString("MgPipeline.OrfToBtabCmd");
    private static String orfOverlapCmd = SystemConfigurationProperties.getString("MgPipeline.OrfOverlapCmd");
    private static String orfStatsCmd = SystemConfigurationProperties.getString("MgPipeline.OrfStatsCmd");
    private static String orfStatsPercInterval = SystemConfigurationProperties.getString("MgPipeline.OrfStatsPercentNInterval");
    private static String orfStatsLengthInterval = SystemConfigurationProperties.getString("MgPipeline.OrfStatsLengthInterval");
    private static String scratchDirPath = SystemConfigurationProperties.getString("SystemCall.ScratchDir");

    /*
        SCRIPT DEPENDENCIES

        OrfToBtabCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/cameragene2btab.pl
            <none>
        OrfOverlapCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/report_camera_orf_overlaps.pl
            <none>
        OrfStatsCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/camera_orf_stats.pl
            <none>

        MODULE SUMMARY
            <none>
     */

    MetaGenoOrfCallerResultNode resultNode;
    File resultNodeDir;
    SystemCall sc;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            resultNode = (MetaGenoOrfCallerResultNode) processData.getItem("META_GENO_ORF_CALLER_RESULT_NODE");
            logger.info(this.getClass().getName() + " execute() start");
            logger.info("Using result node directory=" + resultNode.getDirectoryPath());
            resultNodeDir = new File(resultNode.getDirectoryPath());

            MetaGenoOrfCallerTask parentTask = (MetaGenoOrfCallerTask) ProcessDataHelper.getTask(processData);
            if (parentTask == null) {
                throw new Exception("Could not get parent task for " + this.getClass().getName());
            }

            File scratchDir = new File(scratchDirPath);
            logger.info("Using scratchDir=" + scratchDir.getAbsolutePath());
            sc = new SystemCall(null, scratchDir, logger);

            File pepBtabFile = processPepBtabFile(processData);
            processOrfOverlapFile(processData);

            if (parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_useClearRange).equals(Boolean.TRUE.toString())) {
                processOrfStats(processData);
            }

            processData.putItem("ORF_BTAB_FILE", pepBtabFile);

            sc.cleanup();

            logger.info(this.getClass().getName() + " execute() finish");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private File processPepBtabFile(IProcessData processData) throws Exception {
        File orfInputFile = (File) processData.getItem("ORF_PEP_CLR_FAA_FILE");
        File pepBtabFile = new File(orfInputFile.getAbsolutePath() + ".btab");

        // Create list file needed for btab script
        File listFile = new File(orfInputFile.getAbsolutePath() + ".btab.list");
        FileWriter listWriter = new FileWriter(listFile);
        listWriter.write(orfInputFile.getAbsolutePath() + "\n");
        listWriter.close();

        // Btab script
        String btabCmd = MetaGenoPerlConfig.getCmdPrefix() + orfBtabCmd +
                " --input_file " + listFile.getAbsolutePath() +
                " --output_file " + pepBtabFile.getAbsolutePath();

        int ev = sc.execute(btabCmd, false);
        if (ev != 0) {
            throw new Exception("SystemCall produced non-zero exit value=" + btabCmd);
        }

        return pepBtabFile;
    }

    private void processOrfOverlapFile(IProcessData processData) throws Exception {
        File orfInputFile = (File) processData.getItem("ORF_CLR_FNA_FILE");

        // Create list file needed for overlap script
        File listFile = new File(orfInputFile.getAbsolutePath() + ".overlap.list");
        FileWriter listWriter = new FileWriter(listFile);
        listWriter.write(orfInputFile.getAbsolutePath() + "\n");
        listWriter.close();

        // Overlap script
        String overlapCmd = MetaGenoPerlConfig.getCmdPrefix() + orfOverlapCmd +
                " --input_list " + listFile.getAbsolutePath() +
                " --output_file " + orfInputFile.getAbsolutePath() + ".overlap" +
                " --stderr " + orfInputFile.getAbsolutePath() + ".overlap.stderr" +
                " --stdout " + orfInputFile.getAbsolutePath() + ".overlap.stdout";
        int ev = sc.execute(overlapCmd, false);
        if (ev != 0) {
            throw new Exception("SystemCall produced non-zero exit value=" + overlapCmd);
        }

    }

    private void processOrfStats(IProcessData processData) throws Exception {
        /*
         *  camera_orf_stats
         *
         *   --input_list <file with input file list>
         *   --percent_n_interval <?>
         *   --size_interval <?>
         *   --library_name <blank>
         *   stderr <stderr file>
         *   stdout <stdout file>
         *
         */
        File orfInputFile = (File) processData.getItem("ORF_CLR_FNA_FILE");

        // Create list file needed for overlap script
        File listFile = new File(orfInputFile.getAbsolutePath() + ".stats.list");
        FileWriter listWriter = new FileWriter(listFile);
        listWriter.write(orfInputFile.getAbsolutePath() + "\n");
        listWriter.close();

        // Overlap script
        String overlapCmd = MetaGenoPerlConfig.getCmdPrefix() + orfStatsCmd +
                " --input_list " + listFile.getAbsolutePath() +
                " --percent_n_interval " + orfStatsPercInterval +
                " --size_interval " + orfStatsLengthInterval +
                " --library_name \"\"" +
                " > " + orfInputFile.getAbsolutePath() + ".stats" +
                "\n";
        int ev = sc.execute(overlapCmd, false);
        if (ev != 0) {
            throw new Exception("SystemCall produced non-zero exit value=" + overlapCmd);
        }
    }

}
