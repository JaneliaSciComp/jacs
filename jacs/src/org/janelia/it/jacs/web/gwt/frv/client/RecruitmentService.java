
package org.janelia.it.jacs.web.gwt.frv.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.janelia.it.jacs.shared.processors.recruitment.AnnotationTableData;
import org.janelia.it.jacs.shared.processors.recruitment.ProjectData;
import org.janelia.it.jacs.shared.processors.recruitment.SampleData;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.model.tasks.LegendItem;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 19, 2008
 * Time: 5:09:02 PM
 */
public interface RecruitmentService extends RemoteService {
    /**
     * Utility/Convenience class.
     * Use RecruitmentService.App.getInstance() to access static instance of RecruitmentServiceAsync
     */
    public static class App {
        private static RecruitmentServiceAsync ourInstance = null;

        public static synchronized RecruitmentServiceAsync getInstance() {
            if (ourInstance == null) {
                ourInstance = (RecruitmentServiceAsync) GWT.create(RecruitmentService.class);
                ((ServiceDefTarget) ourInstance).setServiceEntryPoint(GWT.getModuleBaseURL() + "org.janelia.it.jacs.web.gwt.frv.Frv/RecruitmentService");
            }
            return ourInstance;
        }
    }

    public String runRecruitmentJob(RecruitableJobInfo job) throws GWTServiceException;

    public LegendItem[] getLegend(String nodeId);

    public String getAnnotationInfoForSelection(String nodeId, long ntPosition, String annotationFilter);

    public List<AnnotationTableData> getAnnotationInfoForRange(String nodeId, long ntStartPosition, long ntStopPosition, String annotationFilter);

    public HashMap<ProjectData, ArrayList<SampleData>> getRVSampleData();

    public void runUserBlastRecruitment(String queryNodeId) throws GWTServiceException;

}
