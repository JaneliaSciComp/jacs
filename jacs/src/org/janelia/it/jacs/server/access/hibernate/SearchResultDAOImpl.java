
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.server.access.SearchResultDAO;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: cgoina
 * Implementation of search result access object.
 */
public class SearchResultDAOImpl extends DaoBaseImpl implements SearchResultDAO {
    private static Logger _logger = Logger.getLogger(SearchDAOImpl.class);

    protected SearchResultDAOImpl() {
    }

    public int getNumSearchResultsByTaskId(Long searchTaskId, List<String> categories)
            throws DaoException {
        String hql = "select count(*) " +
                "from SearchHit hit " +
                "where hit.searchResultNode.task.objectId = :taskId ";
        if (categories != null && categories.size() > 0) {
            hql += "and hit.category in (:categories) ";
        }
        StringBuffer orderByFieldsBuffer = new StringBuffer();
        _logger.debug("Count hits hql: " + hql);
        Query countSearchResultsQuery = getSession().createQuery(hql);
        countSearchResultsQuery.setLong("taskId", searchTaskId);
        if (categories != null && categories.size() > 0) {
            countSearchResultsQuery.setParameterList("categories", categories);
        }
        Long searchResultsCount = (Long) countSearchResultsQuery.uniqueResult();
        return searchResultsCount.intValue();
    }

    public List<SearchHit> getPagedSearchResultsByTaskId(Long searchTaskId,
                                                         List<String> categories,
                                                         int startIndex,
                                                         int numRows,
                                                         SortArgument[] sortArgs)
            throws DaoException {
        String hql = "select hit " +
                "from SearchHit hit " +
                "where hit.searchResultNode.task.objectId = :taskId ";
        if (categories != null && categories.size() > 0) {
            hql += "and hit.category in (:categories) ";
        }
        StringBuffer orderByFieldsBuffer = new StringBuffer();
        if (sortArgs != null) {
            for (SortArgument sortArg : sortArgs) {
                String dataSortField = sortArg.getSortArgumentName();
                if (dataSortField == null || dataSortField.length() == 0) {
                    continue;
                }
                if (dataSortField.equals("category")) {
                    dataSortField = "hit.category";
                }
                else if (dataSortField.equals("rank")) {
                    dataSortField = "hit.rank";
                }
                else if (dataSortField.equals("headline")) {
                    dataSortField = "hit.headline";
                }
                if (dataSortField != null && dataSortField.length() != 0) {
                    if (sortArg.isAsc()) {
                        if (orderByFieldsBuffer.length() > 0) {
                            orderByFieldsBuffer.append(',');
                        }
                        orderByFieldsBuffer.append(dataSortField + " asc");
                    }
                    else if (sortArg.isDesc()) {
                        if (orderByFieldsBuffer.length() > 0) {
                            orderByFieldsBuffer.append(',');
                        }
                        orderByFieldsBuffer.append(dataSortField + " desc");
                    }
                }
            } // end for all sortArgs
        }
        String orderByClause = "";
        if (orderByFieldsBuffer.length() > 0) {
            orderByClause = "order by " + orderByFieldsBuffer.toString();
        }
        hql += orderByClause;
        _logger.debug("Search hits hql: " + hql);
        Query searchResultsQuery = getSession().createQuery(hql);
        searchResultsQuery.setLong("taskId", searchTaskId);
        if (categories != null && categories.size() > 0) {
            searchResultsQuery.setParameterList("categories", categories);
        }
        if (startIndex > 0) {
            searchResultsQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            searchResultsQuery.setMaxResults(numRows);
        }
        return searchResultsQuery.list();
    }

    public List<String> getSearchResultCategoriesByTaskId(Long searchTaskId) throws DaoException {
        try {
            SearchTask searchTask = (SearchTask) getHibernateTemplate().get(SearchTask.class, searchTaskId);
            return searchTask.getSearchTopics();
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "TaskDAOImpl - getTaskById");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "TaskDAOImpl - getTaskById");
        }
    }

    /**
     * Return number of hits for every category searched within a task.
     */
    public Map<String, Integer> getNumCategoryResults(Long taskId, Collection<String> categories)
            throws DaoException {
        String sql = "select * from task_category_count(:taskId, :categoryList);";
        //"1215829329003417891,'project,sample,final_cluster')";
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setLong("taskId", taskId);
        sqlQuery.setString("categoryList", getCategoryList(categories));
        List<String> results = sqlQuery.list();

        Map<String, Integer> numhits = new HashMap();
        for (String result : results) {
            String[] vals = result.split(",");
            numhits.put(/*category*/ vals[0], /*num hits*/ new Integer(vals[1]));
        }

        return numhits;
    }

    /**
     * Convert Set<String> to String of comma-separated category names
     */
    private String getCategoryList(Collection<String> categoryList) {
        StringBuffer buf = new StringBuffer();
        for (String category : categoryList) {
            if (buf.length() > 0)
                buf.append(",");
            buf.append(category);
        }
        return buf.toString();
    }
}
