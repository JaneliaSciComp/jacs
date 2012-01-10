
package org.janelia.it.jacs.web.gwt.common.client.ui.suggest;

/**
 * Listener for Search Oracle events.
 *
 * @author Michael Press
 */
public interface SearchOracleListener {
    /**
     * When user types something and clicks the Search button, or hits a letter index link for a "starts with" search
     */
    public void onRunSearch(String searchString);

    /**
     * When user clicks "Show All" or "Clear", indicating no filtering of the data
     */
    public void onShowAll();
}
