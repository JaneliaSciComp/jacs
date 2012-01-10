
package org.janelia.it.jacs.web.gwt.frv.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 19, 2008
 * Time: 5:09:02 PM
 */
public interface RecruitmentServiceAsync {
    public void runUserBlastRecruitment(String queryNodeId, AsyncCallback callback);

    public void runRecruitmentJob(RecruitableJobInfo job, AsyncCallback callback);

    public void getLegend(String nodeId, AsyncCallback callback);

    public void getAnnotationInfoForSelection(String nodeId, long ntPosition, String annotationFilter, AsyncCallback callback);

    public void getAnnotationInfoForRange(String nodeId, long ntStartPosition, long ntStopPosition, String annotationFilterString, AsyncCallback callback);

    public void getRVSampleData(AsyncCallback callback);
}
