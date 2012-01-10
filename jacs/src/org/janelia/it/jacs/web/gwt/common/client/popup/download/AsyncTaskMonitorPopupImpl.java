
package org.janelia.it.jacs.web.gwt.common.client.popup.download;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncTaskMonitorPopup;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.CancelListener;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

/**
 * @author Michael Press
 */
public class AsyncTaskMonitorPopupImpl extends ModalPopupPanel implements AsyncTaskMonitorPopup {
    private static Logger _logger = Logger.getLogger("AsyncExportPopup");

    private HorizontalPanel _mainPanel;
    private RoundedButton _closeButton;
    private CancelListener _cancelListener;
    private String _taskType = "task"; // default

    public AsyncTaskMonitorPopupImpl(String title, String taskType, CancelListener cancelListener) {
        super(title, /*realize now*/ false, /*fade background*/ true);
        setTaskType(taskType);
        setCancelListener(cancelListener);
        realize();
    }

    protected void populateContent() {
        _mainPanel = new HorizontalPanel();
        setProcessingMessage("Your " + getTaskType() + " is being generated.<br><br>You may wait for the download to begin or download files later<br>from the Job Results page.<br><br>"); // default message
        HorizontalPanel _buttonPanel = new CenteredWidgetHorizontalPanel();
        _closeButton = new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget sender) {
                onCancel();
            }
        });

        RoundedButton _jobStatusButton = new RoundedButton("Go To Job Results", new ClickListener() {
            public void onClick(Widget sender) {
                close();
                String url = UrlBuilder.getStatusUrl();
                Window.open(url, "_self", "");
            }
        });

        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(_mainPanel);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _buttonPanel.add(_closeButton);
        _buttonPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _buttonPanel.add(_jobStatusButton);
        add(_buttonPanel);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
    }

    public void setProcessingMessage(String message) {
        _mainPanel.clear();
        _mainPanel.add(ImageBundleFactory.getAnimatedImageBundle().getBusyAnimatedIcon().createImage());
        _mainPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;", "text"));
        _mainPanel.add(HtmlUtils.getHtml(message, "text"));
    }

    private void onCancel() {
        if (_cancelListener != null)
            _cancelListener.onCancel();
        close();
    }

    public void close() {
        hide();
    }

    public void setFailureMessage(String message) {
        setFailureMessage(message, message);
    }

    public void setFailureMessage(String message, String logMessage) {
        _logger.debug(getTaskType() + " submission failed due to : " + logMessage);
        _mainPanel.clear();
        _mainPanel.add(HtmlUtils.getHtml(message, "error"));
        _closeButton.setText("Close");
    }

    public void setCancelListener(CancelListener cancelListener) {
        _cancelListener = cancelListener;
    }

    public void setTaskType(String taskType) {
        _taskType = taskType;
    }

    public String getTaskType() {
        return _taskType;
    }
}
