
package org.janelia.it.jacs.web.gwt.common.client.panel.user.node;

import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 13, 2010
 * Time: 11:45:03 AM
 */
public abstract class UserNodeManagementNewNodePopup extends BasePopupPanel {

    public UserNodeManagementNewNodePopup(String title) {
        super(title, /*realize now*/false, /*autohide*/false, /*modal*/ false);
    }

    @Override
    protected abstract void populateContent();
    
}
