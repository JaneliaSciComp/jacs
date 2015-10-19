
package org.janelia.it.jacs.web.gwt.admin.editpublication.client;

import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;

/**
 * Created by IntelliJ IDEA.
 * User: gcao
 * Date: Jul 2, 2007
 * Time: 9:41:17 AM
 */
public class EditPublication extends BaseEntryPoint {


    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.EDIT_PUBLICATION_SECTION_LABEL), Constants.ROOT_PANEL_NAME);

        VerticalPanel rows = new VerticalPanel();
        rows.setWidth("100%");

        // Edit Project panel
        EditPublicationPanel editPublicationPanel = new EditPublicationPanel();
        editPublicationPanel.setWidth("100%");
        rows.add(editPublicationPanel);

        RootPanel.get(Constants.ROOT_PANEL_NAME).add(rows);
        show();

    }


}
