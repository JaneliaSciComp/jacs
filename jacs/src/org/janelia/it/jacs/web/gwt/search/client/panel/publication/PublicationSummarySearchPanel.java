
package org.janelia.it.jacs.web.gwt.search.client.panel.publication;

import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySummarySearchPanel;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class PublicationSummarySearchPanel extends CategorySummarySearchPanel {

    public PublicationSummarySearchPanel(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected CategorySearchDataBuilder createDataBuilder(String searchId, String searchQuery) {
        return new PublicationSearchDataBuilder(searchId, searchQuery);
    }

}
