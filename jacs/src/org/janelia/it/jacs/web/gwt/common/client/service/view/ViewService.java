
package org.janelia.it.jacs.web.gwt.common.client.service.view;

import com.google.gwt.user.client.rpc.RemoteService;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 29, 2007
 * Time: 5:01:53 PM
 */
public interface ViewService extends RemoteService {

    public void getTileForView(Task viewTask, String tilename);

}
