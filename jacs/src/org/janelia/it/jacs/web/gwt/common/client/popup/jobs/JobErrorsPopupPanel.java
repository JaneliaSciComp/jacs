
package org.janelia.it.jacs.web.gwt.common.client.popup.jobs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.HTML;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: May 23, 2007
 * Time: 3:31:05 PM
 */
public class JobErrorsPopupPanel extends BasePopupPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.popup.jobs.JobErrorsPopupPanel");

    private JobInfo jobInfo;
    private static DataServiceAsync dataservice = (DataServiceAsync) GWT.create(DataService.class);
    private LoadingLabel loadingLabel;

    static {
        ((ServiceDefTarget) dataservice).setServiceEntryPoint("data.srv");
    }

    public JobErrorsPopupPanel(JobInfo jobInfo, String title) {
        super(title, false, true);
        this.jobInfo = jobInfo;
        this.loadingLabel = new LoadingLabel("Loading messages...", true);
        add(this.loadingLabel);
    }

    protected void populateContent() {
        dataservice.getTaskMessages(jobInfo.getJobId(), new AsyncCallback() {
            public void onFailure(Throwable caught) {
                logger.error("JobErrorsPopupPanel dataservice.getTaskWithMessages().onFailure(): ", caught);
                add(HtmlUtils.getHtml("Error retrieving error messages", "error"));
            }

            // On success, populate the table with the DataNodes received
            public void onSuccess(Object result) {
                try {
                    loadingLabel.setVisible(false);
                    Set<String> messages = (Set<String>) result;

                    if (null != messages) {
                        if (logger.isDebugEnabled())
                            logger.debug("JobErrorsPopupPanel dataservice.getTaskWithMessages() onSuccess() got messages: " + messages.toString());

                        for (String message : messages) {
                            add(new HTML(message, true));
                            add(new HTML("<br/>", true));
                        }
                    }
                }
                catch (Exception e) {
                    logger.error("JobErrorsPopupPanel.realize() dataservice.getTaskWithMessages onSuccess() caught exception:" + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        });
    }

}
