
package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.server.access.hibernate.DaoException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 */
public interface SearchResultDAO extends DAO {

    int getNumSearchResultsByTaskId(Long searchTaskId, List<String> categories)
            throws DaoException;

    List<SearchHit> getPagedSearchResultsByTaskId(Long searchTaskId,
                                                  List<String> categories,
                                                  int startIndex,
                                                  int numRows,
                                                  SortArgument[] sortArgs)
            throws DaoException;

    List<String> getSearchResultCategoriesByTaskId(Long searchTaskId)
            throws DaoException;

    Map<String, Integer> getNumCategoryResults(Long taskId, Collection<String> categoryList)
            throws DaoException;

}
