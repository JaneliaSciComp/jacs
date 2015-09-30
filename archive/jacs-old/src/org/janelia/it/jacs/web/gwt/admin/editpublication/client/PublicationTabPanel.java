
package org.janelia.it.jacs.web.gwt.admin.editpublication.client;

import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;


/**
 * @author Guy Cao
 */
public class PublicationTabPanel extends RoundedTabPanel {

    private PublicationDetailsPanel publicationdDetailPanel = null;
    private PublicationDescriptionPanel publicationDescriptionPanel = null;
    private PublicationPreviewPanel publicationPreviewPanel = null;

    public PublicationTabPanel() {

        // details
        publicationdDetailPanel = new PublicationDetailsPanel(this);

        publicationDescriptionPanel = new PublicationDescriptionPanel(this);

        publicationPreviewPanel = new PublicationPreviewPanel(this);

        selectTab(0);

        setWidth("100%");

    }

    public PublicationDetailsPanel getDetailPanel() {
        return publicationdDetailPanel;
    }

    public PublicationDescriptionPanel getDescriptionPanel() {
        return publicationDescriptionPanel;
    }

    public PublicationPreviewPanel getPreviewPanel() {
        return publicationPreviewPanel;
    }


}
