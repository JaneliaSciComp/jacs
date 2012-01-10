
package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;

/**
 * This class is the base class for content expanders.  It is a refactored version of ContentExpander
 * put together by Michael Press and Les Foster
 *
 * @author Tareq Nabeel
 */
public abstract class BaseContentExpander implements ClickListener {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.BaseContentExpander");

    private boolean isFullSize = false;
    private ActionLink actionLink;

    // Defaulted values may be overridden.
    private String expandedLinkValue = "more";
    private String shrinkLinkValue = "less";

    private HorizontalPanel parentContentPanel;
    private HTMLPanel currentContentPanel;
    private HTMLPanel expandedContentPanel;
    private HTMLPanel shrinkContentPanel;

    private String expandedContent;
    private String shrinkContent;

    private String actionLinkId;
    public static final int DEFAULT_MIN_CHARS = 200;
    private int minSize = DEFAULT_MIN_CHARS;
    private boolean expandable = true; // whether to show expansion controls


    public BaseContentExpander() {
        parentContentPanel = new HorizontalPanel();
    }

    public BaseContentExpander(int minSize, String actionLinkId) {
        this(false, minSize, actionLinkId);
    }

    public BaseContentExpander(boolean initiallyFullSize, int minSize, String actionLinkId) {
        this();
        this.setFullSize(initiallyFullSize);
        this.setMinSize(minSize);
        this.setActionLinkId(actionLinkId);
    }

    /**
     * Builds the different components of the small and big panels.  Order of method calls matter.
     * Method could be moved to Builder over time.
     */
    protected void createPanels() {
        createShrinkContentAndSetExpandable();
        if (isExpandable()) {
            setActionLink(new ActionLink(getDisplay(), this));
            addActionLinks();
            createExpandPanel();
        }
        createShrinkPanel();
        setCurrentContentPanel(isFullSize() ? getExpandContentPanel() : getShrinkContentPanel());
        resetParentPanel();
    }

    /**
     * This is what clients of BaseContentExpander would call to
     */
    public Panel getContentPanel() {
        return parentContentPanel;
    }

    /**
     * Allow override of display for expanded mode.
     *
     * @param expandedLinkValue what to show in link.
     */
    public void setExpandedLinkValue(String expandedLinkValue) {
        this.expandedLinkValue = expandedLinkValue;
    }

    /**
     * Allow override of display string for shrunken mode.
     *
     * @param shrinkLinkValue what to show in link when widget is diminished in size.
     */
    public void setShrinkLinkValue(String shrinkLinkValue) {
        this.shrinkLinkValue = shrinkLinkValue;
    }

    /**
     * Return whatever is set for display when in expanded mode, for external use.
     *
     * @return display string set currently when widget is expanded.
     */
    public String getExpandedLinkValue() {
        return expandedLinkValue;
    }

    /**
     * Return whatever is set for display when in shrunken mode, for external use.
     *
     * @return display string set currently when widget is shrunken.
     */
    public String getShrinkLinkValue() {
        return shrinkLinkValue;
    }

    /**
     * Would be called from subclass to set the expanded content
     *
     * @param content
     */
    protected void setExpandedContent(String content) {
        this.expandedContent = content;
    }

    /**
     * Would be called from subclass to set the shrink content
     *
     * @param content
     */
    protected void setShrinkContent(String content) {
        this.shrinkContent = content;
    }

    /**
     * Returns the expanded content as a string
     */
    protected String getExpandedContent() {
        return expandedContent;
    }

    /**
     * Returns the shrink content as a string
     */
    protected String getShrinkContent() {
        return shrinkContent;
    }

    /**
     * This method creates the panel that would be displayed when the expand link is clicked
     */
    private void createExpandPanel() {
        setExpandedContent();
        HTMLPanel contentPanel = new HTMLPanel(getExpandedContent());
        contentPanel.add(getActionLink(), getActionLinkId());
        setExpandContentPanel(contentPanel);
    }

    /**
     * This method creates the panel that would be displayed when the shrink link is clicked
     */
    private void createShrinkPanel() {
        setShrinkContent();
        HTMLPanel contentPanel = new HTMLPanel(getShrinkContent());
        if (isExpandable()) {
            contentPanel.add(getActionLink(), getActionLinkId());
        }
        this.setShrinkContentPanel(contentPanel);
    }


    /**
     * Sets the current content panel
     *
     * @param currentContentPanel
     */
    private void setCurrentContentPanel(HTMLPanel currentContentPanel) {
        this.currentContentPanel = currentContentPanel;
    }

    /**
     * Changes the current panel of the container to currentContentPanel.  CurrentContentPanel
     * would have been changed before this method is called.
     */
    private void resetParentPanel() {
        parentContentPanel.clear();
        parentContentPanel.add(currentContentPanel);
    }


    /**
     * Return string that should be displayed for the link, given the desired mode (as given
     * in other parameter).
     *
     * @return a suitable string for an anchor text.
     */
    private String getDisplay() {
        return isExpanded() ? getShrinkLinkValue() : getExpandedLinkValue();
    }

    /**
     * Return boolean current expansion mode.  True=expanded.
     *
     * @return t=expanded, f=shrunken.
     */
    private boolean isExpanded() {
        return isFullSize;
    }

    public void onClick(Widget sender) {
        if (actionLink == null)
            return;

        if (isFullSize)
            shrink();
        else
            expand();
    }

    /**
     * This method switches the currentContentPanel to expandedContentPanel
     */
    private void expand() {
        // Minimalist behavior.
        currentContentPanel = expandedContentPanel;
        resetActionLink(getShrinkLinkValue());
        resetParentPanel();
        isFullSize = true;
    }

    /**
     * This method switches the currentContentPanel to shrinkContentPanel
     */
    private void shrink() {
        // Minimalist behavior.
        currentContentPanel = shrinkContentPanel;
        resetActionLink(getExpandedLinkValue());
        resetParentPanel();
        isFullSize = false;
    }

    /**
     * This method switches the actionlink e.g. "more" to "less" or vice versa
     *
     * @param linkText
     */
    private void resetActionLink(String linkText) {
        currentContentPanel.remove(actionLink);
        actionLink.setText(linkText);
        currentContentPanel.add(actionLink, actionLinkId);
    }

    protected void setExpandable(boolean isExpandable) {
        expandable = isExpandable;
    }

    private boolean isExpandable() {
        return expandable;
    }

    private boolean isFullSize() {
        return isFullSize;
    }

    private void setFullSize(boolean fullSize) {
        isFullSize = fullSize;
    }

    private ActionLink getActionLink() {
        return actionLink;
    }

    private void setActionLink(ActionLink actionLink) {
        this.actionLink = actionLink;
    }

    private HTMLPanel getExpandContentPanel() {
        return expandedContentPanel;
    }

    private void setExpandContentPanel(HTMLPanel contentPanel) {
        this.expandedContentPanel = contentPanel;
    }

    private HTMLPanel getShrinkContentPanel() {
        return shrinkContentPanel;
    }

    private void setShrinkContentPanel(HTMLPanel shortContentPanel) {
        this.shrinkContentPanel = shortContentPanel;
    }

    protected String getActionLinkId() {
        return actionLinkId;
    }

    private void setActionLinkId(String actionLinkId) {
        this.actionLinkId = actionLinkId;
    }

    protected int getMinSize() {
        return minSize;
    }

    private void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    protected abstract void createShrinkContentAndSetExpandable();

    protected abstract void addActionLinks();

    protected abstract void setShrinkContent();

    protected abstract void setExpandedContent();

}
