
package org.janelia.it.jacs.web.gwt.common.client.popup.jobs;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * @author Michael Press
 */
public class JobCompletedPopup extends BasePopupPanel {
    private JobInfo _job;

    public JobCompletedPopup(JobInfo job) {
        super("Job Completed", /*realize now*/ false);
        _job = job;
    }

    protected void populateContent() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(HtmlUtils.getHtml("Your job", "text"));
        panel.add(HtmlUtils.getHtml("&nbsp;&nbsp;\"" + _job.getJobname() + "\"&nbsp;&nbsp;", "prompt"));
        panel.add(HtmlUtils.getHtml("has completed.", "text"));

        add(panel);
        add(HtmlUtils.getHtml("&nbsp;", "text")); //spacer
    }

    protected ButtonSet createButtons() {
        RoundedButton closeButton = new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget widget) {
                hide();
            }
        });

        // Also show the View Results button if the job completed successfully
// NOTE: the 'View Results' button has been disabled because the HitResult table is no longer supported
//        if (_job.jobCompletedSuccessfully()) {
//            RoundedButton viewResultsButton = new RoundedButton("View Results", new ClickListener() {
//                public void onClick(Widget widget) {
//                    Window.open(UrlBuilder.getStatusUrl() + "#JobDetailsPage" + "?jobId=" + _job.getJobId(), "_self", "");
//                }
//            });
//            return new ButtonSet(new RoundedButton[] {viewResultsButton, closeButton});
//        }
//        else
        return new ButtonSet(new RoundedButton[]{closeButton});
    }
}
