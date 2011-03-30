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

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.*;
import org.janelia.it.jacs.model.tasks.psiBlast.PsiBlastTask;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.io.File;
import java.util.Properties;

/**
 * @author Sean Murphy
 */
public class BlastCommand {
    private static final String BLAST_CMD_PROP = "BlastServer.BlastCmd";
    private String blastCmd = SystemConfigurationProperties.getString(BLAST_CMD_PROP);
    private static Properties programMap = null;

    static {
        programMap = new Properties();
        programMap.setProperty(BlastNTask.BLASTN_NAME, "blastn");
        programMap.setProperty(MegablastTask.MEGABLAST_NAME, "blastn -n T");
        programMap.setProperty(BlastPTask.BLASTP_NAME, "blastp");
        programMap.setProperty(BlastXTask.BLASTX_NAME, "blastx");
        programMap.setProperty(TBlastNTask.TBLASTN_NAME, "tblastn");
        programMap.setProperty(TBlastXTask.TBLASTX_NAME, "tblastx");
        programMap.setProperty(PsiBlastTask.BLAST_NAME, "blastpgp");
    }

    public String getCommandString(Task task, File subjectDatabase, long totalDatabaseLength, String tempBlastOutputFileName) throws ParameterException {
        StringBuffer sb = new StringBuffer();
        String programName = programMap.getProperty(task.getTaskName());
        if (programName == null)
            throw new RuntimeException("Could not find blast program mapping for createtask type " + task.getTaskName());
        if (task.getTaskName().equals(BlastNTask.BLASTN_NAME)) {
            sb.append(blastCmd).append(" ").append(((BlastNTask) task).generateCommandStringNotIncludingIOParams());
            sb.append((" -p " + programName));
        }
        else if (task.getTaskName().equals(MegablastTask.MEGABLAST_NAME)) {
            sb.append(blastCmd).append(" ").append(((MegablastTask) task).generateCommandStringNotIncludingIOParams());
            sb.append((" -p " + programName));
        }
        else if (task.getTaskName().equals(BlastPTask.BLASTP_NAME)) {
            sb.append(blastCmd).append(" ").append(((BlastPTask) task).generateCommandStringNotIncludingIOParams());
            sb.append((" -p " + programName));
        }
        else if (task.getTaskName().equals(BlastXTask.BLASTX_NAME)) {
            sb.append(blastCmd).append(" ").append(((BlastXTask) task).generateCommandStringNotIncludingIOParams());
            sb.append((" -p " + programName));
        }
        else if (task.getTaskName().equals(TBlastNTask.TBLASTN_NAME)) {
            sb.append(blastCmd).append(" ").append(((TBlastNTask) task).generateCommandStringNotIncludingIOParams());
            sb.append((" -p " + programName));
        }
        else if (task.getTaskName().equals(TBlastXTask.TBLASTX_NAME)) {
            sb.append(blastCmd).append(" ").append(((TBlastXTask) task).generateCommandStringNotIncludingIOParams());
            sb.append((" -p " + programName));
        }
        else if (PsiBlastTask.BLAST_NAME.equals(task.getTaskName())) {
            sb.append(task.getTaskName()).append(" ").append(((PsiBlastTask) task).generateCommandStringNotIncludingIOParams());
        }
        else {
            throw new RuntimeException("Could not find generateCommand method for blast type " + task.getTaskName());
        }
        sb.append((" -d " + subjectDatabase.getAbsolutePath()));
        sb.append((" -i " + "$BLASTQUERY_FILE"));
        sb.append((" -o " + tempBlastOutputFileName));
        // NOTE: Doug says to use a very high number to normalize the blast results.  This allows him to compare results
        // even after the blast database may have changed in size
        sb.append((" -z " + totalDatabaseLength));
        sb.append((" -m 7 ")); // Output XML
        return sb.toString();
    }

}
