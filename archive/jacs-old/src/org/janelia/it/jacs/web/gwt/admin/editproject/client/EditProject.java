
package org.janelia.it.jacs.web.gwt.admin.editproject.client;

import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;

/**
 * Created by IntelliJ IDEA.
 * User: gcao
 * Date: Jun 6, 2007
 * Time: 10:20:03 AM
 */
public class EditProject extends BaseEntryPoint {

    /**
     * Here is where the module associated with download, is 'activated'.
     */
    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb[]{
                new Breadcrumb((Constants.ADMIN_SECTION_LABEL)),
                new Breadcrumb(Constants.EDIT_PROJECT_SECTION_LABEL)
        },
                Constants.ROOT_PANEL_NAME);

        VerticalPanel rows = new VerticalPanel();
        rows.setWidth("100%");

        // Edit Project panel
        EditProjectPanel editProjectPanel = new EditProjectPanel();
        editProjectPanel.setWidth("100%");
        rows.add(editProjectPanel);

        RootPanel.get(Constants.ROOT_PANEL_NAME).add(rows);
        show();

    }


}
