
package org.janelia.it.jacs.server.api;

import org.janelia.it.jacs.server.access.SearchDAO;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 */
public class SearchDAOFactory {

    private Map<String, SearchDAO> searchDAOs;

    public SearchDAOFactory() {
    }

    public SearchDAO getSearchDAO(String category) {
        return searchDAOs.get(category);
    }

    public void setSearchDAOs(Map<String, SearchDAO> searchDAOs) {
        this.searchDAOs = searchDAOs;
    }

}
