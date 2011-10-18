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

package org.janelia.it.jacs.compute.api;

import java.util.*;

import javax.ejb.Local;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.model.user_data.hmmer.HmmerPfamDatabaseNode;

/**
 * Local interface to ComputeBeanImpl
 */
@Local
public interface ComputeBeanLocal {
    
     public void updateTaskStatus(long taskId, String status, String comment) throws DaoException;
     public Node getResultNodeByTaskId(long taskId) throws DaoException;
     public List<Node> getResultNodesByTaskId(long taskId) throws DaoException;
     public void saveTaskMessages(long taskId, Set<TaskMessage> messages) throws DaoException;
     public Node saveOrUpdateNode(Node node) throws DaoException;
     public Task saveOrUpdateTask(Task task) throws DaoException;
     public User saveOrUpdateUser(User user) throws DaoException;
     public String[] getTaskStatus(long taskId) throws DaoException;
     public User getUserByName(String name);
     public void removePreferenceCategory(String categoryName) throws DaoException;
     public boolean buildUserFilestoreDirectory(String userLoginName);
     public Event saveEvent(Long taskId, String eventType, String description, Date timestamp) throws DaoException;
     public void setTaskNote(long taskId, String note) throws DaoException;
     public void addTaskNote(long taskId, String note) throws DaoException;
     public Object genericSave(Object object) throws DaoException;
     public void genericDelete(Object object) throws DaoException;
     public Object genericLoad(Class c, Long id) throws DaoException;
     public BlastResultNode getBlastHitResultDataNodeByTaskId(Long taskId);
     public Long getBlastHitCountByTaskId(Long taskId) throws DaoException;
     public void recordProcessSuccess(ProcessDef processDef, Long processId);
     public void recordProcessError(ProcessDef processDef, Long processId, Throwable e);
     public Map<Long,String> getAllTaskPvoStrings();
     public Task getTaskForNodeId(long nodeId);
     public Node getNodeById(long nodeId);
     public void setRVHitsForNode(Long recruitmentNodeId, String numRecruited);
     public void setBlastHitsForNode(Long nodeId, Long numHits) throws DaoException;
     public void submitJob(String processDefName,long taskId);
     public Long submitJob(String processDefName,Map<String, Object> processConfiguration);
     public void submitJobs(String processDefName, List<Long> taskIds);

    public List<Node> getNodesByClassAndUser(String className, String username) throws DaoException;

    public List getSampleInfo() throws DaoException;

    public String getSystemConfigurationProperty(String propertyKey);

    public List getHeaderDataForFRV(ArrayList readAccList) throws DaoException;
    public int getNumCategoryResults(Long nodeId, String category); 
    public Node createNode(Node node) throws DaoException;
    public List<Node> getNodeByName(String nodeName) throws DaoException;

    public void setSystemDataRelatedToGiNumberObsolete(String giNumber) throws DaoException;
    public List<HmmerPfamDatabaseNode> getHmmerPfamDatabases();

    public List<Task> getUserTasks(String userLogin) throws Exception;
    public List<Task> getUserParentTasks(String userLogin) throws Exception;
    public List<Task> getUserTasksByType(String simpleName, String userName);
    public List<Event> getEventsForTask(long taskId) throws DaoException; 

    public Task getRecruitmentFilterTaskByUserPipelineId(Long objectId) throws DaoException;

    public void setParentTaskId(Long parentTaskId, Long childTaskId) throws DaoException;

    public List<User> getAllUsers();

    public List<Task> getChildTasksByParentTaskId(long taskId);

    public Long getSystemDatabaseIdByName(String databaseName);

    public void validateBlastTaskQueryDatabaseMatch(BlastTask blastTask) throws Exception;

    public void deleteTaskById(Long taskId) throws Exception;
    public void cancelTaskById(Long taskId) throws Exception;
    public boolean deleteNode(String username, Long nodeId, boolean clearFromFilestoreIfAppropriate);

    public void stopContinuousExecution(long taskId) throws ServiceException;
    public Task getTaskById(long taskId);
    public List<Long> getTaskTreeIdList(Long taskId) throws Exception;
    public HashSet<String> getProjectCodes() throws Exception;
    //public void testAccFile();

    public HashMap<String, String> getChildTaskStatusMap(Long objectId) throws Exception;
}
