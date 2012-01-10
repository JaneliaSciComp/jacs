
package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HasActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HideShowActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ToggleActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * A rounded box with a title that "floats" on the top line of the box.
 */
// TODO: extract code in common with TiteldPanel (actionlink, realizenow) into common superclass (BaseTitledArea?)
public class TitledBox extends VerticalPanel {
    //TODO: can share action link stuff with TitledPanel?  Extend common parent class TitledThing?
    private String _title;
    private VerticalPanel _contentsPanel = new VerticalPanel();
    private Panel _contentsPanelWrapper;
    private Label _titleLabel; // title of panel
    private Panel _titleLabelPanel; // wrapper for the title label area (inside the rounded panel)
    private HorizontalPanel _titleWrapper;
    private RoundedPanel2 _titleRoundedPanel; // rounded panel around title
    private RoundedPanel2 _roundedPanel; // rounded panel around main body
    private boolean _showActionLinks = true;
    private boolean _showContent = true;

    private SimplePanel _actionLinkWrapper; // left and right padding around action links
    private HorizontalPanel _actionLinkPanel; // panel of action links
    private String _actionLinkBackgroundStyleName = "titledBoxActionLinkBackground"; // default
    private HideShowActionLink _hideShowActionLink;


    public TitledBox(String title) {
        this(title, true);
    }

    public TitledBox(String title, boolean showActionLinks) {
        this(title, showActionLinks, true);
    }

    public TitledBox(String title, boolean showActionLinks, boolean showContent) {
        this(title, showActionLinks, showContent, true);
    }

    public TitledBox(String title, boolean showActionLinks, boolean showContent, boolean initNow) {
        super();
        setShowActionLinks(showActionLinks);
        _showContent = showContent;
        _title = title;
        if (initNow)
            init();
    }

    public void realize() {
        init();
    }

    protected void init() {
        // Our parent Panel functions as an outer _contenstPanelWrapper to set offset styles
        setStyleName("titledBox");

        // Add the contents panel and wrapper (to use spacers instead of CSS padding because IE adds spacing to each
        // element in the vertical panel)
        _contentsPanel.setWidth("100%");
        _contentsPanelWrapper = new VerticalPanel();
        _contentsPanelWrapper.setWidth("100%");
        _contentsPanelWrapper.add(HtmlUtils.getHtml("&nbsp;", "titledBoxTopSpacer"));
        _contentsPanelWrapper.add(_contentsPanel);
        _contentsPanelWrapper.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        setContentsPanelStyleName("titledBoxContentsPanel"); // takes care of _contentsPanel and _contentsPanelWrapper

        // Rounded panel for the title.  Wrapped so it can be moved ontop of the top line
        _titleWrapper = new HorizontalPanel();
        _titleWrapper.setStyleName("titledBoxLocator");

        _titleLabelPanel = new SimplePanel();
        setTitle(_title);
        _titleRoundedPanel = new RoundedPanel2(_titleLabelPanel, RoundedPanel2.ALL, getTitleBorderColor(), RoundedPanel2.ROUND_MEDIUM);
        _titleRoundedPanel.setCornerStyleName("titledBoxLabelRounding");
        _titleWrapper.add(_titleRoundedPanel);

        // Add the action links to the title wrapper
        if (getShowActionLinks()) {
            showActionLinks();
        }

        //TODO: get CSS style dynamically
        _roundedPanel = new RoundedPanel2(_contentsPanelWrapper, RoundedPanel2.ALL, getBorderColor(), RoundedPanel2.ROUND_LARGE, _titleWrapper);
        setCornerStyleName("titledBoxRounding");

        // Finally, add the rounded panel (which contains the title and the contents panel) to the real parent panel
        super.add(_roundedPanel);

        // Hook for subclasses to add content
        popuplateContentPanel();

        // Last, automatically set the debug ID (optimized out for production)
        ensureDebugId(_title.replaceAll(" ", "") + "TitledBox");
    }

    protected void showActionLinks() {
        // Need a wrapper around the action links to get padding right in IE
        _actionLinkWrapper = new SimplePanel();
        setActionLinkPanelStyleName("titledBoxActionLinkWrapper");

        _actionLinkPanel = new HorizontalPanel();
        _actionLinkWrapper.add(_actionLinkPanel);

        createActionLinks();

        _titleWrapper.add(_actionLinkWrapper);
        _titleWrapper.setCellVerticalAlignment(_actionLinkPanel, VerticalPanel.ALIGN_MIDDLE);
    }

    /**
     * Set the title string
     */
    public void setTitle(String title) {
        setTitle(title, "titledBoxLabel");
    }

    /**
     * Set the title string; allows override of title label style
     */
    public void setTitle(String title, String styleName) {
        _title = title;
        _titleLabel = new Label(_title);
        _titleLabel.setStyleName(styleName);
        setTitle(_titleLabel);
    }

    /**
     * Set the title as an externally-supplied widget
     */
    public void setTitle(Widget widget) {
        _titleLabelPanel.clear();
        _titleLabelPanel.add(widget);
    }

    /**
     * Hook for subclasses to add to/change the action links in linkpanel - call addActionLink(HasActionLink) to add
     */
    protected void createActionLinks() {
        _hideShowActionLink = new HideShowActionLink(_contentsPanel, _contentsPanelWrapper,
                (_showContent ? ToggleActionLink.PRIMARY_STATE : ToggleActionLink.SECONDARY_STATE));
        addActionLink(_hideShowActionLink);
    }

    public void showContent() {
        if (getShowActionLinks())
            _hideShowActionLink.toggleToPrimaryState();

    }

    public void hideContent() {
        if (getShowActionLinks())
            _hideShowActionLink.toggleToSecondaryState();
    }

    public HideShowActionLink getHideShowActionLink() {
        return _hideShowActionLink;
    }

    /**
     * Appends an action link to the right of any existing ActionLinks
     */
    public void addActionLink(HasActionLink actionLink) {
        if (_actionLinkPanel == null)
            return;

        // Have to manually add a spacer since IE misapplies margins in HorizontalPanels
        HTML spacer;
        if (_actionLinkPanel.getWidgetCount() > 0)
            spacer = HtmlUtils.getHtml("&nbsp;", "titledBoxActionLinkSpacer");
        else
            spacer = HtmlUtils.getHtml("&nbsp;", "titledBoxActionLinkFirstSpacer");
        spacer.addStyleName(getActionLinkBackgroundStyleName());
        _actionLinkPanel.add(spacer);

        ((ActionLink) actionLink).addLinkStyleName(getActionLinkBackgroundStyleName());
        _actionLinkPanel.add((Widget) actionLink);
    }

    public void setActionLinkBackgroundStyleName(String styleName) {
        if (_actionLinkPanel != null) {
            _actionLinkBackgroundStyleName = styleName;
            for (int i = 0; i < _actionLinkPanel.getWidgetCount(); i += 2) // add the style to spacer widgets (even widgets)
                _actionLinkPanel.getWidget(i).addStyleName(styleName);
            for (int i = 1; i < _actionLinkPanel.getWidgetCount(); i += 2) // add the style name to the action link text (odd widgets)
                ((ActionLink) _actionLinkPanel.getWidget(i)).addLinkStyleName(styleName);
        }
    }

    public String getActionLinkBackgroundStyleName() {
        return _actionLinkBackgroundStyleName;
    }

    public void removeActionLinks() {
        _actionLinkPanel.clear();
    }

    protected void popuplateContentPanel() {
    }

    protected String getBorderColor() {
        //TODO: get this dynamically from CSS style
        return "#777777";
    }

    protected String getTitleBorderColor() {
        return getBorderColor();
    }

    /**
     * Override the FlowPanel add to add Widgets to the content panel
     */
    public void add(Widget widget) {
        _contentsPanel.add(widget);
    }

    public boolean remove(Widget widget) {
        return _contentsPanel.remove(widget);
    }

    public void setContentsPanelStyleName(String styleName) {
        _contentsPanel.setStyleName(styleName);
        _contentsPanelWrapper.setStyleName(styleName + "Wrapper");  // hack
    }

    public void setCornerStyleName(String styleName) {
        _roundedPanel.setCornerStyleName(styleName);
    }

    public void setLabelPanelStyleName(String styleName) {
        _titleLabelPanel.setStyleName(styleName);
    }

    public void setLabelStyleName(String styleName) {
        setTitle(_title, styleName);
    }

    public void addLabelStyleName(String styleName) {
        if (_titleLabel != null)
            _titleLabel.addStyleName(styleName);
    }

    public void setLabelCornerStyleName(String styleName) {
        _titleRoundedPanel.setCornerStyleName(styleName);
    }

    public void addContentsPanelStyleName(String styleName) {
        _contentsPanel.addStyleName(styleName);
    }

    public void setActionLinkPanelStyleName(String styleName) {
        if (getShowActionLinks())
            _actionLinkWrapper.setStyleName(styleName);
    }

    public void setShowActionLinks(boolean showActionLinks) {
        _showActionLinks = showActionLinks;
    }

    public boolean getShowActionLinks() {
        return _showActionLinks;
    }

    public void clear() {
        this._contentsPanel.clear();
    }

    public void clearContent() {
        this._contentsPanel.clear();
    }

    /**
     * for subclass access
     */
    protected Label getTitleLabel() {
        return _titleLabel;
    }

    /**
     * for subclass access
     */
    protected void setTitleLabel(Label titleLabel) {
        _titleLabel = titleLabel;
    }

    /**
     * for subclass access
     */
    protected RoundedPanel2 getTitleRoundedPanel() {
        return _titleRoundedPanel;
    }

    public VerticalPanel getContentPanel() {
        return _contentsPanel;
    }
}

