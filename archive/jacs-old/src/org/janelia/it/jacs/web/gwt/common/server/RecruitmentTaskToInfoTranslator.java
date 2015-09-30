
package org.janelia.it.jacs.web.gwt.common.server;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerRecruitmentTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.janelia.it.jacs.model.vo.DoubleParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;

import java.text.DecimalFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 13, 2007
 * Time: 4:08:13 PM
 */
public class RecruitmentTaskToInfoTranslator {

    public static RecruitableJobInfo getInfoForRecruitmentResultTask(RecruitmentViewerFilterDataTask task)
            throws ParameterException {
        RecruitableJobInfo info = new RecruitableJobInfo();
        info.setJobId(String.valueOf(task.getObjectId()));
        info.setUsername(task.getOwner());
        info.setJobname(task.getJobName());
        info.setQueryName(task.getParameter(RecruitmentViewerTask.QUERY));
        info.setSubjectName(task.getParameter(RecruitmentViewerTask.SUBJECT));
        Long tmpNumHits = null;
        if (null != task.getParameter(RecruitmentViewerFilterDataTask.NUM_HITS)) {
            tmpNumHits = new Long(task.getParameter(RecruitmentViewerFilterDataTask.NUM_HITS));
            info.setNumHitsFormatted(new DecimalFormat("###,###").format(new Long(task.getParameter(RecruitmentViewerFilterDataTask.NUM_HITS))));
        }
        info.setNumHits(tmpNumHits);
        info.setPercentIdentityMin(((DoubleParameterVO) task
                .getParameterVO(RecruitmentViewerFilterDataTask.PERCENT_ID_MIN))
                .getActualValue().intValue());
        info.setPercentIdentityMax(((DoubleParameterVO) task
                .getParameterVO(RecruitmentViewerFilterDataTask.PERCENT_ID_MAX))
                .getActualValue().intValue());
        long refStart = ((DoubleParameterVO) task.getParameterVO(RecruitmentViewerFilterDataTask.REF_BEGIN_COORD)).getActualValue().longValue();
        long refEnd = ((DoubleParameterVO) task.getParameterVO(RecruitmentViewerFilterDataTask.REF_END_COORD)).getActualValue().longValue();
        info.setRefAxisBeginCoord(refStart);
        info.setRefAxisEndCoord(refEnd);
        info.setMateInfo(task.getParameter(RecruitmentViewerFilterDataTask.MATE_BITS));
        info.setGenomeLengthFormatted(new DecimalFormat("###,###").format(refEnd - refStart));
        info.setAnnotationFilterString(task.getAnnotationFilterString());

        // Set the gi number and the node id
        Node dataNode = task.getInputNodes().iterator().next();
        info.setRecruitableNodeId(dataNode.getObjectId().toString());
        RecruitmentViewerRecruitmentTask recruitmentTask = (RecruitmentViewerRecruitmentTask) dataNode.getTask();
        info.setGiNumberOfSourceData(recruitmentTask.getParameter(RecruitmentViewerRecruitmentTask.GI_NUMBER));
        info.setSamplesRecruited(task.getSampleListAsCommaSeparatedString());
        info.setMateSpanPoint(task.getMateSpanPoint());
        info.setColorizationType(task.getColorizationType());

        // for now this works only if there's one result recruitment node per task
        for (Object on : task.getOutputNodes()) {
            Node resultNode = (Node) on;
            if (resultNode instanceof RecruitmentResultFileNode) {
                info.setRecruitmentResultsFileNodeId(String.valueOf(resultNode.getObjectId()));
                break;
            }
        }

        // Copy the events
        // Event-based attributes
        Event lastEvent = task.getLastEvent();
        if (lastEvent != null) {
            info.setStatusDescription(lastEvent.getDescription());
            info.setStatus(lastEvent.getEventType());
        }
        Event firstEvent = task.getFirstEvent();
        if (firstEvent != null) {
            info.setSubmitted(new Date(firstEvent.getTimestamp().getTime()));
        }

        // NOTE: Might need other information.  See TaskDAOImpl:createBlastStatusInfo
        // The RecruitableJobInfo explicitly handles information when it could actually
        // copy the param map in one step.  That would probably be safer.
        return info;
    }
}
