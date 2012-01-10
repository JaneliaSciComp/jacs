
package org.janelia.it.jacs.web.gwt.detail.client.bse.peporf;

import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 4, 2008
 * Time: 2:00:12 PM
 */
public class PeporfMetadataPanel extends VerticalPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.peporf.PeporfMetadataPanel");
    PeporfPanel parentPanel;

    public PeporfMetadataPanel(PeporfPanel parentPanel) {
        this.parentPanel = parentPanel;
        setVisible(false);
    }

    public void initialize() {
        if (parentPanel.hasSample()) {
            parentPanel.createSampleSiteMapPanel(this);
        }
    }

    public void display() {
        logger.debug("PeporfMetadataPanel display...");
        if (parentPanel.hasSample()) {
            setVisible(true);
        }
    }

}
