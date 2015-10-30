
package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.ResultReceiver;

public class DownloadSampleFileClickListener implements ClickListener {

    private String _contextEntityId;
    private DownloadableDataNode _file;

    public DownloadSampleFileClickListener(String contextEntityId, DownloadableDataNode file) {
        _contextEntityId = contextEntityId;
        _file = file;
    }

    public void onClick(Widget widget) {
        final Widget currentWidget = widget;
        ResultReceiver rcv = new ResultReceiver() {
            public void setResult(Object result) {
                if (result instanceof Boolean) {
                    Boolean fileExists = (Boolean) result;
                    if (fileExists.booleanValue()) { // service handles login or file-not-found failure
                        // track the project sample download
                        SystemWebTracker.trackActivity("DownloadSampleDataFile",
                                new String[]{
                                        _contextEntityId,
                                        _file.getLocation()
                                });
                        // Now, launch popup to retrieve the data file.
                        new PopupAboveLauncher(new DownloadFilePopup(_file, false)).showPopup(currentWidget);
                    }
                }
            }
        };
//        PublicationServiceHelper.checkFileLocation(rcv, _file.getLocation());
    }

}
