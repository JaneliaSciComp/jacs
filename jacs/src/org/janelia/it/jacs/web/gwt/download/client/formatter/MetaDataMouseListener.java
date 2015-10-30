
package org.janelia.it.jacs.web.gwt.download.client.formatter;

import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.TreeListener;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.ResultReceiver;
import org.janelia.it.jacs.web.gwt.download.client.DownloadFilePopup;
//import org.janelia.it.jacs.web.gwt.download.client.PublicationServiceHelper;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 7, 2006
 * Time: 5:23:51 PM
 *
 */

/**
 * Listens for clicks against a tree, then finds the relevant tree item,
 * to present a popup about it.
 */
public class MetaDataMouseListener implements TreeListener {
    public MetaDataMouseListener() {
    }

    public void onTreeItemSelected(TreeItem item) {
        handleDownloadPopupTriggerEvent(item);
    }

    public void onTreeItemStateChanged(TreeItem item) {
    }

    /**
     * Handles an event from some widget, to allow for the presentation of a popup.
     */
    private void handleDownloadPopupTriggerEvent(final TreeItem item) {
        if (item == null || item.getUserObject() == null || !(item.getUserObject() instanceof DownloadableDataNode))
            return;

        final DownloadableDataNode dataFile = (DownloadableDataNode) item.getUserObject();
        ResultReceiver rcv = new ResultReceiver() {
            public void setResult(Object result) {
                if ((Boolean) result) {  // service handles login or file-not-found failure
                    // // Now, launch popup to retrieve the data file.
                    new PopupAboveLauncher(new DownloadFilePopup(dataFile)).showPopup(item);
                }
            }
        };
//        PublicationServiceHelper.checkFileLocation(rcv, dataFile.getLocation());
    }
}

