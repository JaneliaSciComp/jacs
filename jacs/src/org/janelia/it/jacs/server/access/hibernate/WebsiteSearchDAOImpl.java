
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;
//import org.janelia.it.jacs.web.gwt.search.client.model.WebsiteResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: cgoina
 * Implementation of JaCS website search access object. This class is in a different
 * package than the other searchDAOs because this is not a hibernate based implementation.
 */
public class WebsiteSearchDAOImpl extends SearchDAOImpl {
    private static Logger _logger = Logger.getLogger(WebsiteSearchDAOImpl.class);

    protected WebsiteSearchDAOImpl() {
    }

    public int executeSearchTask(SearchTask searchTask)
            throws DaoException {
        return -1;
    }

    public int getNumSearchHits(String searchString, int matchFlags)
            throws DaoException {
        if (Constants.USE_GOOGLE_API_FOR_WEBSITE_SEARCH) {
            return -3;
        }
        return countSearchHits(searchString, SearchTask.TOPIC_WEBSITE, matchFlags);
    }

    public List<SearchHit> search(String searchString,
                                  int matchFlags,
                                  int startIndex,
                                  int numRows,
                                  SortArgument[] sortArgs)
            throws DaoException {
        return new ArrayList<SearchHit>();
    }

//    public List<WebsiteResult> getPagedCategoryResultsByNodeId(Long nodeId,
//                                                               int startIndex,
//                                                               int numRows,
//                                                               SortArgument[] sortArgs) throws DaoException {
//        if (Constants.USE_GOOGLE_API_FOR_WEBSITE_SEARCH) {
//            return null;
//        }
//        String sql = "select " +
////                "docid," +
//                "accession," +
//                "docname," +
//                "headline " +
//                "from website_ts_result nt " +
//                "where nt.node_id = :nodeId";
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        sqlQuery.setLong("nodeId", nodeId);
//        if (startIndex >= 0) {
//            sqlQuery.setFirstResult(startIndex);
//        }
//        if (numRows > 0) {
//            sqlQuery.setMaxResults(numRows);
//        }
//        List<Object[]> results = sqlQuery.list();
//        List<WebsiteResult> webSearchResults = new ArrayList<WebsiteResult>();
//        for (Object[] res : results) {
//            WebsiteResult webSearchRes = new WebsiteResult();
//            String title = (String) res[1];
//            webSearchRes.setTitle(title);
//            webSearchRes.setTitleURL(extractHREF(title));
//            webSearchRes.setBlurb((String) res[2]);
//            webSearchResults.add(webSearchRes);
//        }
//        return webSearchResults;
//    }
//
    public int getNumCategoryResultsByNodeId(Long nodeId) throws DaoException {
        if (Constants.USE_GOOGLE_API_FOR_WEBSITE_SEARCH) {
            return -3;
        }
        String sql = "select cast(count(1) as Integer) " +
                "from website_ts_result nt " +
                "where nt.node_id = :nodeId";
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setLong("nodeId", nodeId);
        int count = ((Integer) sqlQuery.uniqueResult()).intValue();
        return count;
    }

    public List<ImageModel> getSearchResultCharts(Long searchId, String resultBaseDirectory)
            throws DaoException {
        return null;  // no charts for web results
    }

    public Set<Site> getMapInfoForSearchResultsBySearchId(Long searchId, String category)
            throws DaoException {
        throw new UnsupportedOperationException("Map Info is not supported for " + category);
    }

    /**
     * This is a really kludgy way to extract the HREF url from an anchor element
     *
     * @param anchor
     * @return
     */
    private String extractHREF(String anchor) {
        String url = null;
        if (anchor != null) {
            int currentpos = 0;
            for (; ;) {
                currentpos = anchor.indexOf('=', currentpos);
                if (currentpos == -1) {
                    // no attribute found
                    break;
                }
                else {
                    String leftsubstring = anchor.substring(0, currentpos);
                    String rightsubstring = anchor.substring(currentpos + 1);
                    if (leftsubstring.trim().toUpperCase().endsWith("HREF")) {
                        // the right hand side contains the actual URL
                        int startURL = rightsubstring.indexOf('"');
                        if (startURL >= 0) {
                            int endURL = rightsubstring.indexOf('"', startURL + 1);
                            if (endURL >= 0) {
                                url = rightsubstring.substring(startURL + 1, endURL);
                            }
                        }
                        break;
                    }
                    else {
                        currentpos++;
                        continue;
                    }
                }
            }
        }
        return url;
    }

}
