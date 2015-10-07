
package org.janelia.it.jacs.web.gwt.search.server.formatter;

import org.janelia.it.jacs.web.gwt.search.client.model.CategoryResult;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 7, 2007
 * Time: 5:04:54 PM
 */
abstract public class SearchResultCSVFormatter<E extends CategoryResult> {

    protected SearchResultCSVFormatter() {
        super();
    }

    public abstract String[] getResultHeadings();

    public abstract List<String> getResultFields(E cr);

}
