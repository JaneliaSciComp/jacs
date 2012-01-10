
package org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables;

/**
 * An extension of the PopperUpperHTML that displays an abbreviated version of some text, and on mouse hover pops
 * up a panel with the complete text. For performance reasons, doesn't create the popup until the
 * first mouse hover, or if the full text is not longer than the short text.  Caller can either 1) provide the
 * full text and let this class abbreviate it (adding "..." to the end), or 2) explicitly provide both the short and full text.
 *
 * @author Michael Press
 */
public class FulltextPopperUpperHTML extends PopperUpperHTML {
    private String _fullText;
    private String _shortText;

    public static final String ELLIPSIS = "...";
    public static final String HTML_BREAK = "<br>";
    private boolean _needPopup = false;

    /**
     * @param fullText
     */
    public FulltextPopperUpperHTML(String fullText, int shortTextLength) {
        super(null, null, false); // will update abbreviated text in init()
        _shortText = abbreviateText(fullText, shortTextLength);
        _fullText = fullText;
        init();
    }

    /**
     * Supports the client showing different text (not just abbreviated) for short and fulltext.
     */
    public FulltextPopperUpperHTML(String shortText, String fullText) {
        super(null, null, false); // will update abbreviated text in init()
        _fullText = fullText;
        _shortText = shortText;
        init();
    }

    protected void init() {
        if (_fullText == null) _fullText = "";
        if (_shortText == null) _shortText = "";

        setVisibleText(_shortText);
        setPopupContents(_fullText);

        determinePopupNeed();
        super.init();
    }

    /**
     * Do this once so needPopup() is fast during hovers
     */
    private void determinePopupNeed() {
        // Determine length of short text (without trailing elipses)
        String shortTextOnly = _shortText;
        if (shortTextOnly.endsWith(ELLIPSIS))
            shortTextOnly = shortTextOnly.substring(0, shortTextOnly.lastIndexOf(ELLIPSIS) - 1);

        _needPopup = _fullText.length() > shortTextOnly.length();
    }

    protected boolean needClickPopup() {
        return _needPopup;
    }

    protected boolean needHoverPopup() {
        return _needPopup;
    }

    /**
     * Determine if we need to show a popup - if the fulltext is longer than the short text (not including the "...").
     */
    protected boolean needPopup() {
        return _needPopup;
    }

    public static String abbreviateText(String str, int visibleSize) {
        // If nothing or blank, return empty string
        if (null == str || "".equals(str)) {
            return "";
        }
        //TODO: can remove the HTML break stuff?
        if (0 <= str.indexOf(HTML_BREAK) && visibleSize > str.indexOf(HTML_BREAK)) {
            return str.substring(0, str.indexOf(HTML_BREAK)) + ELLIPSIS + HTML_BREAK;
        }
        if (str.length() > visibleSize) {
            return str.substring(0, visibleSize - ELLIPSIS.length()) + ELLIPSIS;
        }
        else return str;
    }
}
