
package org.janelia.it.jacs.web.gwt.common.client.service.view;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 29, 2007
 * Time: 5:02:48 PM
 */
public interface ViewServiceAsync {

    void getTileForView(Task viewTask, String tilename, AsyncCallback callback);

}
