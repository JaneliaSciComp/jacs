
package org.janelia.it.jacs.server.api;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.blast.persist.query.PersistQueryNodeRemote;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.server.access.UserDAO;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Aug 4, 2006
 * Time: 11:25:09 AM
 *
 */
public class BlastAPITest extends JacswebTestCase {
    private static final String TEST_USER_NAME = SystemConfigurationProperties.getString("junit.test.username");

    private BlastAPI blastAPI;
    private UserDAO userDAO;

    public BlastAPITest() {
        super(BlastAPITest.class.getName());
        setAutowireMode(AUTOWIRE_BY_NAME);
    }

    public BlastAPI getBlastAPI() {
        return blastAPI;
    }

    public void setBlastAPI(BlastAPI blastAPI) {
        this.blastAPI = blastAPI;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void testBlast() {
        String testSequence = ">mydefline\\nTTGGGGATCGTGCTGGGTGTCATTGTTGCTTTCGTGTCAGCGGTGGTTGCTGTTGGTCGCTTCCTGATTG";
        String testSubject = "GOS: move858 Assembled Sequences from 0.002-0.22 Chesapeake Bay (N)";
        blast("TestBlast " + new Date(),testSequence,testSubject,-1,-1);
    }

    public BlastTask blast(String jobName, String querySequence, String subjectDBName, int nExpectedResults, long timeout) {
        BlastTask blastTask = null;
        try {
            blastTask = startBLASTAndWaitForCompletion(jobName,querySequence,subjectDBName,timeout);
            verifyBLASTResults(blastTask.getObjectId(),nExpectedResults);
        } catch(Exception e) {
            fail("Submit BLAST " + querySequence + " against " + subjectDBName + " failed: " + e.toString());
        }
        return blastTask;
    }

    private FastaFileNode createFASTAQueryNode(String querySequence) throws Exception {
        PersistQueryNodeRemote persistQueryBean = (PersistQueryNodeRemote)
                EJBFactory.getRemoteInterface("remote/PersistBlastQueryNodeSLSB");
        Long fastaFileNodeId = persistQueryBean.saveFastaText(getTestUser().getUserLogin(),querySequence);
        return blastAPI.getNodeDAO().getFastaFileNode(fastaFileNodeId);
    }

    private BlastTask createBLASTTaskOnComputeResource(String jobName, String querySequence, String subjectDBName)
            throws Exception {
        // create the query node
        FastaFileNode queryNode = createFASTAQueryNode(querySequence);
        // retrieve the subject node
        BlastDatabaseFileNode subjectNode = (BlastDatabaseFileNode)blastAPI.getNodeDAO().getNodeByName(subjectDBName);
        assertTrue(subjectNode != null);
        String querySequenceType = queryNode.getSequenceType();
        String subjectSequenceType = subjectNode.getSequenceType();
        BlastTask[] availableTasks = blastAPI.getBlastPrograms(querySequenceType,subjectSequenceType);
        assertTrue(availableTasks.length > 0);
        BlastTask blastTask = availableTasks[0];
        blastTask.setJobName(jobName);
        blastTask.setOwner(getTestUser().getUserLogin());
        // set the query node
        blastTask.setParameter(BlastTask.PARAM_query,queryNode.getObjectId().toString());
        // set the subject database
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(String.valueOf(subjectNode.getObjectId()));
        ms.setActualUserChoices(dbList);
        blastTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        blastTask = (BlastTask)blastAPI.computeBean.saveOrUpdateTask(blastTask);
        return blastTask;
    }

    private User getTestUser() throws Exception {
        return userDAO.getUserByName(TEST_USER_NAME);
    }

    private BlastTask startBLASTAndWaitForCompletion(String jobName, String querySequence, String subjectDBName, long timeout)
            throws Exception {
        BlastTask  blastTask = createBLASTTaskOnComputeResource(jobName, querySequence, subjectDBName);
        // submit the task
        blastAPI.runBlast(getTestUser(),blastTask);
        // check job's status
        boolean completionStatus = verifyCompletion(blastTask.getObjectId(),timeout);
        assertTrue("BLAST " + blastTask.getParameterVO(BlastTask.PARAM_query) + " against " + subjectDBName + " timed out",
                completionStatus);
        return blastTask;
    }

    private boolean verifyCompletion(Long taskId,long timeout) throws Exception {
        long startTime = System.currentTimeMillis();
        boolean completionStatus = false;
        while(true) {
            blastAPI.getTaskDAO().getSessionFactory().getCurrentSession().clear();
            if(blastAPI.isTaskDone(taskId)) {
                completionStatus = true;
                break;
            } else {
                if(timeout > 0 && System.currentTimeMillis() - startTime > timeout) {
                    return completionStatus;
                }
            }
            Thread.sleep(500);
        }
        return completionStatus;
    }

    private void verifyBLASTResults(Long taskId, long expectedNumberOfResults)
            throws Exception {
        JobInfo blastJobInfo = blastAPI.getTaskDAO().getJobStatusByJobId(taskId);
        assertTrue(blastJobInfo.getStatus().equals(Event.COMPLETED_EVENT));
        if(expectedNumberOfResults >= 0) {
            assertTrue("Mismatch in the number of BLAST results: " +
                    blastJobInfo.getNumHits()  + " vs " + expectedNumberOfResults,
                    blastJobInfo.getNumHits() == expectedNumberOfResults);
        }
    }

}
