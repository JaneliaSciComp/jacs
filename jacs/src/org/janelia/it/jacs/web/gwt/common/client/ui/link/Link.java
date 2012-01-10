
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.SimpleTooltipPopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;

/**
 * A true hyperlink (the GWT Hyperlink is for 'internal' links using history tokens).  Has 2 use cases:
 * <ol>
 * <li>Pass the destination URL and the URL will be invoked when the user clicks the link (regular link)</li>
 * <li>Pass a clickListener so you are notified when the link is clicked and can take appropriate action<li>
 * </ol>
 *
 * @author Michael Press
 */
public class Link extends HTML {
    protected String _linkText = null;
    protected String _destURL = null;
    protected String _hyperlinkStyleName = null;
    protected ClickListener _clickListener = null;

    public Link(String linkText, ClickListener clickListener) {
        super(""); // we'll add the HTML manually later
        setHyperlinkStyleName("textLink");
        init(linkText, null, clickListener);
    }

    public Link(String linkText, String destURL) {
        super(""); // we'll add the HTML manually later
        setHyperlinkStyleName("textLink");
        init(linkText, destURL, null);
    }

    /**
     * for subclass use only - defers initialization
     */
    protected Link(String linkText, String destURL, ClickListener clickListener) {
        super("");
        _linkText = linkText;
        _destURL = destURL;
        _clickListener = clickListener;
    }

    protected void init(String linkText, String destURL, ClickListener clickListener) {
        _linkText = linkText;
        _destURL = destURL;

        // remove any old clicklistener
        if (_clickListener != null)
            super.removeClickListener(_clickListener);
        _clickListener = clickListener;
        if (_clickListener != null)
            super.addClickListener(_clickListener);

        setHTML(getInnerHTML());

        // Last, automatically set the debug ID (optimized out for production)
        if (_linkText != null)
            ensureDebugId(_linkText.replaceAll(" ", "") + "Link");
    }

    protected String getInnerHTML() {
        StringBuffer html = new StringBuffer();
        html.append("<a class='").append(getHyperlinkStyleName()).append("'");
        if (_destURL != null && !_destURL.equals(""))
            html.append(" href='").append(_destURL).append("'");
        //else
        //    html.append(" href='#notoken'"); // allows right-click "open in new tab" menu in browser
        if (getTargetWindow() != null)
            html.append(" target='").append(getTargetWindow()).append("'");
        html.append(">");
        html.append(getLinkContents());
        html.append("</a>");
        return html.toString();
    }

    protected String getLinkContents() {
        if (_linkText != null)
            return _linkText;
        return "";
    }

    // Allows subclasses to specify which window the link should open in
    protected String getTargetWindow() {
        return null;
    }

    public void setLinkText(String linkText) {
        init(linkText, _destURL, _clickListener);
    }

    public void addClickListener(ClickListener clickListener) {
        init(_linkText, _destURL, clickListener);
    }

    public void setDestURL(String destURL) {
        init(_linkText, destURL, _clickListener);
    }

    public String getLinkText() {
        return _linkText;
    }

    public String getDestURL() {
        return _destURL;
    }

    /**
     * Overrides normal setStyleName to use setHyperLinkStyleName() so that the style applies to the hyperlink part
     * of this link but not any other wrapping content.
     */
    public void setStyleName(String styleName) {
        setHyperlinkStyleName(styleName);
    }

    /**
     * Style of the hyperlink itself, but not any wrapping content
     */
    public void setHyperlinkStyleName(String styleName) {
        _hyperlinkStyleName = styleName;
        init(_linkText, _destURL, _clickListener);
    }

    /**
     * Style name for the hyperlink part of this link
     */
    public String getHyperlinkStyleName() {
        return _hyperlinkStyleName;
    }

    /**
     * Creates a mouse listener that pops up a SimpleTooltipPopup when the mouse hovers on this link
     */
    public void setHoverPopup(String popupText) {
        if (popupText != null) {
            BasePopupPanel popup = new SimpleTooltipPopup(popupText);
            setHoverPopup(popup, new PopupAboveLauncher(popup));
        }
    }

    /**
     * Creates a mouse listener that pops up the supplied Popup when the mouse hovers on this link
     */
    public void setHoverPopup(BasePopupPanel popup, final PopupLauncher launcher) {
        final Widget This = this; // necessary for inner class reference
        launcher.setPopup(popup);
        addMouseListener(new MouseListenerAdapter() {
            public void onMouseEnter(Widget widget) {
                launcher.onHover(This);
            }

            public void onMouseLeave(Widget widget) {
                launcher.afterHover(This);
            }
        });
    }
}
