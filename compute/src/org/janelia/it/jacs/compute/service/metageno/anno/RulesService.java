
package org.janelia.it.jacs.compute.service.metageno.anno;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.metageno.MetaGenoPerlConfig;
import org.janelia.it.jacs.compute.service.metageno.SimpleGridJobRunner;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 19, 2009
 * Time: 2:34:23 PM
 */
public class RulesService extends MgAnnoBaseService {

    /*
     * Inputs to the rules service:
     *
     *   hmm_full .raw
     *   // note: we are skipping the hmm frag dir for the time being
     *   blastp_panda .raw
     *   priam_ec .raw
     *   lipo .raw
     *   tmhmm .raw
     *   hypothetical .raw
     */

    /* SCRIPT DEPENDENCIES

        rulesAnnotateCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/camera_annotate_from_sorted_table.pl
            #use lib "/share/apps/ergatis/jboss-test/lib";
            use lib "/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/current/annotation_tool";
            use strict;
            use warnings;
            use Carp;
            use CAMERA::Polypeptide;
            use CAMERA::AnnotationData::Polypeptide;
            use CAMERA::AnnotationRules::PredictedProtein;
            use Data::Dumper;
            use Getopt::Long;

        MODULE SUMMARY
            CAMERA

     */

    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            logger.info(getClass().getName() + " execute() start");


            logger.info("Concatinating files...");
            // Step 1: concat all parsed files
            List<File> parsedFileList = getParsedFileList();
            logger.info("Contents of parsedFileList for RulesService:");
            for (File f : parsedFileList) {
                logger.info(f.getAbsolutePath());
            }
            FileUtil.concatFilesUsingSystemCall(parsedFileList, new File(resultFile.getAbsolutePath() + ".unsorted"));

             logger.info("Sorting files...");
            // Step 2: sort
            File sortDir = new File(resultFile.getAbsolutePath() + "_rulesSortDir");
            sortDir.mkdirs();
            String sortCmd = "/bin/sort --key=1,1 -T " + sortDir.getAbsolutePath() +
                    " -S 1G -d -o " + resultFile.getAbsolutePath() + ".sorted " +
                    resultFile.getAbsolutePath() + ".unsorted";

            SimpleGridJobRunner job = new SimpleGridJobRunner(workingDir, sortCmd, queue, parentTask.getParameter("project"), parentTask.getObjectId());
            if (!job.execute()) {
                throw new Exception("Grid job failed with cmd=" + sortCmd);
            }
            sortDir.delete();

            logger.info("Create annotation...");
            // Step 3: create annotation from sorted file
            String rulesAnnoStr = MetaGenoPerlConfig.getCmdPrefix() + rulesAnnotateCmd +
                    " --input " + resultFile.getAbsolutePath() + ".sorted" +
                    " --output " + resultFile.getAbsolutePath() + ".apparently_unused" +
                    " --synonyms " + snapshotDir+"/synonyms.tab" +
                    " > " + resultFile.getAbsolutePath() + ".rules";

            logger.info("Ececuting... "+"\n"+rulesAnnoStr);
            job = new SimpleGridJobRunner(workingDir, rulesAnnoStr, queue, parentTask.getParameter("project"), parentTask.getObjectId());

            if (!job.execute()) {
                throw new Exception("Grid job failed with cmd=" + rulesAnnoStr);
            }

             logger.info("Cleaning Directory...");
            // Step 4: clean
            File unusedFile = new File(resultFile.getAbsolutePath() + ".apparently_unused");
            if (unusedFile.length() == 0L) {
                unusedFile.delete();
            }

            //document each step
            logger.debug(getClass().getName()+"\n"+
                        "Annotation Rulessubmissions  Summary"                 +"\n"+
                        "Step 1 Sort:"                          +sortCmd+"\n"+
                        "Step 2 Create Annotation:"             +rulesAnnoStr+"\n"
            );            

            logger.info(getClass().getName() + " execute() finish");
        }
        catch (Exception e) {
            if (parentTaskErrorFlag) {
                logger.info("Parent task has error - returning");
            }
            else {
                this.setParentTaskToErrorStatus(parentTask, this.getClass().getName() + " : " + e.getMessage());
                throw new ServiceException(e);
            }
        }
    }

    protected void init(IProcessData processData) throws MissingDataException, IOException {
        super.init(processData);
        setup(getClass().getSimpleName(), ".cat");
    }

}
