
package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.ClickableGrid;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.VerticalTabBar;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 12, 2006
 * Time: 10:37:26 AM
 * <p/>
 * Special vertical tab bar that can have a widget for a tab, and can have an adornment added below
 * all tabs.
 */
public class WidgetVerticalTabBar extends VerticalTabBar {
    public WidgetVerticalTabBar() {
        super();
    }

    /**
     * Adds an "adornment" (in the JFileChooser sense), to the tabs.  Adds a
     * sort of 'non-tab' to be aligned vertically below the tabs, but not
     * to have any tab-like interactions.  Les Foster
     * <p/>
     * For best results, add this AFTER all tabs.
     *
     * @param w what to put below
     */
    public void addAdornment(Widget w) {
        getPanelWithTabs().add(w);
    }

    /**
     * Adds a tab which is a widget, rather than creating a specific widget to
     * surround text.  Because clicks are to be handled, must be a FocusWidget.
     * NOTE: any developer who wishes to add widgets at specific positions, feel
     * free to make that change (add another overload) or inform LLF.
     * NOTE: there is a remove-by-index already out there.  Therefore no need to
     * add a remove method as a complement to this method.
     *
     * @param widget what to add.
     */
    public void addTab(ClickableGrid widget) {
        widget.addClickListener(this);
        widget.setStyleName("gwt-VerticalTabBarItem");
        getPanelWithTabs().insert(widget, getTabCount() + 1);

//        getPanelWithTabs().insert(createRoundedPanel(widget), getTabCount() + 1);

//        widget.setStyleName("gwt-VerticalTabBarItem");
//        DockPanel dockPanel = new DockPanel();
//        dockPanel.add(widget, DockPanel.CENTER);
//        dockPanel.add(createRoundedPanel(), DockPanel.WEST);
//        getPanelWithTabs().insert(dockPanel, getTabCount() + 1);
    }

    /**
     * Pass back tab widget at index given.
     *
     * @param tabNumber which tab's widget to pass back?
     * @return that widget.
     */
    public ClickableGrid getTab(int tabNumber) {
        if (tabNumber < 0 || tabNumber > getTabCount())
            return null;
        return (ClickableGrid) getPanelWithTabs().getWidget(tabNumber + 1);
    }

    public RoundedPanel2 createRoundedPanel(Widget w) {
        RoundedPanel2 returnPanel = new RoundedPanel2(w, RoundedPanel2.LEFT, "#BBBBBB");
        returnPanel.setStyleName("gwt-VerticalTabBarItem");
        return returnPanel;
    }
}
