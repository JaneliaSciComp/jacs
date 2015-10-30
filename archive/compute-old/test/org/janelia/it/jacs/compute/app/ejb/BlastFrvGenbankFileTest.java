
package org.janelia.it.jacs.compute.app.ejb;

import org.janelia.it.jacs.compute.ComputeTestCase;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.mbean.RecruitmentNodeManager;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 13, 2008
 * Time: 3:36:08 PM
 *
 */
public class BlastFrvGenbankFileTest extends ComputeTestCase {
    RecruitmentNodeManager recruitmentNodeManager;

    public BlastFrvGenbankFileTest() {
        super(BlastFrvGenbankFileTest.class.getName());
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testBlastFrvGenbankFile() {
        String filePath="S:\\filestore\\system\\genomeProject\\1167236322503426404\\NC_002253.gbk";
        String ownerLogin="testuser";
        long taskId=0L;
        // Launch test - this does not block but uses a queue
        try {
            recruitmentNodeManager=new RecruitmentNodeManager();
            taskId=recruitmentNodeManager.blastFrvASingleGenbankFileReturnId(filePath, ownerLogin);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.toString());
        }
        // Next, poll and wait for result, within a limit
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            String[] statusTypeAndValue = computeBean.getTaskStatus(taskId);
            int sanityCheck=300; // five-minutes
            while (!Task.isDone(statusTypeAndValue[0])) {
                sanityCheck--;
                if (sanityCheck<=0)
                    throw new Exception("Test exceeded maximum permitted time");
                Thread.sleep(1000);
                statusTypeAndValue = computeBean.getTaskStatus(taskId);
            }
            if (!statusTypeAndValue[0].equals(Event.COMPLETED_EVENT))
                throw new Exception("Task finished with status other than complete="+statusTypeAndValue[0]);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.toString());
        }
    }

}
