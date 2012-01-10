
package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A HorizontalPanel that automatically centers all added Widgets.  Defaults to 100% wide .
 *
 * @author Michael Press
 */
public class CenteredWidgetHorizontalPanel extends HorizontalPanel {
    private HorizontalPanel _centeredPanel;

    public CenteredWidgetHorizontalPanel() {
        init();
    }

    public CenteredWidgetHorizontalPanel(Widget centeredWidget) {
        init();
        add(centeredWidget);
    }

    private void init() {
        setWidth("100%");

        HTML leftSpacer = new HTML("&nbsp;");
        super.add(leftSpacer);
        setCellWidth(leftSpacer, "50%");

        _centeredPanel = new HorizontalPanel();
        super.add(_centeredPanel);

        HTML rightSpacer = new HTML("&nbsp;");
        super.add(rightSpacer);
        setCellWidth(rightSpacer, "50%");
    }

    public void add(Widget widget) {
        _centeredPanel.add(widget);
    }

    public boolean remove(Widget widget) {
        return _centeredPanel.remove(widget);
    }
}
