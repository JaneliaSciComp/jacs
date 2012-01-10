
package org.janelia.it.jacs.web.gwt.common.client.ui.suggest;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxFactory;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SmallRoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.Collection;

/**
 * A TitledBox containing a SuggestOracle (text box with dropdown that shows items matching the typed sequence).
 * When the user selects an item from the dropdown, or hits the Search button, the listener is notified.
 *
 * @author Michael Press
 */
public class SearchOraclePanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.suggest.SearchOraclePanel");
    private String _promptString;
    private SuggestBox _suggestBox;
    private BaseSuggestOracle _suggestOracle;
    private SearchOracleListener _oracleListener;
    private String _title;
    private TitledBox _titledBox;

    protected static final int ASCII_A = 65;
    protected static final int ASCII_Z = 91;
    private static final String DEFAULT_TITLE = "Search";

    public SearchOraclePanel(String promptString, SearchOracleListener listener) {
        this(DEFAULT_TITLE, promptString, TitledBoxFactory.BoxType.TERTIARY_BOX, /*show content*/ true, listener);
    }

    public SearchOraclePanel(String title, String promptString, TitledBoxFactory.BoxType boxType,
                             boolean showContent, SearchOracleListener listener) {
        _promptString = promptString;
        _oracleListener = listener;
        _title = title;
        init(boxType, showContent);
    }


    private void init(TitledBoxFactory.BoxType boxType, boolean showContent) {
        // Create the TitledBox labeled "Search"
        _titledBox = TitledBoxFactory.createTitledBox(_title, boxType);
        _titledBox.setWidth("350px"); // min width when content hidden
        if (!showContent)
            _titledBox.hideContent();

        Grid grid = new Grid(2, 2);
        grid.setCellPadding(0);
        grid.setCellSpacing(0);

        // Create the prompt/SuggestBox area
        HTML prompt = HtmlUtils.getHtml(_promptString + ":", "prompt");
        prompt.addStyleName("suggestBoxPrompt");
        grid.setWidget(0, 0, prompt);
        grid.setWidget(0, 1, createSearchArea());

        // Create Index links for each letter
        HTML index = HtmlUtils.getHtml("Index:", "prompt");
        index.addStyleName("suggestBoxPrompt");
        index.addStyleName("suggestBoxRowSeparator");
        grid.setWidget(1, 0, index);
        grid.setWidget(1, 1, createIndexLinks());

        _titledBox.add(grid);

        initWidget(_titledBox);
    }

    /**
     * Hook to set styles on the titled box
     */
    public TitledBox getTitledBox() {
        return _titledBox;
    }

    private HorizontalPanel createSearchArea() {
        // Create the suggest box (text box)
        _suggestBox = getSuggestBox();
        _suggestBox.setStyleName("suggestBox");
        //_suggestBox.setFocus(true); // throws JavaScriptException

        HorizontalPanel panel = new HorizontalPanel();
        panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        panel.add(_suggestBox);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));

        // Search button - notifies listener to run a "%<term>%" search
        RoundedButton searchButton = new SmallRoundedButton("Search", new ClickListener() {
            public void onClick(Widget sender) {
                notifyRunSearch("%" + _suggestBox.getText() + "%");
            }
        });
        panel.add(searchButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        // Clear button - notifies listener to clear results
        RoundedButton clearButton = new SmallRoundedButton("Clear", new ClickListener() {
            public void onClick(Widget sender) {
                clear();
            }
        });
        panel.add(clearButton);

        return panel;
    }

    public void clear() {
        _suggestBox.setText("");
        notifyShowAll();
    }

    private HorizontalPanel createIndexLinks() {
        HorizontalPanel indexPanel = new HorizontalPanel();
        indexPanel.addStyleName("suggestBoxRowSeparator");
        indexPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

        for (int i = ASCII_A; i < ASCII_Z; i++) {
            final String letter = String.valueOf((char) i);
            Link letterLink = new Link(letter, new ClickListener() {
                public void onClick(Widget sender) {
                    notifyRunSearch(letter + "%");
                }
            });
            indexPanel.add(letterLink);
            indexPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;", "letter"));
        }

        // "Show All" link
        indexPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;&nbsp;&nbsp;", "text"));
        indexPanel.add(new Link("Show All", new ClickListener() {
            public void onClick(Widget sender) {
                notifyShowAll(); // search for all
            }
        }));
        return indexPanel;
    }

    protected SuggestBox getSuggestBox() {
        // Create the oracle that will search the list to find matches
        _suggestOracle = new MatchesAnywhereSuggestOracle();

        // Create a text box that uses the oracle to display items that match the typed text.  Upon selection, run a
        // search for the selection so the table's updated
        final SuggestBox suggestBox = new SuggestBox(_suggestOracle);
        suggestBox.setStyleName("textBox");
        suggestBox.addEventHandler(new SuggestionHandler() {
            public void onSuggestionSelected(SuggestionEvent event) {
                _logger.debug("got text from oracle: " + suggestBox.getText());
                notifyRunSearch("%" + suggestBox.getText() + "%");
            }
        });

        return suggestBox;
    }

    private void notifyShowAll() {
        if (_oracleListener != null)
            _oracleListener.onShowAll();
    }

    private void notifyRunSearch(String searchString) {
        if (_oracleListener != null)
            _oracleListener.onRunSearch(searchString);
    }

    public void addOracleSuggestion(String suggestion) {
        _suggestOracle.add(suggestion);
    }

    public void addAllOracleSuggestions(Collection<String> values) {
        _suggestOracle.addAll(values);
    }

    public void removeOracleSuggestions() {
        _suggestOracle.removeAll();
    }

    public void addSuggestBoxStyleName(String styleName) {
        _suggestBox.addStyleName(styleName);
    }
}
