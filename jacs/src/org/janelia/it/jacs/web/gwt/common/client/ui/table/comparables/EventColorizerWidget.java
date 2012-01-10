
package org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables;

import com.google.gwt.user.client.ui.HTML;
import org.janelia.it.jacs.model.tasks.Event;

/**
 * Sets the widget style to an appropriate display style (e.g. black/red/green) based on the Event status (running/error/success)
 *
 * @author Michael Press
 */
public class EventColorizerWidget extends HTML {
    private String _status;
    private static final String TEXT_STYLE = "text";
    private static final String SUCCESS_STYLE = "jobSuccess";
    private static final String ERROR_STYLE = "jobError";
    private static final String RUNNING_STYLE = "jobRunning";

    public EventColorizerWidget(String statusString) {
        if (statusString == null)
            statusString = "";

        _status = statusString;
        init();
    }

    private void init() {
        setText(_status);

        setStyleName(TEXT_STYLE);
        if (_status.equals(Event.COMPLETED_EVENT))
            addStyleName(SUCCESS_STYLE);
        else if (_status.equals(Event.ERROR_EVENT) || _status.equals(Event.CANCELED_EVENT) || _status.equals(Event.DELETED_EVENT))
            addStyleName(ERROR_STYLE);
        else if (_status.equals(Event.GENERATING_EVENT) || _status.equals(Event.CREATED_EVENT) || _status.equals(Event.PENDING_EVENT))
            addStyleName(RUNNING_STYLE);
    }
}
