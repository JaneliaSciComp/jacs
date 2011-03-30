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

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.blast.CreateRecruitmentBlastDatabaseTask;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 3, 2008
 * Time: 2:53:59 PM
 */
public class FormatRecruitmentDatabaseService extends CreateBlastDBService {

    protected void init(IProcessData processData) throws MissingDataException, IOException, DaoException, ServiceException {
        // Call the parent class and then massage the deflines after the fasta is available
        super.init(processData);

        // Add the sample name to the deflines
        String sampleName = task.getParameter(CreateRecruitmentBlastDatabaseTask.PARAM_SAMPLE_NAME);
        if (null==sampleName || "".equals(sampleName)) {
            throw new ServiceException("Cannot build blast databases for recruitment without a sample id for the data.");
        }
        Scanner scanner = new Scanner(new File(fastaFile.getFastaFilePath()));
        FileWriter writer=null;
        String newFile = fastaFile.getFastaFilePath()+".new";
        try {
            writer = new FileWriter(new File(newFile));
            while (scanner.hasNextLine()){
                String tmpLine = scanner.nextLine().trim();
                if (tmpLine.startsWith(">") && tmpLine.indexOf(RecruitmentDataHelper.DEFLINE_SAMPLE_NAME)<=0){
                    // If no /sample_name look for /source
                    if (tmpLine.indexOf(RecruitmentDataHelper.DEFLINE_SOURCE)>0) {
                        String tmpSource = tmpLine.substring(tmpLine.indexOf(RecruitmentDataHelper.DEFLINE_SOURCE)+
                                RecruitmentDataHelper.DEFLINE_SOURCE.length());
                        if (tmpSource.indexOf(" ")>0){
                            tmpSource=tmpSource.substring(0, tmpSource.indexOf(" "));
                        }
                        tmpLine = tmpLine+" "+RecruitmentDataHelper.DEFLINE_SAMPLE_NAME+tmpSource+"\n";
                    }
                    // If no /source add the string from the GUI
                    else {
                        tmpLine = tmpLine+" "+RecruitmentDataHelper.DEFLINE_SAMPLE_NAME+sampleName+"\n";
                    }
                }
                tmpLine+="\n";
                writer.write(tmpLine);
            }
        }
        finally {
            if (null!=writer){
                writer.close();
            }
            scanner.close();
        }
        // Now that the sample info exists, replace the files
        FileUtil.moveFileUsingSystemCall(fastaFile.getFastaFilePath(), fastaFile.getFastaFilePath()+".original");
        FileUtil.moveFileUsingSystemCall(newFile, fastaFile.getFastaFilePath());
    }

}
