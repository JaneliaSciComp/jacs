
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.server.access.SearchDAO;
import org.janelia.it.jacs.server.api.ChartTool;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartData;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: cgoina
 * Implementation of search access object.
 */
abstract public class SearchDAOImpl extends DaoBaseImpl implements SearchDAO {
    private static Logger _logger = Logger.getLogger(SearchDAOImpl.class);
    protected static final String ROW_SEP = "ROWSEPARATOR";
    protected static final String LIST_SEP = "LISTSEPARATOR";

    protected ChartTool chartTool;
    //TODO: move these to properties or get from front end
    public static final int PIE_CHART_WIDTH = 320;
    public static final int PIE_CHART_HEIGHT = 220;
    public static final int BAR_CHART_WIDTH = 240;
    public static final int BAR_CHART_HEIGHT = 220;

    protected interface ChartDataCallback {
        public ChartData getChartDataMethod() throws DaoException;
    }

    protected SearchDAOImpl() {
    }

    public void setChartTool(ChartTool chartTool) {
        this.chartTool = chartTool;
    }

    abstract public int executeSearchTask(SearchTask searchTask)
            throws DaoException;

    abstract public int getNumSearchHits(String searchString, int matchFlags)
            throws DaoException;

    abstract public List<SearchHit> search(String searchString,
                                           int matchFlags,
                                           int startIndex,
                                           int numRows,
                                           SortArgument[] sortArgs)
            throws DaoException;

//    abstract public List<? extends CategoryResult> getPagedCategoryResultsByNodeId(Long nodeId,
//                                                                                   int startIndex,
//                                                                                   int numRows,
//                                                                                   SortArgument[] sortArgs)
//            throws DaoException;

    abstract public int getNumCategoryResultsByNodeId(Long nodeId) throws DaoException;

    abstract public List<ImageModel> getSearchResultCharts(Long searchId, String resultBaseDirectory)
            throws DaoException;

    public Set<Site> getMapInfoForSearchResultsBySearchId(Long searchId, String category)
            throws DaoException {
        throw new UnsupportedOperationException("Map Info is not supported for " + category);
    }

    protected ChartData getChartData(String filename, String basedir, ChartDataCallback callback)
            throws IOException, ClassNotFoundException, DaoException {
        File chartDataFile = new File(basedir, filename);
        ChartData dataset = null;
        if (chartDataFile.exists()) {
            logger.debug("Creating object input stream for file=" + chartDataFile);
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(chartDataFile));
            dataset = (ChartData) ois.readObject();
            ois.close();
        }
        else {
            dataset = callback.getChartDataMethod();
            logger.debug("Creating object output stream for file=" + chartDataFile);
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(chartDataFile));
            oos.writeObject(dataset);
            oos.close();
            chartDataFile.deleteOnExit();
        }
        return dataset;
    }

    protected int countSearchHits(String searchString, String searchCategory, int matchFlags)
            throws DaoException {
        try {
            _logger.debug("Count search hits for " + searchCategory);
            String sql = "select count(*) as nhits " +
                    "from " + getSearchQuery();
            _logger.debug("Count search hits sql: " + sql);
            SQLQuery sqlQuery = getSession().createSQLQuery(sql);
            sqlQuery.addScalar("nhits", Hibernate.LONG);
            setSearchQueryParameters(sqlQuery,
                    searchString,
                    searchCategory,
                    SearchTask.matchFlagsToString(matchFlags)
            );
            Long countSearchHits = (Long) sqlQuery.uniqueResult();
            _logger.debug("Number of search hits for " + searchCategory + ": " + countSearchHits.toString());
            return countSearchHits.intValue();
        }
        catch (Exception e) {
            _logger.error(e.getMessage(), e);
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

//    protected int populateSearchResult(SearchTask searchTask,String searchCategory)
//            throws DaoException {
//        try {
//            SearchQuerySessionContainer searchSessionContainer=new SearchQuerySessionContainer(getSession(),
//                    SearchQuerySessionContainer.SEARCH_ENGINE_LUCENE);
//            return searchSessionContainer.populateSearchResult(searchTask, searchCategory);
//        } catch (Exception e) {
//            _logger.error(e.getMessage(), e);
//            throw new DaoException(e.getMessage());
//        }
//    }

    //
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

    protected String adaptSortFieldName(String fieldName) {
        return fieldName;
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

    protected String getResultChartDirectory(Long searchId) {
        return "tmp" + "/" + "charts" + "/" + searchId;
    }

/*
    protected void addDocumentsToCategoryResultsByNodeId(Long nodeId, List<? extends CategoryResult> categoryList) {
        if (categoryList==null || categoryList.size()==0)
            return;
        String categoryType=categoryList.get(0).getResultType();
        StringBuffer accSB=new StringBuffer("");
        for (CategoryResult cr : categoryList) {
            String accession=cr.getAccession();
            if (accSB.toString().equals("")) {
                accSB.append("\'"+accession+"\'");
            } else {
                accSB.append(", \'"+accession+"\'");
            }
        }
        // Note in query below importance of order by rank, which ensures the
        // corresponding DocumentResult lists are populated by rank desc
        String docSql = "select distinct "+
                        //"se.accession, "+
                        "nt.accession "+
                        //"from sequence_entity se, "+
                        "from node_ts_result nt, "+
                        "camera_document doc, "+
                        "(select parameter_value from task_parameter tp"+
                        " where tp.task_id=(select task_id from node where node_id="+nodeId+")"+
                        " and tp.parameter_name='searchString') tp "+
                        //"where se.accession in ("+accSB.toString()+") "+
                        "where nt.accession in ("+accSB.toString()+") "+
                        //"and se.accession=nt.accession "+
                        "and nt.category=\'"+categoryType+"\' " +
                        "and nt.node_id="+nodeId+" "+
                        "and doc.docid=nt.docid "+
                        "order by nt.rank desc";
        _logger.info("Executing docSql="+docSql);
        SQLQuery docSqlQuery = getSession().createSQLQuery(docSql);
        List<Object[]> docResults = docSqlQuery.list();
        Map<String, List<DocumentResult>> docMap=new HashMap<String, List<DocumentResult>>();
        for(Object[] res : docResults) {
            DocumentResult documentResult=new DocumentResult();
            String accession=(String)res[0];
            List<DocumentResult> docList=docMap.get(accession);
            if (docList==null) {
                docList=new ArrayList<DocumentResult>();
                docMap.put(accession, docList);
            }
            documentResult.setCategory((String)res[1]);
            documentResult.setDocid(((BigInteger)res[2]).toString());
            documentResult.setDocname((String)res[3]);
            documentResult.setDoctype((String)res[4]);
            documentResult.setHeadline((String)res[5]);
            documentResult.setRank(((Float)res[6]).toString());
            documentResult.setMethod((String)res[7]);
            docList.add(documentResult);
        }
        for (CategoryResult cr : categoryList) {
            String accession=cr.getAccession();
            List<DocumentResult> docList=docMap.get(accession);
            if (docList==null) {
                _logger.error("docList is unexpectedly null for search nodeId="+nodeId+" accession="+accession);
            } else {
                _logger.info("Adding doclist size="+docList.size()+" for acc="+accession);
            }
            cr.setDocumentResult(docList);
        }
    }
*/

}
