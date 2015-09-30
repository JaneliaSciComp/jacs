
package org.janelia.it.jacs.web.gwt.download.client.samples.wizard;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.BackActionLink;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.detail.client.bse.metadata.SampleDetailPanel;

/**
 * @author Michael Press
 */
public class SampleDetailsWizardPage extends SamplesWizardPage {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.status.client.wizard.DetailsWizardPage");

    public static final String HISTORY_TOKEN = "DetailPage";
    private SimplePanel _mainPanel;

    public SampleDetailsWizardPage(SampleInfo data, WizardController controller) {
        super(data, controller);
        init();
    }

    private void init() {
        _mainPanel = new SimplePanel();
    }

    public String getPageToken() // used for history
    {
        return HISTORY_TOKEN;
    }

    public Widget getMainPanel() {
        return _mainPanel;
    }

    public String getPageTitle() {
        return Constants.JOBS_SEQUENCE_DETAILS_LABEL;
    }

    protected void preProcess(Integer priorPageNumber) {
        _logger.debug("DetailsWizardPage.preProcess()");

        // Add a "back" link to the main panel since it's removed after the reset()
        int currentPage = getController().getCurrentPageIndex();
        String backPageToken = getController().getPageTokenAt(currentPage - 1);
        BackActionLink backLink = new BackActionLink("back", new ClickListener() {
            public void onClick(Widget widget) {
                getController().back();
            }
        });
        backLink.setTargetHistoryToken(backPageToken);

        //TODO: remove hide ActionLink
        _mainPanel.clear();
        _mainPanel.add(new SampleDetailPanel("Sample Details", getData().getCurrentSample().getSampleAcc(), backLink, true));
    }
}