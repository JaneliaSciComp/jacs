package org.janelia.it.jacs.web.gwt.common.google.user.client.ui;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;

/**
 * A horizontal bar of folder-style tabs, most commonly used as part of a
 * {@link com.google.gwt.user.client.ui.TabPanel}.
 * <p/>
 * <p>
 * <img class='gallery' src='TabBar.png'/>
 * </p>
 * <p/>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-VerticalTabBar { the tab bar itself }</li>
 * <li>.gwt-VerticalTabBar .gwt-VerticalTabBarFirst { the left edge of the bar }</li>
 * <li>.gwt-VerticalTabBar .gwt-VerticalTabBarRest { the right edge of the bar }</li>
 * <li>.gwt-VerticalTabBar .gwt-VerticalTabBarItem { unselected tabs }</li>
 * <li>.gwt-VerticalTabBar .gwt-VerticalTabBarItem-selected { additional style for selected tabs } </li>
 * </ul>
 */
public class VerticalTabBar extends Composite implements SourcesTabEvents,
        ClickListener {

    private VerticalPanel panel = new VerticalPanel();    //MAP changed
    private Widget selectedTab;
    private TabListenerCollection tabListeners;

    /**
     * Creates an empty tab bar.
     */
    public VerticalTabBar() {
        initWidget(panel);
        sinkEvents(Event.ONCLICK);
        setStyleName("gwt-VerticalTabBar"); //MAP changed

        panel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);

        HTML first = new HTML("&nbsp;", true), rest = new HTML("&nbsp;", true);
        first.setStyleName("gwt-VerticalTabBarFirst");
        rest.setStyleName("gwt-VerticalTabBarRest");
        first.setHeight("100%");
        rest.setHeight("100%");

        panel.add(first);
        panel.add(rest);
        first.setHeight("100%");
        panel.setCellHeight(first, "100%");
        panel.setCellWidth(rest, "100%");
    }

    /**
     * Adds a new tab with the specified text.
     *
     * @param text the new tab's text
     */
    public void addTab(String text) {
        insertTab(text, getTabCount());
    }

    /**
     * Adds a new tab with the specified text.
     *
     * @param text   the new tab's text
     * @param asHTML <code>true</code> to treat the specified text as html
     */
    public void addTab(String text, boolean asHTML) {
        insertTab(text, asHTML, getTabCount());
    }

    public void addTabListener(TabListener listener) {
        if (tabListeners == null)
            tabListeners = new TabListenerCollection();
        tabListeners.add(listener);
    }

    /**
     * Gets the tab that is currently selected.
     *
     * @return the selected tab
     */
    public int getSelectedTab() {
        if (selectedTab == null)
            return -1;
        return panel.getWidgetIndex(selectedTab) - 1;
    }

    /**
     * Gets the number of tabs present.
     *
     * @return the tab count
     */
    public int getTabCount() {
        return panel.getWidgetCount() - 2;
    }

    /**
     * Gets the specified tab's HTML.
     *
     * @param index the index of the tab whose HTML is to be retrieved
     * @return the tab's HTML
     */
    public String getTabHTML(int index) {
        if (index >= getTabCount())
            return null;
        return ((HTML) panel.getWidget(index - 1)).getHTML();
    }

    /**
     * Returns the vertical panel that holds all the tabs.
     *
     * @return panel with tabs.
     */
    public VerticalPanel getPanelWithTabs() {
        return panel;
    }

    /**
     * Inserts a new tab at the specified index.
     *
     * @param text        the new tab's text
     * @param asHTML      <code>true</code> to treat the specified text as HTML
     * @param beforeIndex the index before which this tab will be inserted
     */
    public void insertTab(String text, boolean asHTML, int beforeIndex) {
        if ((beforeIndex < 0) || (beforeIndex > getTabCount()))
            throw new IndexOutOfBoundsException();

        Label item;
        if (asHTML)
            item = new HTML(text);
        else
            item = new Label(text);

        item.setWordWrap(false);
        item.addClickListener(this);
        item.setStyleName("gwt-VerticalTabBarItem");
        panel.insert(item, beforeIndex + 1);
    }

    /**
     * Inserts a new tab at the specified index.
     *
     * @param text        the new tab's text
     * @param beforeIndex the index before which this tab will be inserted
     */
    public void insertTab(String text, int beforeIndex) {
        insertTab(text, false, beforeIndex);
    }

    public void onClick(Widget sender) {
        for (int i = 1; i < panel.getWidgetCount() - 1; ++i) {
            if (panel.getWidget(i) == sender) {
                selectTab(i - 1);
                return;
            }
        }
    }

    /**
     * Removes the tab at the specified index.
     *
     * @param index the index of the tab to be removed
     */
    public void removeTab(int index) {
        checkTabIndex(index);

        // (index + 1) to account for 'first' placeholder widget.
        Widget toRemove = panel.getWidget(index + 1);
        if (toRemove == selectedTab)
            selectedTab = null;
        panel.remove(toRemove);
    }

    public void removeTabListener(TabListener listener) {
        if (tabListeners != null)
            tabListeners.remove(listener);
    }

    /**
     * Programmatically selects the specified tab.
     *
     * @param index the index of the tab to be selected
     * @return <code>true</code> if successful, <code>false</code> if the
     *         change is denied by the {@link TabListener}.
     */
    public boolean selectTab(int index) {
        checkTabIndex(index);

        if (tabListeners != null) {
            if (!tabListeners.fireBeforeTabSelected(this, index))
                return false;
        }

        setSelectionStyle(selectedTab, false);
        selectedTab = panel.getWidget(index + 1);
        setSelectionStyle(selectedTab, true);

        if (tabListeners != null)
            tabListeners.fireTabSelected(this, index);
        return true;
    }

    private void checkTabIndex(int index) {
        if ((index < 0) || (index >= getTabCount()))
            throw new IndexOutOfBoundsException();
    }

    private void setSelectionStyle(Widget item, boolean selected) {
        if (item != null) {
            if (selected)
                item.addStyleName("gwt-VerticalTabBarItem-selected");
            else
                item.removeStyleName("gwt-VerticalTabBarItem-selected");
        }
    }
}
