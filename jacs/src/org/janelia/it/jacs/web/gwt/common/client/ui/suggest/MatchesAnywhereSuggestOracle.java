
package org.janelia.it.jacs.web.gwt.common.client.ui.suggest;

/**
 * Implementation of BaseSuggestOracle that returns items that contain the query at any location.
 *
 * @author Michael Press
 */
public class MatchesAnywhereSuggestOracle extends BaseSuggestOracle {
    /**
     * Implementation of superclass matches() - returns the index of the query's location on the item, or -1
     * if there's no match.
     */
    protected int matches(String query, String item) {
        return item.indexOf(query);
    }
}
