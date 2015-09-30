
package org.janelia.it.jacs.web.gwt.search.client.panel.project;

import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectInfoPanel;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 9, 2007
 * Time: 4:42:32 PM
 */
public class ProjectDetailPanel extends TitledBox {
    public ProjectDetailPanel(String title, boolean showActionLinks, final String projectSymbol, ActionLink actionLink) {
        super(title, false /*showActionLinks*/, true /*show content*/);
        buildPanel(projectSymbol, actionLink, showActionLinks);
    }

    private void buildPanel(String projectSymbol, ActionLink actionLink, boolean showActionLinks) {
        if (showActionLinks) {
            setShowActionLinks(true);
            showActionLinks();
            if (actionLink != null)
                addActionLink(actionLink);
        }

        add(new ProjectInfoPanel(projectSymbol, TitledBoxFactory.BoxType.SECONDARY_BOX));
    }

    protected void populateContentPanel() {
        // nothing until populateContentPanel2
    }
}