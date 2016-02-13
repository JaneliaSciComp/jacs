/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.largevolume;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.tiledMicroscope.TmNeuronPBUpdateTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Creates and initiates a task for moving TmNeuron definitions from Entity
 * to serialized-protobuf EntityData.
 *
 * @author Leslie L Foster
 */
public class TmNeuronPBUpdateTest {
	public static void main(String[] args) throws Exception {
		// This can be any unconverted workspace.
		String workspaceIdStr = "2235226784322814120";
		//2232632544023543976";
		HashSet<TaskParameter> taskParameters = new HashSet<>();
		taskParameters.add(new TaskParameter(TmNeuronPBUpdateTask.PARAM_workspaceId, workspaceIdStr, null));
		Task task = new GenericTask(new HashSet<Node>(), "group:mouselight", new ArrayList<Event>(),
				taskParameters, TmNeuronPBUpdateTask.PROCESS_NAME, TmNeuronPBUpdateTask.PROCESS_NAME);		
		
		Hashtable environment = new Hashtable();
		environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
		environment.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
		environment.put(Context.PROVIDER_URL, "jnp://foster-ws.janelia.priv:1199");
		InitialContext context = new InitialContext(environment);
        System.out.println("-->> connected successfully to server");

        System.out.println("\n*************************************\n"); 		
		ComputeBeanRemote computeBean = (ComputeBeanRemote) context.lookup("compute/ComputeEJB/remote");		
		if (computeBean == null) {
			throw new RuntimeException("Failed to get EJB");
		}
		task = computeBean.saveOrUpdateTask(task);
		computeBean.submitJob(task.getJobName(), task.getObjectId());
		/*
		 public Task submitJob(String processDefName, String displayName) throws Exception {
		 HashSet<TaskParameter> taskParameters = new HashSet<>();
		 return submitJob(processDefName, displayName, taskParameters);
		 }

		 public Task submitJob(String processDefName, String displayName, HashSet<TaskParameter> parameters) throws Exception {
		 GenericTask task = new GenericTask(new HashSet<Node>(), SessionMgr.getSubjectKey(), new ArrayList<Event>(),
		 parameters, processDefName, displayName);
		 return submitJob(task);
		 }

		 private Task submitJob(GenericTask genericTask) throws Exception {
		 Task task = saveOrUpdateTask(genericTask);
		 submitJob(task.getTaskName(), task);
		 return task;
		 }

		 public TaskRequest submitJob(String processDefName, Task task) throws Exception {
		 FacadeManager.getFacadeManager().getComputeFacade().submitJob(processDefName, task.getObjectId());
		 return new TaskRequest(new TaskFilter(task.getJobName(), task.getObjectId()));
		 }
		*/
	}

}
