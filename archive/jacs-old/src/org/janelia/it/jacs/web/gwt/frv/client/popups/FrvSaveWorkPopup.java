
package org.janelia.it.jacs.web.gwt.frv.client.popups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 4, 2007
 * Time: 4:39:10 PM
 */
public class FrvSaveWorkPopup extends BasePopupPanel {

    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.popups.FrvSaveWorkPopup");

    private RecruitableJobInfo job = new RecruitableJobInfo();
    private Widget parent;
    private TextBox nameBox = new TextBox();

    private VerticalPanel panel = new VerticalPanel();

    private static DataServiceAsync _dataService = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataService).setServiceEntryPoint("data.srv");
    }

    public FrvSaveWorkPopup(RecruitableJobInfo job, Widget parent) {
        super("Name Required", /*realizeNow*/ false, /*autohide*/ true, /*modal*/ true);
        this.job = job;
        this.parent = parent;
    }

    protected void populateContent() {
        this.add(panel);
//        panel.setSize("400px", "100px");
        panel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        panel.add(new Label("Please provide a name for your saved work:"));
        nameBox.setVisibleLength(40);
        panel.add(nameBox);
    }

    protected ButtonSet createButtons() {
        RoundedButton saveButton = new RoundedButton("Save", new ClickListener() {
            public void onClick(Widget widget) {
                // Make a call to remove the expiration date; thereby, saving the task for the user
                _dataService.setTaskExpirationAndName(job.getJobId(), null, nameBox.getText(), new AsyncCallback() {
                    public void onFailure(Throwable caught) {
                        _logger.error("Could not prevent the task from expiring\n" + caught.getMessage());
                        ErrorPopupPanel popup = new ErrorPopupPanel("Unable to save your work at this time.");
                        new PopupCenteredLauncher(popup, 250).showPopup(parent);
                    }

                    public void onSuccess(Object result) {
                        // todo Maybe toggle Save My Work button after save, and after changes
                        InfoPopupPanel popup = new InfoPopupPanel("Your work has been saved.");
                        new PopupAboveLauncher(popup, 250).showPopup(parent);
                    }
                });
                hidePopup();
            }
        });
        RoundedButton cancelButton = new RoundedButton("Cancel", new ClickListener() {
            public void onClick(Widget widget) {
                hidePopup();
            }
        });
        return new ButtonSet(new RoundedButton[]{saveButton, cancelButton});
    }

    /**
     * Hook for inner classes to hide the popup
     */
    protected void hidePopup() {
        this.hide();
    }

}