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
 * Time: 2:34:09 PM
 */
public class HypotheticalService extends MgAnnoBaseService {

    public static final String ANNOTATION_INPUT_DATA_TYPE = "Hypothetical";

    /* SCRIPT DEPENDENCIES

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
            CAMERA
     */

    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            logger.info(getClass().getName() + " execute() start");

            // Step 1: parse
            File parseDir = new File(resultFile.getAbsolutePath() + "_hypoParseDir");
            parseDir.mkdirs();
            File parsedFile = new File(resultFile.getAbsolutePath());
            String parserStr = MetaGenoPerlConfig.getCmdPrefix() + parserCmd +
                    " --input_file " + inputFile.getAbsolutePath() +
                    " --input_type " + ANNOTATION_INPUT_DATA_TYPE + " " +
                    " --output_file " + parsedFile.getAbsolutePath() +
                    " --work_dir " + parseDir.getAbsolutePath();
            SimpleGridJobRunner job = new SimpleGridJobRunner(workingDir, parserStr, queue, parentTask.getParameter("project"), parentTask.getObjectId());
            if (!job.execute()) {
                throw new Exception("Grid job failed with cmd=" + parserStr);
            }
            addParsedFile(parsedFile);

            // Step 2: Clean parse directories
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
        setup(getClass().getSimpleName(), ".hypothetical.parsed");
    }

}
