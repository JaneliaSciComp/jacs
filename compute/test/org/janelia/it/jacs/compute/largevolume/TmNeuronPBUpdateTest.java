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
		/*		  		  		 
		 2238556804587979655
		 2238556844924601223
		 2238556889711379335
		 2238556943146812295
		 2238556988264940423
		 2238557031063618439
		 2238557075917505415
		 2238557124034560903
		 2238557168712287111
		 2238557211070563207
		 2238557248961905543
		 2238557289906701191
		 2238557338787120007
		 2238557380528833415
		 2238557427538592647
        */
		String workspaceIdStr = "2238556769146110855";
        //2238556729870648199";
        //2238556687109718919";
		//2238556639701500807";
		//2238556632118199175";
		//2235226784322814120";
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
	}

}
