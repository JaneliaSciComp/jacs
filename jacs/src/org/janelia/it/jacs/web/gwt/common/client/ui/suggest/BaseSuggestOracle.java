
package org.janelia.it.jacs.web.gwt.common.client.ui.suggest;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.SuggestOracle;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base implementation of GWT's SuggestOracle. Iterates through the list of items added to the associated
 * SuggestBox, and returns a list (up to the first MAX_ITEMS) of matching items. Subclasses must implement
 * <code>matches(query, item)</code> to determine if the query matches the provided item (both query and
 * item have been toLower()ed).
 *
 * @author Michael Press
 */
public abstract class BaseSuggestOracle extends SuggestOracle {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.suggest.BaseSuggestOracle");

    private ArrayList items = new ArrayList<SuggestionImpl>();

    public BaseSuggestOracle() {
    }

    /**
     * Wrapper class that's used for each matching Suggestion returned.
     */
    public class SuggestionImpl implements Suggestion, Serializable, IsSerializable {
        private String _value;
        private String _displayString;

        /**
         * Constructor used by RPC.
         */
        public SuggestionImpl() {
        }

        public SuggestionImpl(String value, String displayString) {
            _value = value;
            _displayString = displayString;
        }

        /**
         * Value to put in pulldown for this item
         */
        public String getDisplayString() {
            return _displayString;
        }

        /**
         * Value to put in the text box after an item is selected
         */
        public String getReplacementString() {
            return _value;
        }

        public Object getValue() {
            return _value;
        }
    }

    /**
     * Implementation of the sole abstract method in parent SuggestOracle.  Determines which strings match the
     * query string, populates a Response (which contains a List<SuggestionImpl>) and passes it to the supplied
     * Callback
     */
    public void requestSuggestions(Request request, Callback callback) {
        List<SuggestionImpl> suggestions = computeItemsFor(request.getQuery(), request.getLimit());
        Response response = new Response(suggestions);
        callback.onSuggestionsReady(request, response);
    }

    /**
     * Returns ArrayList<SuggestionImpl> of items that match the query;  relies on subclass implementation of
     * matches(query, item).
     *
     * @param query
     * @param limit
     * @return ArrayList<SuggestionImpl>
     */
    protected List<SuggestionImpl> computeItemsFor(String query, int limit) {
        int index;
        ArrayList<SuggestionImpl> suggestions = new ArrayList<SuggestionImpl>();
        for (int i = 0; i < items.size() && suggestions.size() < limit; i++) {
            String item = ((String) items.get(i));
            if ((index = matches(query.toLowerCase(), item.toLowerCase())) >= 0)
                suggestions.add(getFormattedSuggestion(query, item, index));
        }
        _logger.debug("found " + suggestions.size() + " suggestions for query " + query);
        return suggestions;
    }

    /**
     * Subclasses implement this to determine if the query matches the item.  Both are provided as fully lower case.
     *
     * @param query
     * @param item
     * @return
     */
    abstract protected int matches(String query, String item);

    /**
     * Formats the display string that will be shown in the SuggestBox popup by highlighting the matching letters
     *
     * @param query      the search query
     * @param suggestion the line shown in the popup
     * @param index      the query's location in the suggestion
     * @return
     */
    private SuggestionImpl getFormattedSuggestion(String query, String suggestion, int index) {
        StringBuffer formattedSuggestion = new StringBuffer();

        // part before match
        if (index > 0)
            formattedSuggestion.append(suggestion.substring(0, index));

        // matching part
        formattedSuggestion
                .append("<span class='suggestTextMatch'>")
                .append(suggestion.substring(index, index + query.length()))
                .append("</span>");

        // part after match
        if (suggestion.length() >= index + query.length())
            formattedSuggestion.append(suggestion.substring(index + query.length()));

        return new SuggestionImpl(suggestion, formattedSuggestion.toString());
    }

    public void add(String suggestion) {
        items.add(suggestion);
    }

    public void addAll(Collection collection) {
//        _logger.debug("added " + collection.size() + " items to SuggestOracle");
        items.addAll(collection);
    }

    public void removeAll() {
//        _logger.debug("emptying the SuggestOracle");
        items.clear();
    }

    /**
     * Specifies that the display string we're returning in the Suggestion is HTML
     */
    public boolean isDisplayStringHTML() {
        return true;
    }


    // Method to ensure the string passed in is actually a suggestion
    public boolean hasItem(String targetItem) {
        for (Object item1 : items) {
            String item = ((String) item1);
            if (item.equals(targetItem)) {
                return true;
            }
        }
        return false;
    }
}
