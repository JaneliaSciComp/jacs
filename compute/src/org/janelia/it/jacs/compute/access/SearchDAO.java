
package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.compute.access.search.CategoryResult;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 14, 2008
 * Time: 10:54:04 AM
 */
public abstract class SearchDAO extends ComputeBaseDAO {

    public SearchDAO(Logger logger) {
        super(logger);
    }

    abstract public List<? extends CategoryResult> getPagedCategoryResultsByNodeId(Long nodeId,
                                                                                   int startIndex,
                                                                                   int numRows,
                                                                                   SortArgument[] sortArgs)
            throws DaoException;

    abstract public int getNumCategoryResultsByNodeId(Long nodeId) throws DaoException;

    protected int countSearchHits(String searchString, String searchCategory, int matchFlags)
            throws DaoException {
        try {
            log.debug("Count search hits for " + searchCategory);
            String sql = "select count(*) as nhits " +
                    "from " + getSearchQuery();
            log.debug("Count search hits sql: " + sql);
            SQLQuery sqlQuery = getSession().createSQLQuery(sql);
            sqlQuery.addScalar("nhits", Hibernate.LONG);
            setSearchQueryParameters(sqlQuery,
                    searchString,
                    searchCategory,
                    SearchTask.matchFlagsToString(matchFlags)
            );
            Long countSearchHits = (Long) sqlQuery.uniqueResult();
            log.debug("Number of search hits for " + searchCategory + ": " + countSearchHits.toString());
            return countSearchHits.intValue();
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DaoException(e.getMessage());
        }
    }

    private static String getSearchQuery() {
        return "fullTextQuery(:searchString,:searchCategory,:matchFlags)";
    }

    private static void setSearchQueryParameters(SQLQuery sqlQuery,
                                                 String searchString,
                                                 String searchCategory,
                                                 String matchFlags) {
        sqlQuery.setString("searchString", searchString);
        sqlQuery.setString("searchCategory", searchCategory);
        sqlQuery.setString("matchFlags", matchFlags);
    }

    protected List<SearchHit> performGenericSearch(String searchString,
                                                   String searchCategory,
                                                   int matchFlags,
                                                   int startIndex,
                                                   int numRows,
                                                   SortArgument[] sortArgs)
            throws DaoException {
        String sql = "select " + getSearchQueryFields() +
                "from " + getSearchQuery();
        sql = addOrderClauseToSql(sql, sortArgs);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        setSearchQueryParameters(sqlQuery,
                searchString,
                searchCategory,
                SearchTask.matchFlagsToString(matchFlags)
        );
        if (startIndex >= 0) {
            sqlQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            sqlQuery.setMaxResults(numRows);
        }
        List<Object[]> results = sqlQuery.list();
        List<SearchHit> searchHits = new ArrayList<SearchHit>();
        for (Object[] res : results) {
            SearchHit searchHit = new SearchHit();
            searchHit.setDocumentId((Long) res[0]);
            searchHit.setAccession((String) res[1]);
            searchHit.setDocumentName((String) res[2]);
//            searchHit.setCategory((String)res[3]);
//            searchHit.setDocumentType((String)res[4]);
            searchHit.setDocumentType((String) res[3]);
/*
            searchHit.setMethod((String)res[5]);
            searchHit.setHeadline((String)res[6]);
            searchHit.setRank(((Float)res[7]).doubleValue());
*/
            searchHits.add(searchHit);
        }
        return searchHits;
    }

    private static String getSearchQueryFields() {
        return "docid," +
                "accession," +
                "docname," +
                "doctype," +
                "headline";
    }

    protected String addOrderClauseToSql(String sql, SortArgument[] sortArgs) {
        StringBuffer orderByFieldsBuffer = new StringBuffer();
        if (sortArgs != null) {
            for (SortArgument sortArg : sortArgs) {
                String dataSortField = sortArg.getSortArgumentName();
                if (dataSortField != null && dataSortField.length() != 0) {
                    dataSortField = adaptSortFieldName(dataSortField);
                    if (sortArg.isAsc()) {
                        if (orderByFieldsBuffer.length() > 0) {
                            orderByFieldsBuffer.append(',');
                        }
                        orderByFieldsBuffer.append(dataSortField).append(" asc");
                    }
                    else if (sortArg.isDesc()) {
                        if (orderByFieldsBuffer.length() > 0) {
                            orderByFieldsBuffer.append(',');
                        }
                        orderByFieldsBuffer.append(dataSortField).append(" desc");
                    }
                }
            } // end for all sortArgs
        }
/*
        if (orderByFieldsBuffer.length() > 0) orderByFieldsBuffer.append(',');
        orderByFieldsBuffer.append("rank desc");
*/

        String orderByClause = "";
        if (orderByFieldsBuffer.length() > 0) {
            orderByClause = " order by " + orderByFieldsBuffer.toString();
        }
        sql += orderByClause;
        return sql;
    }

    protected String adaptSortFieldName(String fieldName) {
        return fieldName;
    }

    public SearchResultNode retrieveSearchNodeByTask(Long searchId)
            throws DaoException {
        try {
            SearchTask st = (SearchTask) getTaskById(searchId);
            return st.getSearchResultNode();
        }
        catch (Throwable e) {
            // log the error
            log.error(e);
            // rethrow the error
            throw new DaoException(e);
        }
    }

}
