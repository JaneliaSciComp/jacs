
package org.janelia.it.jacs.compute.service.metageno.anno;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.metageno.MetaGenoPerlConfig;
import org.janelia.it.jacs.compute.service.metageno.SimpleGridJobRunner;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 19, 2009
 * Time: 2:33:44 PM
 */
public class LipoproteinService extends MgAnnoBaseService {

    public static final String ANNOTATION_INPUT_DATA_TYPE = "LipoproteinMotifBSML";

    /* SCRIPT DEPENDENCIES

        lipoCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/lipoprotein_motif
            use strict;
            use warnings;
            use Pod::Usage;
            use Ergatis::IdGenerator;
            use Ergatis::Logger;
            use BSML::BsmlBuilder;
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev pass_through);
        parserCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/camera_parse_annotation_results_to_text_table.pl
            use strict;
            use warnings;
            use Carp;
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

    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            logger.info(getClass().getName() + " execute() start");

            // Step 1: lipoprotein_motif
            String lipoStr = MetaGenoPerlConfig.getCmdPrefix() + lipoCmd +
                    " --input " + inputFile.getAbsolutePath() +
                    " --output " + resultFile.getAbsolutePath() +
                    " --gzip_output 0 " +
                    " --id_repository " + getIdRepositoryDir().getAbsolutePath() +
                    " --is_mycoplasm " + lipoIsMyco;
            SimpleGridJobRunner job = new SimpleGridJobRunner(workingDir, lipoStr, queue, parentTask.getParameter("project"), parentTask.getObjectId());
            if (!job.execute()) {
                throw new Exception("Grid job failed with cmd=" + lipoStr);
            }

            // Step 2: parse bsml
            File parseDir = new File(resultFile.getAbsolutePath() + "_lipoParseDir");
            parseDir.mkdirs();
            File parsedFile = new File(resultFile.getAbsolutePath() + ".parsed");
            String parserStr = MetaGenoPerlConfig.getCmdPrefix() + parserCmd +
                    " --input_file " + resultFile.getAbsolutePath() +
                    " --input_type " + ANNOTATION_INPUT_DATA_TYPE + " " +
                    " --output_file " + parsedFile.getAbsolutePath() +
                    " --work_dir " + parseDir.getAbsolutePath();
            job = new SimpleGridJobRunner(workingDir, parserStr, queue, parentTask.getParameter("project"), parentTask.getObjectId());
            if (!job.execute()) {
                throw new Exception("Grid job failed with cmd=" + parserStr);
            }
            addParsedFile(parsedFile);

            // Step 3: Clean parse directories
            File[] parseFiles = parseDir.listFiles();
            for (File f : parseFiles) {
                f.delete();
            }
            parseDir.delete();

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
        setup(getClass().getSimpleName(), ".bsml");
    }

}
