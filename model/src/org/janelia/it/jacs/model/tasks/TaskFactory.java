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

package org.janelia.it.jacs.model.tasks;

import org.janelia.it.jacs.model.tasks.blast.*;

/**
 * @author Tareq Nabeel
 */
public class TaskFactory {

    public static Task createTask(String taskType) {
        Task task;
        if (BlastNTask.BLASTN_NAME.equals(taskType)) {
            task = new BlastNTask();
        }
        else if (BlastPTask.BLASTP_NAME.equals(taskType)) {
            task = new BlastPTask();
        }
        else if (BlastXTask.BLASTX_NAME.equals(taskType)) {
            task = new BlastXTask();
        }
        else if (MegablastTask.MEGABLAST_NAME.equals(taskType)) {
            task = new MegablastTask();
        }
        else if (TBlastNTask.TBLASTN_NAME.equals(taskType)) {
            task = new TBlastNTask();
        }
        else if (TBlastXTask.TBLASTX_NAME.equals(taskType)) {
            task = new TBlastXTask();
        }
        else {
            throw new IllegalArgumentException("taskType:" + taskType);
        }
        return task;
    }
}
