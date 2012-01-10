
package org.janelia.it.jacs.web.gwt.blast.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.common.BlastTaskVO;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.tasks.Task;


/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Nov 20, 2006
 * Time: 9:40:35 AM
 * <p/>
 * Required by GWT.  Can call this, with call-back, and results will thus be delivered
 * to the client, from the server-side of the AJAX call.
 */
public interface BlastServiceAsync {
    public void getBlastPrograms(String querySequenceType, String subjectSequenceType, AsyncCallback callback);

    public void getBlastableSubjectSets(String seqType, AsyncCallback callback);

    public void getBlastableSubjectSetByNodeId(String nodeId, AsyncCallback<BlastableNodeVO> callback);

    public void runBlastJob(Task targetTask, AsyncCallback callback);

    public void getSiteLocations(String project, AsyncCallback callback);

    public void getNodeIdVsSiteLocation(String project, AsyncCallback callback);

    public void getPrepopulatedBlastTask(String taskId, AsyncCallback<BlastTaskVO> callback);
}
