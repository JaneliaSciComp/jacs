
package org.janelia.it.jacs.web.gwt.common.client.ui.suggest;

/**
 * Implementation of BaseSuggestOracle that returns matching items if they <strong>start with</strong> the
 * specified query string (case-insensitive)
 *
 * @author Michael Press
 */
public class StartsWithSuggestOracle extends BaseSuggestOracle {
    protected int matches(String query, String item) {
        return (item.startsWith(query)) ? 0 : -1;
    }
}
