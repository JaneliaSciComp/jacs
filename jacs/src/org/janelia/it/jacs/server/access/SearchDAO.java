
package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;
//import org.janelia.it.jacs.web.gwt.search.client.model.CategoryResult;

import java.util.List;
import java.util.Set;

/**
 * SearchDAO typically performs category specific searches. Also notice that
 * a category search may use a list of topics but these topis are specific
 * for each category and for now are not exposed in the API
 * <p/>
 * Created by IntelliJ IDEA.
 * User: cgoina
 */
public interface SearchDAO extends DAO {

    int executeSearchTask(SearchTask searchTask)
            throws DaoException;

    int getNumSearchHits(String searchString, int matchFlags)
            throws DaoException;

    List<SearchHit> search(String searchString,
                           int matchFlags,
                           int startIndex,
                           int numRows,
                           SortArgument[] sortArgs)
            throws DaoException;

//    List<? extends CategoryResult> getPagedCategoryResultsByNodeId(Long nodeId,
//                                                                   int startIndex,
//                                                                   int numRows,
//                                                                   SortArgument[] sortArgs)
//            throws DaoException;
//
    int getNumCategoryResultsByNodeId(Long nodeId) throws DaoException;

    List<ImageModel> getSearchResultCharts(Long searchId, String resultBaseDirectory)
            throws DaoException;

    Set<Site> getMapInfoForSearchResultsBySearchId(Long searchId, String category)
            throws DaoException;

}
