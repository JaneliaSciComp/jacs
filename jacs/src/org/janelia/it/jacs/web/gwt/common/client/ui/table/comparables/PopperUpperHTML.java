
package org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.SimpleTooltipPopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * An extension of the GWT HTML Widget - displays one version of text, and on hover pops up a panel with another version
 * of the text (such as the complete text).  For performance reasons, doesn't create the popup until the
 * first mouse hover.
 *
 * @author Michael Press
 */
public class PopperUpperHTML extends HTML {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.PopperUpperHTML");

    private String _text = null;
    private String _textStyleName = DEFAULT_TEXT_STYLE;
    private String _hoverStyleName = DEFAULT_HOVER_STYLE;
    private String _hostTextStyleName = DEFAULT_HOST_TEXT_STYLE;
    private String _hostHoverStyleName = DEFAULT_HOST_HOVER_STYLE;
    private String _popupTextStyleName = DEFAULT_POPUP_TEXT_STYLE;
    private String _clickStyleName = DEFAULT_HOST_CLICK_STYLE;
    private HTML _popupContents = null;
    private PopupLauncher _launcher = null;
    private boolean _needHoverPopup = DEFAULT_NEED_HOVER_POPUP;
    private boolean _needClickPopup = DEFAULT_NEED_CLICK_POPUP;
    // Default styles for the "host" widget of the popup
    public static final String DEFAULT_TEXT_STYLE = "text";        // for the item that "hosts" the popup
    public static final String DEFAULT_HOVER_STYLE = "textHover";  // the host on hover
    // Styles if the host needs a popup
    public static final String DEFAULT_HOST_TEXT_STYLE = "popperUpperText";        // for the item that "hosts" the popup
    public static final String DEFAULT_HOST_HOVER_STYLE = "popperUpperTextHover";  // the host on hover
    public static final String DEFAULT_HOST_CLICK_STYLE = "popperUpperTextClick";
    // Style for the popup text
    public static final String DEFAULT_POPUP_TEXT_STYLE = "text";             // within the popup

    public static final boolean DEFAULT_NEED_HOVER_POPUP = true;
    public static final boolean DEFAULT_NEED_CLICK_POPUP = false;

    protected PopperUpperHTML(String text) {
        this(text, DEFAULT_TEXT_STYLE, DEFAULT_HOVER_STYLE, DEFAULT_HOST_TEXT_STYLE, DEFAULT_HOST_HOVER_STYLE, null);
    }

    public PopperUpperHTML(String text, HTML popupContents) {
        this(text, DEFAULT_TEXT_STYLE, DEFAULT_HOVER_STYLE, DEFAULT_HOST_TEXT_STYLE, DEFAULT_HOST_HOVER_STYLE, popupContents);
    }

    public PopperUpperHTML(String text, String hostTextStyleName, String hostHoverName, HTML popupContents) {
        this(text, DEFAULT_TEXT_STYLE, DEFAULT_HOVER_STYLE, hostTextStyleName, hostHoverName, DEFAULT_HOST_CLICK_STYLE, popupContents, true, DEFAULT_NEED_HOVER_POPUP, DEFAULT_NEED_CLICK_POPUP);
    }

    public PopperUpperHTML(String text, String textStyleName, String hoverStyleName, String hostStyleName,
                           String hostHoverName, HTML popupContents) {
        this(text, textStyleName, hoverStyleName, hostStyleName, hostHoverName, DEFAULT_HOST_CLICK_STYLE, popupContents, true, DEFAULT_NEED_HOVER_POPUP, DEFAULT_NEED_CLICK_POPUP);
    }

    /**
     * subclass use to defer initialization
     */
    protected PopperUpperHTML(String text, HTML popupContents, boolean initNow) {
        this(text, DEFAULT_TEXT_STYLE, DEFAULT_HOVER_STYLE, DEFAULT_HOST_TEXT_STYLE, DEFAULT_HOST_HOVER_STYLE, DEFAULT_HOST_CLICK_STYLE, popupContents, initNow, DEFAULT_NEED_HOVER_POPUP, DEFAULT_NEED_CLICK_POPUP); // will update abbreviated text in init()
    }

    /**
     * subclass use to defer initialization
     */
    protected PopperUpperHTML(String text, String textStyleName, String hoverStyleName, String hostStyleName,
                              String hostHoverName, String hostClickName, HTML popupContents, boolean initNow, boolean needHoverPopup, boolean needClickPopup) {
        super();
        setVisibleText(text);
        setPopupContents(popupContents);
        _textStyleName = textStyleName;
        _hoverStyleName = hoverStyleName;
        _hostTextStyleName = hostStyleName;
        _hostHoverStyleName = hostHoverName;
        _clickStyleName = hostClickName;
        _needHoverPopup = needHoverPopup;
        _needClickPopup = needClickPopup;
        if (initNow)
            init();
    }


    public void setVisibleText(String text) {
        _text = text;
    }

    public void setPopupContents(String popupContents) {
        setPopupContents(HtmlUtils.getHtml(popupContents, _popupTextStyleName));
    }

    public void setPopupContents(HTML popupContents) {
        _popupContents = popupContents;
    }

    protected void init() {
        // Set the initial display html
        setHTML(_text);
        setTextStyleNames(_textStyleName, _hostTextStyleName, _hostHoverStyleName);
        setWordWrap(false);

        // Create a popup with the full text
        if (needHoverPopup())
            addMouseListener(new PopperUpperHTML.HoverMouseListener(this));

        if (needClickPopup())
            addClickListener(new PopperUpperHTML.ClickMouseListener(this));
    }

    public void setTextStyleNames(String textStyleName, String hostTextStyleName, String hostHoverTextStyleName) {
        if (needHoverPopup()) {
            _hostTextStyleName = hostTextStyleName;
            _hostHoverStyleName = hostHoverTextStyleName;
            setStyleName(_hostTextStyleName);
        }
        else {
            _textStyleName = textStyleName;
            setStyleName(_textStyleName);
        }
    }

    /**
     * Determine if we need to show a popup - always true (allows subclasses to override)
     */
    protected boolean needHoverPopup() {
        return _needHoverPopup;
    }

    protected boolean needClickPopup() {
        return _needClickPopup;
    }

    /**
     * Defers creation of popup until the first time it's needed
     */
    public class HoverMouseListener extends MouseListenerAdapter {
        private Widget _baseWidget;

        public HoverMouseListener(Widget baseWidget) {
            _baseWidget = baseWidget;
        }

        public void onMouseEnter(Widget widget) {
            setStyleName(needHoverPopup() ? _hostHoverStyleName : _hoverStyleName);
            getLauncher().onHover(_baseWidget);
        }

        public void onMouseLeave(Widget widget) {
            setStyleName(needHoverPopup() ? _hostTextStyleName : _textStyleName);
            getLauncher().afterHover(_baseWidget);
        }
    }

    public class ClickMouseListener implements ClickListener {
        private Widget _baseWidget;

        public ClickMouseListener(Widget baseWidget) {
            _baseWidget = baseWidget;
        }

        public void onClick(Widget widget) {
            setStyleName(needClickPopup() ? _clickStyleName : _hoverStyleName);
            getLauncher().onClick(_baseWidget);
        }

    }

    public PopupLauncher getLauncher() {
        if (_launcher == null)
            createLauncher();
        return _launcher;
    }

    public void setLauncher(PopupLauncher launcher) {
        _launcher = launcher;
        configLauncher();
    }

    private void createLauncher() {
        _launcher = new PopupAboveLauncher();
        configLauncher();
    }

    protected void configLauncher() {
        _launcher.setPopup(new SimpleTooltipPopup(_popupContents));
        _launcher.setDelay(300);
    }

}
