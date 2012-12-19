package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Local interface to ComputeBeanImpl
 */
@Local
public interface ComputeBeanLocal extends ComputeBeanRemote {

	public void updateTaskStatus(long taskId, String status, String comment) throws DaoException;

	public void saveTaskMessages(long taskId, Set<TaskMessage> messages) throws DaoException;

	public Object genericSave(Object object) throws DaoException;

	public void genericDelete(Object object) throws DaoException;

	public Object genericLoad(Class c, Long id) throws DaoException;

	public void recordProcessSuccess(ProcessDef processDef, Long processId);

	public void recordProcessError(ProcessDef processDef, Long processId, Throwable e);

	public void setBlastHitsForNode(Long nodeId, Long numHits) throws DaoException;

	public List<Node> getNodesByClassAndUser(String className, String username) throws DaoException;

	public List getSampleInfo() throws DaoException;

	public String getSystemConfigurationProperty(String propertyKey);

	public List getHeaderDataForFRV(ArrayList readAccList) throws DaoException;

	public Task getRecruitmentFilterTaskByUserPipelineId(Long objectId) throws DaoException;

	public void setParentTaskId(Long parentTaskId, Long childTaskId) throws DaoException;

	public HashMap<String, String> getChildTaskStatusMap(Long objectId) throws Exception;
	
	public boolean createUser(String newUserName) throws DaoException;
}
