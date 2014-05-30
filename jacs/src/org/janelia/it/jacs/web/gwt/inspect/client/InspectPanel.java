
package org.janelia.it.jacs.web.gwt.inspect.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.inspect.client.popup.InspectPopup;

/**
 * @author Todd Safford
 */
public class InspectPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.inspect.client.InspectPanel");

    private RoundedButton _inspectData;
    private TitledBox _mainPanel;
    private JobSubmissionListener _listener;
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public InspectPanel(String title, JobSubmissionListener listener) {
        _listener = listener;
        _mainPanel = new TitledBox(title);
        _mainPanel.removeActionLinks();
        initWidget(_mainPanel);
        popuplateContentPanel();
    }

    private void popuplateContentPanel() {
        VerticalPanel contentPanel = new VerticalPanel();
        _inspectData = new RoundedButton("Run Inspect from archive files", new ClickListener() {
            @Override
            public void onClick(Widget widget) {
                new PopupCenteredLauncher(new InspectPopup(_listener), 250).showPopup(_inspectData);
            }
        });
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "smallSpacer"));
        contentPanel.add(_inspectData);
        _mainPanel.add(contentPanel);
    }

}