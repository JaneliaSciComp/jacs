
package org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.NumberUtils;
import org.janelia.it.jacs.web.gwt.search.client.panel.SelectionCounter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 6, 2007
 * Time: 3:17:27 PM
 */
public class SearchIconPanel extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanel");

    // Overridable by subclasses
    protected String STYLE_NAME = "SearchIconPanel";
    protected String HEADER_STYLENAME = "SearchIconTitle";
    protected String FOOTER_STYLENAME = "SearchIconFooter";
    protected String ICON_STYLENAME = "SearchIconImage";
    protected String DEFAULTICON_WIDTH = "62px";
    protected String DEFAULTICON_HEIGHT = "50px";
    protected String PANEL_HOVER = "SearchIconPanelHover";
    protected String PANEL_SELECTED = "SearchIconPanelSelected";
    protected String PANEL_UNSELECTED = "SearchIconPanelUnselected";

    private Image _iconImage;
    private HTML _headerHtml;
    private HTML _footerHtml;
    private boolean _selected = false;
    private Set _unselectTheseOnSelect = new HashSet();
    private SelectionCounter _selectionCounter;
    private boolean _highlightMatches = true;

    private String panelUnselectedStylename = PANEL_UNSELECTED;
    private String panelSelectedStylename = PANEL_SELECTED;
    private String panelHoverStylename = PANEL_HOVER;

    private String iconStyleName = ICON_STYLENAME;
    private String iconWidth = DEFAULTICON_WIDTH;
    private String iconHeight = DEFAULTICON_HEIGHT;

    private String headerStyleName = HEADER_STYLENAME;
    private String footerStyleName = FOOTER_STYLENAME;

    public SearchIconPanel(Image iconImage, String header, SelectionCounter selectionCounter) {
        this._iconImage = iconImage;
        this._selectionCounter = selectionCounter;
        init(header);
    }

    private void init(String header) {
        setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        // Set styles
        addStyleName(getPanelUnselectedStyleName()); // start with "unselected" style
        _iconImage.addStyleName(getIconStyleName());

        // Create the panel contents
        _headerHtml = HtmlUtils.getHtml(header, getHeaderStyleName());
        _footerHtml = HtmlUtils.getHtml("&nbsp;", getFooterStyleName()); // placeholder so highlight colors look right
        add(_headerHtml);
        add(_iconImage);
        add(_footerHtml);

        // Create ClickListener and MouseListener, but let subclasses override
        ClickListener clickListener = createClickListener();
        MouseListener mouseListener = createMouseListener();
        if (clickListener != null)
            addClickListener(clickListener);
        if (mouseListener != null)
            addMouseListener(mouseListener);

        _iconImage.ensureDebugId(header + "SearchIconPanelImage");
        _headerHtml.ensureDebugId(header + "SearchIconPanelHeader");
        _footerHtml.ensureDebugId(header + "SearchIconPanelFooter");
    }

    protected ClickListener createClickListener() {
        return new SearchIconMouseManager(this);
    }

    protected MouseListener createMouseListener() {
        return new SearchIconMouseManager(this);
    }

    public String getIconStyleName() {
        return iconStyleName;
    }

    public void setIconStyleName(String iconStyleName) {
        if (iconStyleName == null) {
            this.iconStyleName = ICON_STYLENAME;
        }
        else {
            this.iconStyleName = iconStyleName;
        }
        if (_iconImage != null) {
            _iconImage.setStyleName(this.iconStyleName);
        }
    }

    public void addIconStyleName(String iconStyleName) {
        if (_iconImage != null)
            _iconImage.addStyleName(iconStyleName);
    }

    public String getIconWidth() {
        return iconWidth;
    }

    public String getIconHeight() {
        return iconHeight;
    }

    public void setIconSize(String width, String height) {
        if (width == null) {
            this.iconWidth = DEFAULTICON_WIDTH;
        }
        else {
            this.iconWidth = width;
        }
        if (height == null) {
            this.iconHeight = DEFAULTICON_HEIGHT;
        }
        else {
            this.iconHeight = height;
        }
        if (_iconImage != null) {
            _iconImage.setSize(iconWidth, iconHeight);
        }
    }

    public String getHeaderStyleName() {
        return headerStyleName;
    }

    public void setHeaderStyleName(String headerStyleName) {
        if (headerStyleName == null) {
            this.headerStyleName = HEADER_STYLENAME;
        }
        else {
            this.headerStyleName = headerStyleName;
        }
        if (_headerHtml != null) {
            _headerHtml.setStyleName(this.headerStyleName);
        }
    }

    public String getFooterStyleName() {
        return footerStyleName;
    }

    public void setFooterStyleName(String footerStyleName) {
        if (footerStyleName == null) {
            this.footerStyleName = FOOTER_STYLENAME;
        }
        else {
            this.footerStyleName = footerStyleName;
        }
        if (_footerHtml != null) {
            _footerHtml.setStyleName(this.footerStyleName);
        }
    }

    public void setSelectionCounter(SelectionCounter selectionCounter) {
        _selectionCounter = selectionCounter;
    }

    public SelectionCounter getSelectionCounter() {
        return _selectionCounter;
    }

    public void setNumMatches(Integer numMatches) {
        setNumMatches((numMatches == null) ? 0 : numMatches.intValue());
    }

    public void setNumMatches(int numMatches) {
        setFooterHTML(numMatches);
        _footerHtml.setStyleName(getFooterStyleName());
        if (getHighlightMatches())
            highlightFooter();
    }

    public void setNoMatches() {
        setFooterHTML("--");
    }

    protected void setFooterHTML(int numMatches) {
        setFooterHTML(NumberUtils.formatInteger(numMatches));
    }

    protected void setFooterHTML(String html) {
        _footerHtml.setHTML(html);
    }

    private void highlightFooter() {
        //Store the current background color
        final String bg = DOM.getStyleAttribute(_footerHtml.getElement(), "backgroundColor");
        ;

        new Timer() {
            public void run() {
                SafeEffect.highlight(_footerHtml, new EffectOption[]{
                        new EffectOption("duration", "3.0")
                        , new EffectOption("afterFinish", new Callback() {
                            /** Have to forcibly unset the the highlight color since it sometimes sticks around */
                            public void execute() {
                                DOM.setStyleAttribute(_footerHtml.getElement(), "backgroundColor", bg);
                            }
                        })
                });
            }
        }.schedule(1);
    }

    public boolean isSelected() {
        return _selected;
    }

    public void clearFooter() {
        _footerHtml.setHTML("&nbsp;");
    }

    public void unSelect() {
        if (_selected) {
            _selected = false;
            this.removeStyleName(getPanelSelectedStyleName());
            this.addStyleName(getPanelUnselectedStyleName());
            _selectionCounter.decrement();
        }
    }

    public void select() {
        if (!_selected) {
            _selected = true;
            this.removeStyleName(getPanelUnselectedStyleName());
            this.addStyleName(getPanelSelectedStyleName());
            _selectionCounter.increment();
        }
    }

    public void addIconToUnselect(SearchIconPanel toUnselect) {
        _unselectTheseOnSelect.add(toUnselect);
    }

    public void unselectOtherIcons() {
        Iterator iter = _unselectTheseOnSelect.iterator();
        while (iter.hasNext()) {
            SearchIconPanel sip = (SearchIconPanel) iter.next();
            sip.unSelect();
        }
    }

    public String getHeader() {
        return _headerHtml.getText();
    }

    public void setPanelHoverStyleName(String panelHoverStylename) {
        this.panelHoverStylename = panelHoverStylename;
    }

    public void setPanelSelectedStyleName(String panelSelectedStylename) {
        this.panelSelectedStylename = panelSelectedStylename;
    }

    public void setPanelUnselectedStyleName(String panelUnselectedStylename) {
        this.panelUnselectedStylename = panelUnselectedStylename;
        setStyleName(panelUnselectedStylename);
    }

    public String getPanelUnselectedStyleName() {
        return panelUnselectedStylename;
    }

    public String getPanelHoverStyleName() {
        return panelHoverStylename;
    }

    public String getPanelSelectedStyleName() {
        return panelSelectedStylename;
    }

    public void toggleSelected() {
        _selected = !_selected;
    }

    public void addClickListener(ClickListener clickListener) {
        _iconImage.addClickListener(clickListener);
    }

    public void addMouseListener(MouseListener mouseListener) {
        _iconImage.addMouseListener(mouseListener);
    }

    public boolean getHighlightMatches() {
        return _highlightMatches;
    }

    public void setHighlightMatches(boolean highlightMatches) {
        _highlightMatches = highlightMatches;
    }

    public void setEnabled(boolean enabled) {
        if (!GWT.isScript()) return;  // can't fade in hosted mode

        if (enabled)
            SafeEffect.fade(this, new EffectOption[]{
                    new EffectOption("to", "1.0")
                    , new EffectOption("duration", "0.01")
            });
        else
            SafeEffect.fade(this, new EffectOption[]{
                    new EffectOption("to", "0.15")
                    , new EffectOption("duration", "0.01")
            });
    }
}
