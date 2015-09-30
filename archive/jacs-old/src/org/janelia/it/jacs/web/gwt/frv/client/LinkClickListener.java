
package org.janelia.it.jacs.web.gwt.frv.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAtRelativePixelLauncher;
import org.janelia.it.jacs.web.gwt.detail.client.util.SampleAndSiteInfoPopup;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 29, 2007
 * Time: 4:30:42 PM
 */
public class LinkClickListener implements ClickListener {
    private String sampleId;
    private Widget parentWidget;

    public LinkClickListener(String sampleId, Widget parentWidget) {
        this.sampleId = sampleId;
        this.parentWidget = parentWidget;
    }

    public void onClick(Widget widget) {
        // Put the popup left-aligned with the legend panel and just above the parent FRV panel
        new PopupAtRelativePixelLauncher(new SampleAndSiteInfoPopup(sampleId), -38, -7).showPopup(parentWidget);
    }
}
