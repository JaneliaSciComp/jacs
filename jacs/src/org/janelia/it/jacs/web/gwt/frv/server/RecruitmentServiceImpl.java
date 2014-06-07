
package org.janelia.it.jacs.web.gwt.frv.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.server.api.RecruitmentAPI;
import org.janelia.it.jacs.shared.processors.recruitment.AnnotationTableData;
import org.janelia.it.jacs.shared.processors.recruitment.ProjectData;
import org.janelia.it.jacs.shared.processors.recruitment.SampleData;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.model.tasks.LegendItem;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 19, 2008
 * Time: 5:09:02 PM
 */
public class RecruitmentServiceImpl extends JcviGWTSpringController implements RecruitmentService {

    static Logger logger = Logger.getLogger(RecruitmentServiceImpl.class.getName());
    private RecruitmentAPI recruitmentAPI;

    public void setRecruitmentAPI(RecruitmentAPI recruitmentAPI) {
        this.recruitmentAPI = recruitmentAPI;
    }

    public String runRecruitmentJob(RecruitableJobInfo job) throws GWTServiceException {
        try {
            return recruitmentAPI.runRecruitmentJob(job, getSessionUser().getUserLogin());
        }
        catch (Throwable e) {
            logger.error("Exception: " + e.getMessage());
            throw new GWTServiceException(e.getMessage());
        }
    }

    public LegendItem[] getLegend(String nodeId) {
        try {
            return recruitmentAPI.getLegend(nodeId);
        }
        catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
        }
        return null;
    }

    public String getAnnotationInfoForSelection(String nodeId, long ntPosition, String annotationFilter) {
        try {
            return recruitmentAPI.getAnnotationInfoForSelection(nodeId, ntPosition, annotationFilter);
        }
        catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
        }
        return null;
    }

    public List<AnnotationTableData> getAnnotationInfoForRange(String nodeId, long ntStartPosition, long ntStopPosition, String annotationFilter) {
        try {
            return recruitmentAPI.getAnnotationInfoForRange(nodeId, ntStartPosition, ntStopPosition, annotationFilter);
        }
        catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
        }
        return null;
    }

    public HashMap<ProjectData, ArrayList<SampleData>> getRVSampleData() {
        try {
            return recruitmentAPI.getRVSampleData();
        }
        catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
        }
        return null;
    }

    public void runUserBlastRecruitment(String queryNodeId) throws GWTServiceException {
        try {
            recruitmentAPI.runUserBlastRecruitment(queryNodeId, getSessionUser().getUserLogin());
        }
        catch (Throwable e) {
            logger.error("Exception: " + e.getMessage());
            throw new GWTServiceException(e.getMessage());
        }
    }


}