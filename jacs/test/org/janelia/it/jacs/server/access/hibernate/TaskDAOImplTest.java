
package org.janelia.it.jacs.server.access.hibernate;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastNTask;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.access.UserDAO;
import org.janelia.it.jacs.shared.tasks.BlastJobInfo;
import org.janelia.it.jacs.shared.tasks.SearchJobInfo;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Aug 4, 2006
 * Time: 11:25:09 AM
 */
public class TaskDAOImplTest extends JacswebTestCase {
    private TaskDAO taskDAO;
    private UserDAO userDAO;

    public TaskDAOImplTest() {
        super(TaskDAOImplTest.class.getName());
    }

    public TaskDAO getTaskDAO() {
        return taskDAO;
    }

    public void setTaskDAO(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void testGetTaskById() {
        Task task;
        try {
            task=taskDAO.getTaskById(1044410836048675132L);
            assertNotNull(task);
            BlastTask blastTask=(BlastTask)task;
            Event startEvent=blastTask.getFirstEvent();
            Event endEvent=blastTask.getLastEvent();
            assertTrue(endEvent.getTimestamp().getTime()-startEvent.getTimestamp().getTime() > 0);
            TaskDAOImplTest.assertEquals(task.getTaskName(),"blastx");
            TaskDAOImplTest.assertEquals(task.getOwner(),"kli");
            Set parameterKeys=task.getParameterKeySet();

            assertNotNull(parameterKeys);
            assertTrue(parameterKeys.size() > 0);
            for (Object parameterKey : parameterKeys) {
                String key = (String) parameterKey;
                if (key != null) {
                    if (task.getParameter(key) == null) {
                        fail("Parameter map for task has null value for key=" + key);
                    }
                }
            }
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

    public void testSaveOrUpdateTask() {
        try {
            createTask();
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

    public void testGetJobById() {
        try {
            assertStatusInfo((BlastJobInfo)taskDAO.getJobStatusByJobId(new Long("1045126164458242436")), "1045126164458242436");
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

    public void testPurgeJobById() {
        try {
            Task t=createTask();
            taskDAO.purgeTask(t.getObjectId());
            assertNull(taskDAO.getJobStatusByJobId(t.getObjectId()));
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

    public void testGetSystemTaskQueryNames() {
        try {
            taskDAO.getTaskQueryNamesForUser(User.SYSTEM_USER_LOGIN);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

    public void testGetPagedTasksForUserLoginByClass() {
        try {
            String testUser= User.SYSTEM_USER_LOGIN;
            Class taskClass=RecruitmentViewerTask.class;
            int maxRecords=30;
            String likeString="";
            Integer nTasks=taskDAO.getNumTasksForUserLoginByClass(testUser,
                    likeString,
                    taskClass.getName(),
                    false);
            List<Task> taskList=taskDAO.getPagedTasksForUserLoginByClass(testUser,
                    likeString,
                    taskClass.getName(),
                    0,
                    maxRecords,
                    new SortArgument[] {
                            new SortArgument(BlastJobInfo.SORT_BY_NUM_HITS, SortArgument.SORT_ASC),
                            new SortArgument(BlastJobInfo.SORT_BY_QUERY_SEQ, SortArgument.SORT_DESC),
                            new SortArgument(BlastJobInfo.SORT_BY_SUBJECT_DB, SortArgument.SORT_ASC),
                            new SortArgument(BlastJobInfo.SORT_BY_JOB_NAME, SortArgument.SORT_DESC)
                    },
                    false);
            assertTrue(nTasks < maxRecords ?
                    taskList.size() == nTasks :
                    taskList.size() <= maxRecords);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

// Deprecated method for taskDAO
//
//    public void testGetBlastJobsStatusForUserLogin() {
//        try {
//            String testUser="testuser";
//            int maxRecords=50;
//            Integer nBlastJobResults =
//                    taskDAO.getNumTasksForUserLoginByClass(testUser, null, "BlastTask");
//            List<org.janelia.it.jacs.shared.tasks.BlastJobInfo> blastJobsStatusList = taskDAO.getPagedBlastJobsStatusForUserLogin(
//                    testUser,
//                    0,
//                    maxRecords,
//                    new SortArgument[] {
//                            new SortArgument(BlastJobInfo.SORT_BY_NUM_HITS,SortArgument.SORT_ASC),
//                            new SortArgument(BlastJobInfo.SORT_BY_QUERY_SEQ, SortArgument.SORT_DESC),
//                            new SortArgument(BlastJobInfo.SORT_BY_SUBMITTED, SortArgument.SORT_ASC),
//                            new SortArgument(BlastJobInfo.SORT_BY_STATUS,SortArgument.SORT_ASC),
//                            new SortArgument(BlastJobInfo.SORT_BY_SUBJECT_DB, SortArgument.SORT_ASC),
//                            new SortArgument(BlastJobInfo.SORT_BY_JOB_NAME, SortArgument.SORT_DESC)
//                    });
//            assertTrue(nBlastJobResults < maxRecords ?
//                    blastJobsStatusList.size() == nBlastJobResults :
//                    blastJobsStatusList.size() <= maxRecords);
//        } catch (Exception ex) {
//            failFromException(ex);
//        }
//    }

    public void testGetSearchInfoForUserLogin() {
        try {
            String testUser="testuser";
            int maxRecords=50;
            List<SearchJobInfo> searchJobInfoList = taskDAO.getPagedSearchInfoForUserLogin(
                    testUser,
                    0,
                    maxRecords,
                    new SortArgument[] {
                            new SortArgument(BlastJobInfo.SORT_BY_SUBMITTED, SortArgument.SORT_DESC),
                            new SortArgument(BlastJobInfo.SORT_BY_NUM_HITS, SortArgument.SORT_DESC),
                            new SortArgument(BlastJobInfo.SORT_BY_QUERY_SEQ, SortArgument.SORT_DESC),
                            new SortArgument(BlastJobInfo.SORT_BY_STATUS, SortArgument.SORT_ASC),
                            new SortArgument(BlastJobInfo.SORT_BY_JOB_NAME, SortArgument.SORT_DESC)
                    });
            assertTrue(searchJobInfoList.size() >= 0);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

    private void assertStatusInfo(BlastJobInfo jobInfo, String jobId) {
        assertNotNull(jobInfo);
        if (jobId != null)
            assertEquals(jobInfo.getJobId(), jobId);
        assertEquals(jobInfo.getJobname(), "gf");
        assertEquals(jobInfo.getStatus(),  Event.COMPLETED_EVENT);
        assertEquals(jobInfo.getProgram(), "blastn");
    }

    private Task createTask() throws Exception {
        BlastNTask blastNTask=new BlastNTask();
        blastNTask.setOwner("testUser");
        Event startEvent=new Event();
        startEvent.setTimestamp(new Date());
        startEvent.setDescription("start test");
        startEvent.setEventType(Event.CREATED_EVENT);
        blastNTask.addEvent(startEvent);
        taskDAO.saveOrUpdateTask(blastNTask);
        Thread.sleep(10);
        Event endEvent=new Event();
        endEvent.setTimestamp(new Date());
        endEvent.setDescription("end test");
        endEvent.setEventType(Event.COMPLETED_EVENT);
        blastNTask.addEvent(endEvent);
        taskDAO.saveOrUpdateTask(blastNTask);
        return blastNTask;
    }

}
