/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
