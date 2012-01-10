
package org.janelia.it.jacs.web.gwt.common.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import org.gwtwidgets.client.util.Location;
import org.gwtwidgets.client.util.WindowUtils;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.popup.MessagePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupBelowRightAlignedLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Level;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

import java.util.HashMap;
import java.util.Map;

abstract public class BaseEntryPoint implements EntryPoint {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint");
    HTMLPanel _titlePanel = null;
    DockPanel _divContextPanel = null;
    VerticalPanel _breadcrumbPanel = null;
    VerticalPanel _bookmarkPanel = null;
    Map<String, DivContextInfo> _divContextMap = new HashMap<String, DivContextInfo>();
    private boolean bookmarkable = false;

    public static final Level DEFAULT_LOG_LEVEL = Level.ERROR;

    /**
     * Constructor
     */
    protected BaseEntryPoint() {
        configLogLevel();
        catchUncaughtExceptions();
    }

    /**
     * Our Entry Points must implement this
     */
    abstract public void onModuleLoad();

    protected void configLogLevel() {
        String logLevel = SystemProps.getString("clientLogLevel", "ERROR");
        _logger.setLevel(Level.create(logLevel));

        // Retrieve the configured level from the properties
//        _propertyService.getProperty("clientLogLevel", new AsyncCallback() {
//            public void onFailure(Throwable throwable)
//            {
//                _logger.error("Failed to retrieve clientLogLevel property; defaulting to ERROR");
//            }
//            public void onSuccess(Object object)
//            {
//                if (object != null) {
//                    _logger.setLevel(Level.create((String) object));
//                    _logger.info("Setting client-side log level to " + object);
//                }
//            }
//        });
    }

    /**
     * This will catch any uncaught excpeptions before they're released to the browser, and log them
     */
    protected void catchUncaughtExceptions() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable e) {
                _logger.error("Uncaught Exception:", e);
            }
        });
    }

    /**
     * Clears the input container and turns off visiblity so caller can repopulate before reshowing the final layout
     */
    protected void clear() {
        // Clear any widgets added by GWT code and hide the root panel until the subclass has populated it
        RootPanel.get(Constants.ROOT_PANEL_NAME).clear();
        RootPanel.get(Constants.ROOT_PANEL_NAME).setVisible(false);
    }

    protected void clear(final Callback finishedCallback) {
        if (GWT.isScript()) // normal mode
            clearFade(finishedCallback);
        else {             // hosted mode
            clearImmediate();
            if (finishedCallback != null)
                finishedCallback.execute();
        }
    }

    /**
     * Clears the input container usign an async fadeout, and notifies the caller when the fade & clear is complete
     */
    protected void clearFade(final Callback finishedCallback) {
        // After the fade is complete, clear and hide the input container, and notify the caller of completion
        final RootPanel inputContainer = RootPanel.get(Constants.ROOT_PANEL_NAME);
        Callback fadeFinished = new Callback() {
            public void execute() {
                clearDivContext();
                inputContainer.clear();
                //DOM.setStyleAttribute(inputContainer.getElement(), "display", "none");
                if (finishedCallback != null)
                    finishedCallback.execute();
            }
        };

        // Fade out the current contents
        SafeEffect.fade(inputContainer, new EffectOption[]{
                new EffectOption("to", "0.01")
                , new EffectOption("duration", "0.5")
                , new EffectOption("afterFinish", fadeFinished)
        });
    }

    /**
     * In GWT hosted mode we don't have cascading callbacks (due to Scriptaculous bug), so just do everything synchronously
     */
    protected void clearImmediate() {
        final RootPanel inputContainer = RootPanel.get(Constants.ROOT_PANEL_NAME);
        clearDivContext();
        inputContainer.clear();
    }

    public void clearDivContext() {
        if (_divContextPanel != null) {
            RootPanel rootPanel = RootPanel.get(Constants.ROOT_PANEL_NAME);
            rootPanel.remove(_divContextPanel);
        }
        _divContextPanel = null;
        _breadcrumbPanel = null;
        _bookmarkPanel = null;
    }

    protected void show() {
        if (GWT.isScript()) // normal mode
            showFade();
        else                // hosted mode
            showImmediate();
    }

    protected void showFade() {
        final Widget inputContainer = RootPanel.get(Constants.ROOT_PANEL_NAME);

        // Step 2 - show the widgets (at 1% opacity)
        Callback opacityFinished = new Callback() {
            public void execute() {
                // Show the input-container & unshow the "loading" message
                inputContainer.setVisible(true);
                DOM.setStyleAttribute(inputContainer.getElement(), "display", "block");
                Element loadingDiv = DOM.getElementById("loadingMsg");
                if (loadingDiv != null)
                    DOM.setStyleAttribute(loadingDiv, "display", "none");

                // Last, fade in the content
                SafeEffect.fade(inputContainer, new EffectOption[]{
                        new EffectOption("to", "1.0")
                        , new EffectOption("duration", "0.5")
                });
                RootPanel.get(Constants.FOOTER_PANEL_NAME).setVisible(true);
            }
        };

        // Step 1 - set the opacity of the GWT widgets to 1%
        SafeEffect.opacity(inputContainer, new EffectOption[]{
                new EffectOption("to", "0.01")
                , new EffectOption("duration", "0")
                , new EffectOption("afterFinish", opacityFinished)
        });
    }

    /**
     * In GWT hosted mode we don't have cascading callbacks (due to Scriptaculous bug), so just do everything synchronously
     */
    protected void showImmediate() {
        hideLoadingLabel();
        Widget inputContainer = RootPanel.get(Constants.ROOT_PANEL_NAME);
        DOM.setStyleAttribute(inputContainer.getElement(), "display", "block");
        inputContainer.setVisible(true);

        RootPanel.get(Constants.FOOTER_PANEL_NAME).setVisible(true);
    }

    private void hideLoadingLabel() {
        Element loadingDiv = DOM.getElementById("loadingMsg");
        if (loadingDiv != null)
            DOM.setStyleAttribute(loadingDiv, "display", "none");
    }

    public void setBreadcrumb(Breadcrumb breadcrumb, String contentDivName) {
        setBreadcrumb(new Breadcrumb[]{breadcrumb}, contentDivName);
    }

    public void setBreadcrumb(Breadcrumb[] breadcrumbs, String contentDivName) {
        DivContextInfo divContext = getDivContextInfo(contentDivName);
        divContext.setBreadcrumbs(breadcrumbs);
        updateDivContext(contentDivName, divContext);
    }

    private DivContextInfo getDivContextInfo(String contentDivName) {
        DivContextInfo divContext = _divContextMap.get(contentDivName);
        if (divContext == null) {
            divContext = new DivContextInfo();
            _divContextMap.put(contentDivName, divContext);
        }
        return divContext;
    }

    public void setBookmarkable(boolean bookmarkable) {
        this.bookmarkable = bookmarkable;
    }

    public boolean isBookmarkable() {
        return bookmarkable;
    }

    public void clearBookmark(String contentDivName) {
        DivContextInfo divContext = _divContextMap.get(contentDivName);
        if (divContext != null)
            divContext.setBookmarkUrl(null);
    }

    public void setBookmarkUrl(String bookmarkUrl, String contentDivName) {
        DivContextInfo divContext = getDivContextInfo(contentDivName);
        divContext.setBookmarkUrl(bookmarkUrl);
        bookmarkable = true;
        updateDivContext(contentDivName, divContext);
    }

    public void updateDivContext(String contentDivName, DivContextInfo divContext) {
        // Create the panel with the breadcrumbs
        HorizontalPanel breadcrumbs = new HorizontalPanel();

        Image anchorImage = ImageBundleFactory.getControlImageBundle().getBreadcrumbAnchorImage().createImage();
        anchorImage.setStyleName("breadcrumbAnchorImage");
        breadcrumbs.add(anchorImage);

        addBreadcrumbAnchor(breadcrumbs, new Breadcrumb(Constants.TOOLS_LABEL, UrlBuilder.getResearchHomeUrl()));
        for (Breadcrumb link : divContext.getBreadcrumbs())
            addBreadcrumbLink(breadcrumbs, link);

        RootPanel rootPanel = RootPanel.get(contentDivName);
        if (_divContextPanel == null) {
            _divContextPanel = new DockPanel();
            _divContextPanel.setStyleName("breadcrumbDockPanel");
            rootPanel.add(_divContextPanel);
        }

        if (_breadcrumbPanel != null)
            _divContextPanel.remove(_breadcrumbPanel);

        // Need to wrap the HorizontalPanel or else IE will apply styles to every cell
        _breadcrumbPanel = new VerticalPanel();
        _breadcrumbPanel.setStyleName("breadcrumbs");
//        _breadcrumbPanel.add(breadcrumbs);

        _divContextPanel.add(_breadcrumbPanel, DockPanel.WEST);

        addBookmarkLink(divContext);
    }

    private void addBookmarkLink(DivContextInfo divContext) {
        if (_bookmarkPanel != null) {
            _divContextPanel.remove(_bookmarkPanel);
        }
        if (bookmarkable && divContext.getBookmarkUrl() != null) {
            _bookmarkPanel = new VerticalPanel();
            _bookmarkPanel.setStyleName("bookmarkPanel");
            Image linkImage = ImageBundleFactory.getControlImageBundle().getLinkImage().createImage();
            linkImage.setStyleName("linkImage");
            ActionLink bookmarkLink = new ActionLink("Link to this page",
                    linkImage,
                    new PopupBelowRightAlignedLauncher(new MessagePopupPanel("Copy and Paste this URL", divContext.getBookmarkUrl(), "Done")));
            _bookmarkPanel.add(bookmarkLink);
            _divContextPanel.add(_bookmarkPanel, DockPanel.EAST);
            _divContextPanel.setCellHorizontalAlignment(_bookmarkPanel, DockPanel.ALIGN_RIGHT);
        }
    }

    private void addBreadcrumbAnchor(Panel panel, Breadcrumb breadcrumb) {
        addBreadcrumb(panel, breadcrumb, false);
    }

    private void addBreadcrumbLink(Panel panel, Breadcrumb breadcrumb) {
        addBreadcrumb(panel, breadcrumb, true);
    }

    private void addBreadcrumb(Panel panel, Breadcrumb breadcrumb, boolean prependDivider) {
        if (breadcrumb == null || breadcrumb.getLabel() == null)
            return;

        Widget html;
        if (breadcrumb.getUrl() == null)
            html = HtmlUtils.getHtml(breadcrumb.getLabel(), "text");
        else {
            html = new Link(breadcrumb.getLabel(), breadcrumb.getUrl());
            html.addStyleName("breadcrumbLink");
        }

        if (prependDivider)
            panel.add(HtmlUtils.getHtml(">", "breadcrumbDivider"));
        panel.add(html);
    }

    public void setBookmarkId(String id) {
        Location location = WindowUtils.getLocation();
        String href = location.getHref();
        int i = href.lastIndexOf("/jacs");
        String shortUrl = href.substring(0, i) + "/jacs/id?" + id;
        setBookmarkUrl(shortUrl, Constants.ROOT_PANEL_NAME);
    }

    private class DivContextInfo {
        private Breadcrumb[] breadcrumbs;
        private String bookmarkUrl;

        public Breadcrumb[] getBreadcrumbs() {
            return breadcrumbs;
        }

        public void setBreadcrumbs(Breadcrumb[] breadcrumbs) {
            this.breadcrumbs = breadcrumbs;
        }

        public String getBookmarkUrl() {
            return bookmarkUrl;
        }

        public void setBookmarkUrl(String bookmarkUrl) {
            this.bookmarkUrl = bookmarkUrl;
        }
    }


    public class Breadcrumb {
        private String _label;
        private String _url;

        public Breadcrumb(String label) {
            this(label, null);
        }

        public Breadcrumb(String label, String url) {
            _label = label;
            _url = url;
        }

        public String getLabel() {
            return _label;
        }

        public void setLabel(String label) {
            _label = label;
        }

        public String getUrl() {
            return _url;
        }

        public void setUrl(String url) {
            _url = url;
        }
    }
}
