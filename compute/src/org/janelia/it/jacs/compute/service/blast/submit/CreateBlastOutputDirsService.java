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

package org.janelia.it.jacs.compute.service.blast.submit;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.CreateOutputDirsService;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 28, 2008
 * Time: 1:28:38 PM
 */

public class CreateBlastOutputDirsService extends CreateOutputDirsService {

    public void execute(IProcessData processData) throws SubmitJobException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            init(processData);
            createOutputDirs();
            processData.putItem(BlastProcessDataConstants.BLAST_DEST_OUTPUT_DIR, outputDirs);
            processData.putItem(BlastProcessDataConstants.BLAST_QUERY_FILES, queryFiles);
            processData.putItem(BlastProcessDataConstants.BLAST_QUERY_OUTPUT_FILE_MAP, inputOutputDirMap);
        }
        catch (Exception e) {
            throw new SubmitJobException(e);
        }
    }

    protected void doAdditionalIntegrationPerInputOutput(File queryFile, File outputDir) throws Exception {
        FileUtil.copyFile(queryFile.getAbsolutePath() + ".seqCount", outputDir.getAbsolutePath() + File.separator + "seqCount");
    }

}
