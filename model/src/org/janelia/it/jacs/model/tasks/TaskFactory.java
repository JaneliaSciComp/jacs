
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
