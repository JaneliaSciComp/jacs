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

package org.janelia.it.jacs.web.gwt.common.client.wizard;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootPanel;
import org.gwtwidgets.client.util.WindowUtils;
import org.gwtwidgets.client.wrap.Callback;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;

import java.util.ArrayList;

/**
 * @author Michael Press
 */
public abstract class WizardController extends BaseEntryPoint implements HistoryListener {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController");

    ArrayList<WizardPage> _pages = new ArrayList<WizardPage>();
    int _currentPage;
    HTMLPanel _pageTitleHTML;
    HTMLPanel _mainPanelHTML;
    private int _numPages;
    private WizardButtonManager _buttonController = new WizardButtonManager(this);
    private WizardURLManager _urlManager = new WizardURLManager(this);

    public abstract void onModuleLoad();

    public abstract Breadcrumb getBreadcrumbSection();

    protected WizardController() {
        super();
        History.addHistoryListener(this);
    }

    /**
     * Pouplates the input container with the current wizard page.  Assumes the page had already been cleared
     */
    protected void render() {
        // Step 2 - after the clear(fadeout) is complete, populate and show the new page
        Callback populateCallback = new Callback() {
            public void execute() {
                // Set the title
                WizardPage currentPage = getCurrentPage();
                setBreadcrumb(new Breadcrumb[]{getBreadcrumbSection(), new Breadcrumb(currentPage.getPageTitle())},
                        Constants.ROOT_PANEL_NAME);

                // Add the main panel (created by the WizardPage)
                _mainPanelHTML = new HTMLPanel("<span id='mainPanel'></span>");
                _mainPanelHTML.add(currentPage.getMainPanel(), "mainPanel");
                RootPanel.get(Constants.ROOT_PANEL_NAME).add(_mainPanelHTML);
                // notify the current page that the main panel has been added
                currentPage.mainPanelCreated();
                // Add the buttons (created by the WizardPage)
                if (currentPage.showButtons()) {
                    ButtonSet buttonSet = getButtonManager().getButtonSet();
                    HTMLPanel buttonSetHTML = new HTMLPanel("<span id='buttonSet'></span>");
                    buttonSetHTML.add(buttonSet, "buttonSet");
                    RootPanel.get(Constants.ROOT_PANEL_NAME).add(new HTML("&nbsp;")); // spacer
                    RootPanel.get(Constants.ROOT_PANEL_NAME).add(buttonSetHTML);

                    // Notify the page so it can configure the buttons based on current data state
                    currentPage.setupButtons();
                }

                // Fade in the new page
                show();
            }
        };

        // Step 1 - clear the current page
        clear(populateCallback);
    }

    private WizardPage getCurrentPage() {
        return _pages.get(_currentPage);
    }

    public int getCurrentPageIndex() {
        return _currentPage;
    }

    public String getPageTokenAt(int pageIndex) {
        String pageToken = null;
        if (isValidPage(pageIndex)) {
            WizardPage page = _pages.get(pageIndex);
            if (page != null) {
                pageToken = page.getPageToken();
            }
        }
        return pageToken;
    }

    protected void addPage(WizardPage page) {
        _pages.add(page);
        _numPages++;
    }

    public void back() {
        if (isValidPage(_currentPage - 1)) {
            gotoPage(_currentPage - 1);
        }
    }

    // assuming that a wizard "next" always goes to page + 1
    public void next() {
        if (isValidPage(_currentPage + 1)) {
            gotoPage(_currentPage + 1);
            // track the transition to the next page
            SystemWebTracker.trackActivity(getCurrentPage().getPageToken());
        }
    }

    public void refresh() {
        // if something changed in the current page that also reflects in the page's token
        // we simply want to refresh this page instead of going to the next
        gotoPage(_currentPage);
    }

    public boolean isValidPage(int pageNum) {
        return (pageNum >= 0 && pageNum < _numPages);
    }

    //public void gotoPageByNamedTransition(String transitionName)
    //{
    //    PageTransition transition = (PageTransition) _pageTransitions.get(transitionName);
    //    if (transition == null)
    //        _logger.error("No matching page transition \"" + transitionName + "\"");
    //    else {
    //        int pageNum = findPageByName(transition.getDestPageToken());
    //        if (pageNum < 0)
    //            _logger.error("No matching destination page \"" + transition.getDestPageToken() + "\"");
    //        else {
    //            // Set the back destination to the current page
    //            getButtonManager().setBackDestinationPage(_currentPage);
    //            gotoPage(pageNum);
    //        }
    //    }
    //}

    public void gotoPageByName(String futurePageName) {
        gotoPage(findPageByName(futurePageName));
    }

    public void gotoPage(int pageNumber) {
        gotoPage(pageNumber, true);
    }

    /**
     * The core mechanism to transition pages.  Handles executing oldPage.postProcess() and newPage.preProcess().
     *
     * @param newPageNumber page going to
     * @param addToHistory  add to history or not
     */
    public void gotoPage(int newPageNumber, boolean addToHistory) {
        if (!isValidPage(newPageNumber)) {
            _logger.debug("gotoPage - can't go to page " + newPageNumber);
            return;
        }

        // Let the current page deny progression (if we're going "up" in pages)
        if (newPageNumber > _currentPage && !getCurrentPage().isProgressionValid()) {
            _logger.debug("gotoPage - current page " + newPageNumber + " invalidated progression.  Remaining on page " + _currentPage);
            return;
        }

        // clear any bookmark
        clearBookmark(Constants.ROOT_PANEL_NAME);

        // PostProcess() the old page
        getCurrentPage().postProcess(newPageNumber);

        // Change to new page, preProcess() it, add an entry to the browser history, and finally render the new page
        int lastPage = _currentPage;
        _currentPage = newPageNumber;
        if (addToHistory)
            setHistoryEvent(getCurrentPage().getPageToken());
        getCurrentPage().preProcess(lastPage);
        render();
    }

    /**
     * Invoked by entrypoint when the Wizard has been configured. Similar to gotoPage() but starts at the page identified
     * in the URL (or page 0 by default). Using start() instead of gotoPage(0) avoids odd behavior like postProcess()
     * of page 0 before preProcess(), and doesn't add a browser history entry since the Entry Point has already been added.
     */
    public void start() {
        // Let the entrypoint handle any URL params - WizardUrlManager will invoke processParam() for each param.
        _urlManager.processURLParams();

        // Go to the first page as determined by the wizard implementation (possibly specified on the URL)
        _currentPage = getStartingPage(WindowUtils.getLocation().getHref());
        if (_currentPage == 0) {
            setHistoryEvent(getPageTokenAt(0));  // Set hitory token for the first page
            setFavicon(); // Reset the favicon after navigation to the new page (this seems like a GWT bug in gwt-user.jar code)
        }

        //setHistoryEvent(getCurrentPage().getPageToken());
        getCurrentPage().preProcess(null);
        render();

        // track the first transition
        SystemWebTracker.trackActivity(getCurrentPage().getPageToken());
    }

    /**
     * Determine the starting page for the wizard by examining the URL for an anchor tag.  Subclasses can override this behavior
     */
    protected int getStartingPage(String startURL) {
        return _urlManager.getStartPageFromURL(startURL);
    }

    private void setFavicon() {
        Element oldIcon = DOM.getElementById("favicon");
        Element newIcon = DOM.createElement("link");
        Element parent = DOM.getParent(oldIcon);

        DOM.removeChild(parent, oldIcon);
        DOM.setElementAttribute(newIcon, "id", "favicon");
        DOM.setElementAttribute(newIcon, "rel", "shortcut icon");
        DOM.setElementAttribute(newIcon, "href", "/jacs/favicon.ico");
        DOM.appendChild(parent, newIcon);
    }

    /**
     * Adds a history event to the browser to support users using the back and next browser buttons to navigate
     * within the wizard pages
     *
     * @param pageToken page token to use
     */
    protected void setHistoryEvent(String pageToken) {
        _logger.debug("Setting history event - page " + pageToken);
        History.newItem(pageToken);
    }

    /**
     * Implementation of com.google.gwt.user.client.HistoryListener - notifies us of a user-initiated history event on
     * the browser (back or forward). The initial loading event (when the Entry Point is first loaded) does not add a
     * history token (token is ""), but any other token represents a transition to a Wizard page via the browser
     * back/forward, so we'll go the correct wizard page as specified by the token (which each page defines).
     * <p/>
     * Note that user-inititated actions on the page (clicking on a tab, clicking a button) also seem to generate
     * notifications of history changed, so we'll ignore any actions that don't result in a transition to a different
     * page.
     */
    public void onHistoryChanged(String historyToken) {
        // Detect a browser-induced transition to the starting page of the wizard.  We'll go to that page, but 
        // not add it to the history because.... why?
        if (historyToken.equals("")) {
            _logger.debug("onHistoryChanged(): got empty token, therefore taking no action");
            //gotoPage(0, false);      DO NOTHING
            return;
        }

        // Transition to the page specified by historyToken (but ignore if it's the current page)
        int destPage = findPageByName(historyToken);
        if (destPage >= 0) {
            if (destPage != _currentPage) {
                _logger.debug("onHistoryChanged(): caught page transition from browser;  new page is " + destPage + " (" + historyToken + ")");
                gotoPage(destPage);
            }
            else {
                // it's the same page but we want to compare the tokens
                if (!getPageTokenAt(destPage).equals(historyToken)) {
                    // if they differ process the URL parameters then refresh the page
                    _urlManager.processURLParams();
                    gotoPage(destPage, true);
                } // otherwise ignore it
            }
        }
        else {
            _logger.debug("onHistoryChanged(): ignoring token " + historyToken);
        }
    }

    /**
     * Find a page's index given its name (for history support)
     *
     * @param pageName page name to find index for
     * @return page number for given name
     */
    public int findPageByName(String pageName) {
        for (int i = 0; i < _pages.size(); ++i) {
            WizardPage page = _pages.get(i);
            if (page.checkPageToken(pageName))
                return i;
        }

        return -1;
    }

    public WizardButtonManager getButtonManager() {
        return _buttonController;
    }

    /**
     * Hook for subclasses to process URL params before the wizard starts; default implementation is empty.
     * This method is called for each URL param before the first page is rendered.  The name param is guaranteed non-null.
     *
     * @param name  method undefined
     * @param value method undefined
     */
    protected void processURLParam(String name, String value) {
        // empty
    }

    /**
     * Hook for subclasses to define an unusual transition from one wizard page to another.  When
     * the page calls gotoPageByNamedTransition() the Wizard will go to the dest page and set up
     * the back button to return to the calling page.
     */
    //public void addPageTransition(PageTransition pageTransition)
    //{
    //    _pageTransitions.put(pageTransition.getName(), pageTransition);
    //}
}
