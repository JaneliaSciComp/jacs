
package org.janelia.it.jacs.web.gwt.admin.editproject.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;


public class RequiredPanel extends Composite {


    public RequiredPanel() {


        HTMLPanel innerPanel = new HTMLPanel("<span class='requiredInformation'>*</span>" +
                "<span class='commaList'>&nbsp;required</span>");
        innerPanel.setStyleName("requiredInnerPanel");

        RoundedPanel2 required = new RoundedPanel2(innerPanel, RoundedPanel2.ALL, "#AAAAAA");
        required.setStyleName("requiredOutterPanel");


        initWidget(required);

    }


}
