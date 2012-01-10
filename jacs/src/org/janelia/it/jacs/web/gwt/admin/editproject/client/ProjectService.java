
package org.janelia.it.jacs.web.gwt.admin.editproject.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import java.util.Map;


public interface ProjectService extends RemoteService {
    /**
     * Utility/Convinience class.
     * Use ProjectService.App.getInstance() to access static instance of ProjectServiceAsync
     */
    public static class App {
        private static ProjectServiceAsync ourInstance = null;

        public static synchronized ProjectServiceAsync getInstance() {
            if (ourInstance == null) {
                ourInstance = (ProjectServiceAsync) GWT.create(ProjectService.class);
                ((ServiceDefTarget) ourInstance).setServiceEntryPoint(GWT.getModuleBaseURL() + "org.janelia.it.jacs.web.gwt.admin.editproject.EditProject/ProjectService");
            }
            return ourInstance;
        }
    }


    public String getProjectName();


    public Map getAllProjects();


}
