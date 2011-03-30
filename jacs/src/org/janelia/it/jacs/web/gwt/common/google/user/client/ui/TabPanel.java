/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */
package org.janelia.it.jacs.web.gwt.common.google.user.client.ui;

import com.google.gwt.user.client.ui.*;

import java.util.Iterator;

/**
 * (mpress - this is the stock GWT 1.1.10 TabPanel, except it has been modified to allow subclasses to
 * override creation of the TabBar and Deck widgets.)
 * <p/>
 * A panel that represents a tabbed set of pages, each of which contains another
 * widget. Its child widgets are shown as the user selects the various tabs
 * associated with them. The tabs can contain arbitrary HTML.
 * <p/>
 * <p>
 * <img class='gallery' src='TabPanel.png'/>
 * </p>
 * <p/>
 * <p>
 * Note that this widget is not a panel per se, but rather a
 * {@link com.google.gwt.user.client.ui.Composite} that aggregates a
 * {@link com.google.gwt.user.client.ui.TabBar} and a
 * {@link com.google.gwt.user.client.ui.DeckPanel}. It does, however, implement
 * {@link com.google.gwt.user.client.ui.HasWidgets}.
 * </p>
 * <p/>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-TabPanel { the tab panel itself }</li>
 * <li>.gwt-TabPanelBottom { the bottom section of the tab panel (the deck
 * containing the widget) }</li>
 * </ul>
 */
public class TabPanel extends Composite implements TabListener,
        SourcesTabEvents, HasWidgets, IndexedPanel {

    private WidgetCollection children = new WidgetCollection(this);
    private DeckPanel deck;
    private TabBar tabBar;
    private TabListenerCollection tabListeners;

    /**
     * Creates an empty tab panel.
     */
    public TabPanel() {
        tabBar = createTabBar();    // MAP changed
        deck = createDeckPanel(); // MAP changed

        VerticalPanel panel = new VerticalPanel();
        panel.add(tabBar);
        panel.add(deck);

        panel.setCellHeight(deck, "100%");
        tabBar.setWidth("100%");

        tabBar.addTabListener(this);
        initWidget(panel);
        setStyleName("gwt-TabPanel");
        deck.setStyleName("gwt-TabPanelBottom");
    }

    /**
     * MAP added
     */
    protected TabBar createTabBar() {
        return new TabBar();
    }

    /**
     * MAP added
     */
    protected DeckPanel createDeckPanel() {
        return new DeckPanel();
    }

    public void add(Widget w) {
        throw new UnsupportedOperationException(
                "A tabText parameter must be specified with add().");
    }

    /**
     * Adds a widget to the tab panel.
     *
     * @param w       the widget to be added
     * @param tabText the text to be shown on its tab
     */
    public void add(Widget w, String tabText) {
        insert(w, tabText, getWidgetCount());
    }

    /**
     * Adds a widget to the tab panel.
     *
     * @param w       the widget to be added
     * @param tabText the text to be shown on its tab
     * @param asHTML  <code>true</code> to treat the specified text as HTML
     */
    public void add(Widget w, String tabText, boolean asHTML) {
        insert(w, tabText, asHTML, getWidgetCount());
    }

    public void addTabListener(TabListener listener) {
        if (tabListeners == null)
            tabListeners = new TabListenerCollection();
        tabListeners.add(listener);
    }

    public void clear() {
        while (getWidgetCount() > 0)
            remove(getWidget(0));
    }

    /**
     * Gets the deck panel within this tab panel.
     *
     * @return the deck panel
     */
    public DeckPanel getDeckPanel() {
        return deck;
    }

    /**
     * Gets the tab bar within this tab panel
     *
     * @return the tab bar
     */
    public TabBar getTabBar() {
        return tabBar;
    }

    public Widget getWidget(int index) {
        return children.get(index);
    }

    public int getWidgetCount() {
        return children.size();
    }

    public int getWidgetIndex(Widget widget) {
        return children.indexOf(widget);
    }

    /**
     * Inserts a widget into the tab panel.
     *
     * @param widget      the widget to be inserted
     * @param tabText     the text to be shown on its tab
     * @param asHTML      <code>true</code> to treat the specified text as HTML
     * @param beforeIndex the index before which it will be inserted
     */
    public void insert(Widget widget, String tabText, boolean asHTML,
                       int beforeIndex) {
        children.insert(widget, beforeIndex);
        tabBar.insertTab(tabText, asHTML, beforeIndex);
        deck.insert(widget, beforeIndex);
    }

    /**
     * Inserts a widget into the tab panel.
     *
     * @param widget      the widget to be inserted
     * @param tabText     the text to be shown on its tab
     * @param beforeIndex the index before which it will be inserted
     */
    public void insert(Widget widget, String tabText, int beforeIndex) {
        insert(widget, tabText, false, beforeIndex);
    }

    public Iterator iterator() {
        return children.iterator();
    }

    public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
        return tabListeners == null || tabListeners.fireBeforeTabSelected(this, tabIndex);
    }

    public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
        deck.showWidget(tabIndex);
        if (tabListeners != null)
            tabListeners.fireTabSelected(this, tabIndex);
    }

    public boolean remove(int index) {
        return remove(getWidget(index));
    }

    /**
     * Removes the given widget, and its associated tab.
     *
     * @param widget the widget to be removed
     */
    public boolean remove(Widget widget) {
        int index = getWidgetIndex(widget);
        if (index == -1)
            return false;

        children.remove(widget);
        tabBar.removeTab(index);
        deck.remove(widget);
        return true;
    }

    public void removeTabListener(TabListener listener) {
        if (tabListeners != null)
            tabListeners.remove(listener);
    }

    /**
     * Programmatically selects the specified tab.
     *
     * @param index the index of the tab to be selected
     */
    public void selectTab(int index) {
        tabBar.selectTab(index);
    }
}
