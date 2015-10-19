
package org.janelia.it.jacs.web.gwt.cpd.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * @author Todd Safford
 */
public class ClosurePrimerDesignPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.cpd.client.ClosurePrimerDesignPanel");

    public static final String DEFAULT_UPLOAD_MESSAGE = "File upload successful - click Apply to continue.";
    public static final String UPLOAD_SEQUENCE_NAME_PARAM = "uploadSequenceName";

    private TitledBox _mainPanel;
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);


    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public ClosurePrimerDesignPanel(String title, String taskId, JobSubmissionListener listener) {
        init(title, taskId);
    }

    private void init(String title, String taskId) {
        _mainPanel = new TitledBox(title);
        _mainPanel.removeActionLinks();
        initWidget(_mainPanel);
        popuplateContentPanel();
    }

    private void popuplateContentPanel() {
        VerticalPanel contentPanel = new VerticalPanel();

        _mainPanel.add(contentPanel);
//        _submitButton.setEnabled(false);
    }

}